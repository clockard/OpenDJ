/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.server.core;

import org.opends.server.api.DirectoryThread;
import org.opends.server.api.Backend;
import org.opends.server.api.BackendInitializationListener;
import org.opends.server.api.ServerShutdownListener;
import org.opends.server.api.ChangeNotificationListener;
import org.opends.server.loggers.debug.DebugTracer;
import static org.opends.server.loggers.debug.DebugLogger.getTracer;
import static org.opends.server.loggers.debug.DebugLogger.debugEnabled;
import org.opends.server.loggers.ErrorLogger;
import org.opends.server.types.*;
import org.opends.server.types.operation.PostResponseAddOperation;
import org.opends.server.types.operation.PostResponseDeleteOperation;
import org.opends.server.types.operation.PostResponseModifyOperation;
import org.opends.server.types.operation.PostResponseModifyDNOperation;
import static org.opends.server.util.StaticUtils.stackTraceToSingleLineString;
import static org.opends.server.util.ServerConstants.OC_TOP;
import static org.opends.server.util.ServerConstants.
     OID_ENTRY_CHANGE_NOTIFICATION;
import org.opends.server.config.ConfigConstants;
import static org.opends.server.config.ConfigConstants.OC_CRYPTO_INSTANCE_KEY;
import static org.opends.server.config.ConfigConstants.OC_CRYPTO_CIPHER_KEY;
import static org.opends.server.config.ConfigConstants.OC_CRYPTO_MAC_KEY;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.internal.InternalSearchListener;
import org.opends.server.controls.PersistentSearchChangeType;
import org.opends.server.controls.EntryChangeNotificationControl;
import static org.opends.messages.CoreMessages.*;
import org.opends.messages.Message;
import org.opends.admin.ads.ADSContext;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;

/**
 * This class defines a thread that synchronizes certificates from the admin
 * data branch into the trust store backend.
 */
public class TrustStoreSyncThread extends DirectoryThread
     implements ServerShutdownListener, BackendInitializationListener,
     InternalSearchListener, ChangeNotificationListener
{
  /**
   * The debug log tracer for this object.
   */
  private static final DebugTracer TRACER = getTracer();



  // A lock and condition for notifying this thread.
  private Lock lock;
  private Condition condition;

  // The DN of the administration suffix.
  private DN adminSuffixDN;

  // The DN of the instance keys container within the admin suffix.
  private DN instanceKeysDN;

  // The DN of the secret keys container within the admin suffix.
  private DN secretKeysDN;

  // The DN of the trust store root.
  private DN trustStoreRootDN;

  // The attribute type that is used to specify a server instance certificate.
  AttributeType attrCert;

  // The attribute type that holds a server certificate identifier.
  AttributeType attrAlias;

  // The attribute type that holds the time a key was compromised.
  AttributeType attrCompromisedTime;

  // A filter on object class to select key entries.
  private SearchFilter keySearchFilter;

  // Indicates whether the ADS suffix backend is initialized.
  private boolean adminBackendInitialized;

  // Indicates whether the trust store backend is initialized.
  private boolean trustStoreBackendInitialized;

  // Indicates whether a shutdown request has been received.
  private boolean shutdownRequested;

  // Indicates whether the initial search has been done.
  private boolean searchDone;

  // The instance key objectclass.
  private ObjectClass ocInstanceKey;

  // The cipher key objectclass.
  private ObjectClass ocCipherKey;

  // The mac key objectclass.
  private ObjectClass ocMacKey;

  /**
   * Creates a new instance of this trust store synchronization thread.
   */
  public TrustStoreSyncThread()
  {
    super("Trust Store Synchronization Thread");
    setDaemon(true);

    shutdownRequested = false;
    adminBackendInitialized = false;
    trustStoreBackendInitialized = false;
    searchDone = false;

    DirectoryServer.registerShutdownListener(this);
    DirectoryServer.registerBackendInitializationListener(this);

    lock = new ReentrantLock();
    condition = lock.newCondition();

    try
    {
      adminSuffixDN = DN.decode(ADSContext.getAdministrationSuffixDN());
      instanceKeysDN = adminSuffixDN.concat(DN.decode("cn=instance keys"));
      secretKeysDN = adminSuffixDN.concat(DN.decode("cn=secret keys"));
      trustStoreRootDN = DN.decode(ConfigConstants.DN_TRUST_STORE_ROOT);
      keySearchFilter =
           SearchFilter.createFilterFromString("(|" +
                "(objectclass=" + OC_CRYPTO_INSTANCE_KEY + ")" +
                "(objectclass=" + OC_CRYPTO_CIPHER_KEY + ")" +
                "(objectclass=" + OC_CRYPTO_MAC_KEY + ")" +
                ")");
    }
    catch (DirectoryException e)
    {
      //
    }

    ocInstanceKey = DirectoryServer.getObjectClass(
         OC_CRYPTO_INSTANCE_KEY, true);
    ocCipherKey = DirectoryServer.getObjectClass(
         OC_CRYPTO_CIPHER_KEY, true);
    ocMacKey = DirectoryServer.getObjectClass(
         OC_CRYPTO_MAC_KEY, true);

    attrCert = DirectoryServer.getAttributeType(
         ConfigConstants.ATTR_CRYPTO_PUBLIC_KEY_CERTIFICATE, true);
    attrAlias = DirectoryServer.getAttributeType(
         ConfigConstants.ATTR_CRYPTO_KEY_ID, true);
    attrCompromisedTime = DirectoryServer.getAttributeType(
         ConfigConstants.ATTR_CRYPTO_KEY_COMPROMISED_TIME, true);

    if (DirectoryServer.getBackendWithBaseDN(adminSuffixDN) != null)
    {
      adminBackendInitialized = true;
    }

    if (DirectoryServer.getBackendWithBaseDN(trustStoreRootDN) != null)
    {
      trustStoreBackendInitialized = true;
    }

  }


//  We would need this to detect changes if the admin branch was remote.
//  private SearchOperation runPersistentSearch()
//  {
//    InternalClientConnection conn =
//         InternalClientConnection.getRootConnection();
//    LinkedHashSet<String> attributes = new LinkedHashSet<String>(0);
//
//    // Specify the persistent search control.
//    Set<PersistentSearchChangeType> changeTypes =
//         new HashSet<PersistentSearchChangeType>();
//    changeTypes.add(PersistentSearchChangeType.ADD);
//    changeTypes.add(PersistentSearchChangeType.DELETE);
//    changeTypes.add(PersistentSearchChangeType.MODIFY);
//
//    boolean changesOnly = false;
//    boolean returnECs = true;
//
//    PersistentSearchControl psc =
//         new PersistentSearchControl(changeTypes, changesOnly, returnECs);
//    ArrayList<Control> controls = new ArrayList<Control>(1);
//    controls.add(psc);
//
//    InternalSearchOperation searchOperation =
//         new InternalSearchOperation(
//              conn,
//              InternalClientConnection.nextOperationID(),
//              InternalClientConnection.nextMessageID(),
//              controls,
//              adminSuffixDN, SearchScope.WHOLE_SUBTREE,
//              DereferencePolicy.NEVER_DEREF_ALIASES,
//              0, 0,
//              false, instanceKeyFilter, attributes,
//              this);
//
//    searchOperation.run();
//
//    return searchOperation;
//  }


  private SearchOperation runSearch()
  {
    InternalClientConnection conn =
         InternalClientConnection.getRootConnection();
    LinkedHashSet<String> attributes = new LinkedHashSet<String>(0);

    ArrayList<Control> controls = new ArrayList<Control>(0);

    InternalSearchOperation searchOperation =
         new InternalSearchOperation(conn,
                                     InternalClientConnection.nextOperationID(),
                                     InternalClientConnection.nextMessageID(),
                                     controls,
                                     adminSuffixDN, SearchScope.WHOLE_SUBTREE,
                                     DereferencePolicy.NEVER_DEREF_ALIASES,
                                     0, 0,
                                     false, keySearchFilter, attributes,
                                     this);

    searchOperation.run();

    return searchOperation;
  }


  /**
   * Performs an initial search on the ADS branch when enabled, then listens
   * for changes within the branch, writing certificates from instance key
   * entries to the trust store backend.
   */
  public void run()
  {
    while (!shutdownRequested)
    {
      try
      {
        if (!searchDone && adminBackendInitialized &&
             trustStoreBackendInitialized)
        {
          SearchOperation searchOperation = runSearch();

          ResultCode resultCode = searchOperation.getResultCode();
          if (resultCode != ResultCode.SUCCESS)
          {
            Message message =
                 INFO_TRUSTSTORESYNC_ADMIN_SUFFIX_SEARCH_FAILED.get(
                      String.valueOf(adminSuffixDN),
                      searchOperation.getErrorMessage().toString());
            ErrorLogger.logError(message);
          }
          searchDone = true;
          DirectoryServer.registerChangeNotificationListener(this);
        }

        // Wait until a backend changes state or a shutdown is requested.
        awaitCondition();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }

        Message message = ERR_TRUSTSTORESYNC_EXCEPTION.get(
             stackTraceToSingleLineString(e));
        ErrorLogger.logError(message);
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  public String getShutdownListenerName()
  {
    return "Trust Store Synchronization Thread";
  }



  /**
   * {@inheritDoc}
   */
  public void processServerShutdown(Message reason)
  {
    shutdownRequested = true;
    notifyCondition();
  }

  private void notifyCondition()
  {
    lock.lock();
    try
    {
      condition.signalAll();
    }
    finally
    {
      lock.unlock();
    }
  }


  private void awaitCondition()
  {
    lock.lock();
    try
    {
      condition.await();
    }
    catch (InterruptedException e)
    {
      // ignore
    }
    finally
    {
      lock.unlock();
    }
  }


  /**
   * {@inheritDoc}
   */
  public void performBackendInitializationProcessing(Backend backend)
  {
    boolean notify = false;

    DN[] baseDNs = backend.getBaseDNs();
    if (baseDNs != null)
    {
      for (DN baseDN : baseDNs)
      {
        if (baseDN.equals(adminSuffixDN))
        {
          adminBackendInitialized = true;
          notify = true;
        }
        else if (baseDN.equals(trustStoreRootDN))
        {
          trustStoreBackendInitialized = true;
          notify = true;
        }
      }
    }

    if (notify)
    {
      notifyCondition();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void performBackendFinalizationProcessing(Backend backend)
  {
    boolean notify = false;

    DN[] baseDNs = backend.getBaseDNs();
    if (baseDNs != null)
    {
      for (DN baseDN : baseDNs)
      {
        if (baseDN.equals(adminSuffixDN))
        {
          adminBackendInitialized = false;
          notify = true;
        }
        else if (baseDN.equals(trustStoreRootDN))
        {
          adminBackendInitialized = false;
          notify = true;
        }
      }
    }

    if (notify)
    {
      notifyCondition();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void handleInternalSearchEntry(InternalSearchOperation searchOperation,
                                        SearchResultEntry searchEntry)
       throws DirectoryException
  {
    if (searchEntry.hasObjectClass(ocInstanceKey))
    {
      handleInstanceKeySearchEntry(searchEntry);
    }
    else
    {
      try
      {
        if (searchEntry.hasObjectClass(ocCipherKey))
        {
          DirectoryServer.getCryptoManager().importCipherKeyEntry(searchEntry);
        }
        else if (searchEntry.hasObjectClass(ocMacKey))
        {
          DirectoryServer.getCryptoManager().importMacKeyEntry(searchEntry);
        }
      }
      catch (CryptoManager.CryptoManagerException e)
      {
        throw new DirectoryException(
             DirectoryServer.getServerErrorResultCode(), e);
      }
    }
  }


  private void handleInstanceKeySearchEntry(SearchResultEntry searchEntry)
       throws DirectoryException
  {
    RDN srcRDN = searchEntry.getDN().getRDN();

    // Only process the entry if it has the expected form of RDN.
    if (!srcRDN.isMultiValued() &&
         srcRDN.getAttributeType(0).equals(attrAlias))
    {
      DN dstDN = trustStoreRootDN.concat(srcRDN);

      // Extract any change notification control.
      EntryChangeNotificationControl ecn = null;
      List<Control> controls = searchEntry.getControls();
      try
      {
        for (Control c : controls)
        {
          if (c.getOID().equals(OID_ENTRY_CHANGE_NOTIFICATION))
          {
            ecn = EntryChangeNotificationControl.decodeControl(c);
          }
        }
      }
      catch (LDAPException e)
      {
        // ignore
      }

      // Get any existing local trust store entry.
      Entry dstEntry = DirectoryServer.getEntry(dstDN);

      if (ecn != null &&
           ecn.getChangeType() == PersistentSearchChangeType.DELETE)
      {
        // The entry was deleted so we should remove it from the local trust
        // store.
        if (dstEntry != null)
        {
          deleteEntry(dstDN);
        }
      }
      else if (searchEntry.hasAttribute(attrCompromisedTime))
      {
        // The key was compromised so we should remove it from the local
        // trust store.
        if (dstEntry != null)
        {
          deleteEntry(dstDN);
        }
      }
      else
      {
        // The entry was added or modified.
        if (dstEntry == null)
        {
          addEntry(searchEntry, dstDN);
        }
        else
        {
          modifyEntry(searchEntry, dstEntry);
        }
      }
    }
  }


  /**
   * Modify an entry in the local trust store if it differs from an entry in
   * the ADS branch.
   * @param srcEntry The instance key entry in the ADS branch.
   * @param dstEntry The local trust store entry.
   */
  private void modifyEntry(Entry srcEntry, Entry dstEntry)
  {
    List<Attribute> srcList;
    srcList = srcEntry.getAttribute(attrCert);

    List<Attribute> dstList;
    dstList = dstEntry.getAttribute(attrCert);

    // Check for changes to the certificate value.
    boolean differ = false;
    if (srcList == null)
    {
      if (dstList != null)
      {
        differ = true;
      }
    }
    else if (dstList == null)
    {
      differ = true;
    }
    else if (srcList.size() != dstList.size())
    {
      differ = true;
    }
    else
    {
      if (!srcList.equals(dstList))
      {
        differ = true;
      }
    }

    if (differ)
    {
      // The trust store backend does not implement modify so we need to
      // delete then add.
      DN dstDN = dstEntry.getDN();
      deleteEntry(dstDN);
      addEntry(srcEntry, dstDN);
    }
  }


  /**
   * Delete an entry from the local trust store.
   * @param dstDN The DN of the entry to be deleted in the local trust store.
   */
  private void deleteEntry(DN dstDN)
  {
    InternalClientConnection conn =
         InternalClientConnection.getRootConnection();

    DeleteOperation delOperation = conn.processDelete(dstDN);

    if (delOperation.getResultCode() != ResultCode.SUCCESS)
    {
      Message message = INFO_TRUSTSTORESYNC_DELETE_FAILED.get(
           String.valueOf(dstDN),
           String.valueOf(delOperation.getErrorMessage()));
      ErrorLogger.logError(message);
    }
  }


  /**
   * Add an entry to the local trust store.
   * @param srcEntry The instance key entry in the ADS branch.
   * @param dstDN The DN of the entry to be added in the local trust store.
   */
  private void addEntry(Entry srcEntry, DN dstDN)
  {
    LinkedHashMap<ObjectClass,String> ocMap =
         new LinkedHashMap<ObjectClass,String>(2);
    ocMap.put(DirectoryServer.getTopObjectClass(), OC_TOP);
    ocMap.put(ocInstanceKey, OC_CRYPTO_INSTANCE_KEY);

    HashMap<AttributeType, List<Attribute>> userAttrs =
         new HashMap<AttributeType, List<Attribute>>();

    List<Attribute> attrList;
    attrList = srcEntry.getAttribute(attrAlias);
    if (attrList != null)
    {
      userAttrs.put(attrAlias, attrList);
    }
    attrList = srcEntry.getAttribute(attrCert);
    if (attrList != null)
    {
      userAttrs.put(attrCert, attrList);
    }

    Entry addEntry = new Entry(dstDN, ocMap, userAttrs, null);

    InternalClientConnection conn =
         InternalClientConnection.getRootConnection();

    AddOperation addOperation = conn.processAdd(addEntry);
    if (addOperation.getResultCode() != ResultCode.SUCCESS)
    {
      Message message = INFO_TRUSTSTORESYNC_ADD_FAILED.get(
           String.valueOf(dstDN),
           String.valueOf(addOperation.getErrorMessage()));
      ErrorLogger.logError(message);
    }
  }


  /**
   * {@inheritDoc}
   */
  public void handleInternalSearchReference(
       InternalSearchOperation searchOperation,
       SearchResultReference searchReference) throws DirectoryException
  {
    // No implementation required.
  }

  /**
   * {@inheritDoc}
   */
  public void handleAddOperation(PostResponseAddOperation addOperation,
                                 Entry entry)
  {
    if (addOperation.getEntryDN().isDescendantOf(instanceKeysDN))
    {
      handleInstanceKeyAddOperation(entry);
    }
    else if (addOperation.getEntryDN().isDescendantOf(secretKeysDN))
    {
      try
      {
        if (entry.hasObjectClass(ocCipherKey))
        {
          DirectoryServer.getCryptoManager().importCipherKeyEntry(entry);
        }
        else if (entry.hasObjectClass(ocMacKey))
        {
          DirectoryServer.getCryptoManager().importMacKeyEntry(entry);
        }
      }
      catch (CryptoManager.CryptoManagerException e)
      {
        Message message = Message.raw("Failed to import key entry: %s",
                                      e.getMessage());
        ErrorLogger.logError(message);
      }
    }
  }


  private void handleInstanceKeyAddOperation(Entry entry)
  {
    RDN srcRDN = entry.getDN().getRDN();

    // Only process the entry if it has the expected form of RDN.
    if (!srcRDN.isMultiValued() &&
         srcRDN.getAttributeType(0).equals(attrAlias))
    {
      DN dstDN = trustStoreRootDN.concat(srcRDN);

      if (!entry.hasAttribute(attrCompromisedTime))
      {
        addEntry(entry, dstDN);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void handleDeleteOperation(PostResponseDeleteOperation deleteOperation,
                                    Entry entry)
  {
    if (!deleteOperation.getEntryDN().isDescendantOf(instanceKeysDN))
    {
      return;
    }

    RDN srcRDN = entry.getDN().getRDN();

    // Only process the entry if it has the expected form of RDN.
    if (!srcRDN.isMultiValued() &&
         srcRDN.getAttributeType(0).equals(attrAlias))
    {
      DN dstDN = trustStoreRootDN.concat(srcRDN);

      deleteEntry(dstDN);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void handleModifyOperation(PostResponseModifyOperation modifyOperation,
                                    Entry oldEntry, Entry newEntry)
  {
    if (modifyOperation.getEntryDN().isDescendantOf(instanceKeysDN))
    {
      handleInstanceKeyModifyOperation(newEntry);
    }
    else if (modifyOperation.getEntryDN().isDescendantOf(secretKeysDN))
    {
      try
      {
        if (newEntry.hasObjectClass(ocCipherKey))
        {
          DirectoryServer.getCryptoManager().importCipherKeyEntry(newEntry);
        }
        else if (newEntry.hasObjectClass(ocMacKey))
        {
          DirectoryServer.getCryptoManager().importMacKeyEntry(newEntry);
        }
      }
      catch (CryptoManager.CryptoManagerException e)
      {
        Message message = Message.raw("Failed to import modified key entry: %s",
                                      e.getMessage());
        ErrorLogger.logError(message);
      }
    }
  }

  private void handleInstanceKeyModifyOperation(Entry newEntry)
  {
    RDN srcRDN = newEntry.getDN().getRDN();

    // Only process the entry if it has the expected form of RDN.
    if (!srcRDN.isMultiValued() &&
         srcRDN.getAttributeType(0).equals(attrAlias))
    {
      DN dstDN = trustStoreRootDN.concat(srcRDN);

      // Get any existing local trust store entry.
      Entry dstEntry = null;
      try
      {
        dstEntry = DirectoryServer.getEntry(dstDN);
      }
      catch (DirectoryException e)
      {
        // ignore
      }

      if (newEntry.hasAttribute(attrCompromisedTime))
      {
        // The key was compromised so we should remove it from the local
        // trust store.
        if (dstEntry != null)
        {
          deleteEntry(dstDN);
        }
      }
      else
      {
        if (dstEntry == null)
        {
          addEntry(newEntry, dstDN);
        }
        else
        {
          modifyEntry(newEntry, dstEntry);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void handleModifyDNOperation(
       PostResponseModifyDNOperation modifyDNOperation, Entry oldEntry,
       Entry newEntry)
  {
    // No implementation required.
  }
}
