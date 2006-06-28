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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.api;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.AddOperation;
import org.opends.server.core.CancelledOperationException;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.InitializationException;
import org.opends.server.core.LockManager;
import org.opends.server.core.ModifyOperation;
import org.opends.server.core.ModifyDNOperation;
import org.opends.server.core.SearchOperation;
import org.opends.server.types.BackupConfig;
import org.opends.server.types.BackupDirectory;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFExportConfig;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.RestoreConfig;
import org.opends.server.types.WritabilityMode;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.BackendMessages.*;
import static org.opends.server.messages.MessageHandler.*;



/**
 * This class defines the set of methods and structures that must be
 * implemented for a Directory Server backend.
 */
public abstract class Backend
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.api.Backend";



  // The backend that holds a portion of the DIT that is
  // hierarchically above the information in this backend.
  private Backend parentBackend;

  // The set of backends that hold portions of the DIT that are
  // hierarchically below the information in this backend.
  private Backend[] subordinateBackends;

  // Indicates whether this is a private backend or one that holds
  // user data.
  private boolean isPrivateBackend;

  // The unique identifier for this backend.
  private String backendID;

  // The writability mode for this backend.
  private WritabilityMode writabilityMode;



  /**
   * Creates a new backend with the provided information.  All backend
   * implementations must implement a default constructor that use
   * <CODE>super()</CODE> to invoke this constructor.
   */
  protected Backend()
  {
    assert debugConstructor(CLASS_NAME);

    backendID           = null;
    parentBackend       = null;
    subordinateBackends = new Backend[0];
    isPrivateBackend    = false;
    writabilityMode     = WritabilityMode.ENABLED;
  }



  /**
   * Initializes this backend based on the information in the provided
   * configuration entry.
   *
   * @param  configEntry  The configuration entry that contains the
   *                      information to use to initialize this
   *                      backend.
   * @param  baseDNs      The set of base DNs that have been
   *                      configured for this backend.
   *
   * @throws  ConfigException  If an unrecoverable problem arises in
   *                           the process of performing the
   *                           initialization.
   *
   * @throws  InitializationException  If a problem occurs during
   *                                   initialization that is not
   *                                   related to the server
   *                                   configuration.
   */
  public abstract void initializeBackend(ConfigEntry configEntry,
                                         DN[] baseDNs)
         throws ConfigException, InitializationException;



  /**
   * Performs any necessary work to finalize this backend, including
   * closing any underlying databases or connections and deregistering
   * any suffixes that it manages with the Directory Server.  This may
   * be called during the Directory Server shutdown process or if a
   * backend is disabled with the server online.  It must not return
   * until the backend is closed.
   * <BR><BR>
   * This method may not throw any exceptions.  If any problems are
   * encountered, then they may be logged but the closure should
   * progress as completely as possible.
   */
  public abstract void finalizeBackend();



  /**
   * Retrieves the set of base-level DNs that may be used within this
   * backend.
   *
   * @return  The set of base-level DNs that may be used within this
   *          backend.
   */
  public abstract DN[] getBaseDNs();



  /**
   * Indicates whether the data associated with this backend may be
   * considered local (i.e., in a repository managed by the Directory
   * Server) rather than remote (i.e., in an external repository
   * accessed by the Directory Server but managed through some other
   * means).
   *
   * @return  <CODE>true</CODE> if the data associated with this
   *          backend may be considered local, or <CODE>false</CODE>
   *          if it is remote.
   */
  public abstract boolean isLocal();



  /**
   * Retrieves the requested entry from this backend.  Note that the
   * caller must hold a read or write lock on the specified DN.
   *
   * @param  entryDN  The distinguished name of the entry to retrieve.
   *
   * @return  The requested entry, or <CODE>null</CODE> if the entry
   *          does not exist.
   *
   * @throws  DirectoryException  If a problem occurs while trying to
   *                              retrieve the entry.
   */
  public abstract Entry getEntry(DN entryDN)
         throws DirectoryException;



  /**
   * Indicates whether an entry with the specified DN exists in the
   * backend. The default implementation obtains a read lock and calls
   * <CODE>getEntry</CODE>, but backend implementations may override
   * this with a more efficient version that does not require a lock.
   * The caller is not required to hold any locks on the specified DN.
   *
   * @param  entryDN  The DN of the entry for which to determine
   *                  existence.
   *
   * @return  <CODE>true</CODE> if the specified entry exists in this
   *          backend, or <CODE>false</CODE> if it does not.
   *
   * @throws  DirectoryException  If a problem occurs while trying to
   *                              make the determination.
   */
  public boolean entryExists(DN entryDN)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "entryExists",
                      String.valueOf(entryDN));

    Lock lock = null;
    for (int i=0; i < 3; i++)
    {
      lock = LockManager.lockRead(entryDN);
      if (lock != null)
      {
        break;
      }
    }

    if (lock == null)
    {
      int    msgID   = MSGID_BACKEND_CANNOT_LOCK_ENTRY;
      String message = getMessage(msgID, String.valueOf(entryDN));
      throw new DirectoryException(
                     DirectoryServer.getServerErrorResultCode(),
                     message, msgID);
    }

    try
    {
      return (getEntry(entryDN) != null);
    }
    finally
    {
      LockManager.unlock(entryDN, lock);
    }
  }



  /**
   * Adds the provided entry to this backend.  This method must ensure
   * that the entry is appropriate for the backend and that no entry
   * already exists with the same DN.  The caller must hold a write
   * lock on the DN of the provided entry.
   *
   * @param  entry         The entry to add to this backend.
   * @param  addOperation  The add operation with which the new entry
   *                       is associated.  This may be
   *                       <CODE>null</CODE> for adds performed
   *                       internally.
   *
   * @throws  DirectoryException  If a problem occurs while trying to
   *                              add the entry.
   *
   * @throws  CancelledOperationException  If this backend noticed and
   *                                       reacted to a request to
   *                                       cancel or abandon the add
   *                                       operation.
   */
  public abstract void addEntry(Entry entry,
                                AddOperation addOperation)
         throws DirectoryException, CancelledOperationException;



  /**
   * Removes the specified entry from this backend.  This method must
   * ensure that the entry exists and that it does not have any
   * subordinate entries (unless the backend supports a subtree delete
   * operation and the client included the appropriate information in
   * the request).  The caller must hold a write lock on the provided
   * entry DN.
   *
   * @param  entryDN          The DN of the entry to remove from this
   *                          backend.
   * @param  deleteOperation  The delete operation with which this
   *                          action is associated.  This may be
   *                          <CODE>null</CODE> for deletes performed
   *                          internally.
   *
   * @throws  DirectoryException  If a problem occurs while trying to
   *                              remove the entry.
   *
   * @throws  CancelledOperationException  If this backend noticed and
   *                                       reacted to a request to
   *                                       cancel or abandon the
   *                                       delete operation.
   */
  public abstract void deleteEntry(DN entryDN,
                                   DeleteOperation deleteOperation)
         throws DirectoryException, CancelledOperationException;



  /**
   * Replaces the specified entry with the provided entry in this
   * backend.  The backend must ensure that an entry already exists
   * with the same DN as the provided entry.  The caller must hold a
   * write lock on the DN of the provided entry.
   *
   * @param  entry            The new entry to use in place of the
   *                          existing entry with the same DN.
   * @param  modifyOperation  The modify operation with which this
   *                          action is associated.  This may be
   *                          <CODE>null</CODE> for modifications
   *                          performed internally.
   *
   * @throws  DirectoryException  If a problem occurs while trying to
   *                              replace the entry.
   *
   * @throws  CancelledOperationException  If this backend noticed and
   *                                       reacted to a request to
   *                                       cancel or abandon the
   *                                       modify operation.
   */
  public abstract void replaceEntry(Entry entry,
                                    ModifyOperation modifyOperation)
         throws DirectoryException, CancelledOperationException;



  /**
   * Moves and/or renames the provided entry in this backend, altering
   * any subordinate entries as necessary.  This must ensure that an
   * entry already exists with the provided current DN, and that no
   * entry exists with the target DN of the provided entry.  The
   * caller must hold write locks on both the current DN and the new
   * DN for the entry.
   *
   * @param  currentDN          The current DN of the entry to be
   *                            replaced.
   * @param  entry              The new content to use for the entry.
   * @param  modifyDNOperation  The modify DN operation with which
   *                            this action is associated.  This may
   *                            be <CODE>null</CODE>
   *                            for modify DN operations performed
   *                            internally.
   *
   * @throws  DirectoryException  If a problem occurs while trying to
   *                              perform the rename.
   *
   * @throws  CancelledOperationException  If this backend noticed and
   *                                       reacted to a request to
   *                                       cancel or abandon the
   *                                       modify DN operation.
   */
  public abstract void renameEntry(DN currentDN, Entry entry,
                            ModifyDNOperation modifyDNOperation)
         throws DirectoryException, CancelledOperationException;



  /**
   * Processes the specified search in this backend.  Matching entries
   * should be provided back to the core server using the
   * <CODE>SearchOperation.returnEntry</CODE> method.  The caller is
   * not required to have any locks when calling this operation.
   *
   * @param  searchOperation  The search operation to be processed.
   *
   * @throws  DirectoryException  If a problem occurs while processing
   *                              the search.
   *
   * @throws  CancelledOperationException  If this backend noticed and
   *                                       reacted to a request to
   *                                       cancel or abandon the
   *                                       search operation.
   */
  public abstract void search(SearchOperation searchOperation)
         throws DirectoryException, CancelledOperationException;



  /**
   * Retrieves the OIDs of the controls that may be supported by this
   * backend.
   *
   * @return  The OIDs of the controls that may be supported by this
   *          backend.
   */
  public abstract Set<String> getSupportedControls();



  /**
   * Indicates whether this backend supports the specified control.
   *
   * @param  controlOID  The OID of the control for which to make the
   *                     determination.
   *
   * @return  <CODE>true</CODE> if this backend does support the
   *          requested control, or <CODE>false</CODE>
   */
  public abstract boolean supportsControl(String controlOID);



  /**
   * Retrieves the OIDs of the features that may be supported by this
   * backend.
   *
   * @return  The OIDs of the features that may be supported by this
   *          backend.
   */
  public abstract Set<String> getSupportedFeatures();



  /**
   * Indicates whether this backend supports the specified feature.
   *
   * @param  featureOID  The OID of the feature for which to make the
   *                     determination.
   *
   * @return  <CODE>true</CODE> if this backend does support the
   *          requested feature, or <CODE>false</CODE>
   */
  public abstract boolean supportsFeature(String featureOID);



  /**
   * Indicates whether this backend provides a mechanism to export the
   * data it contains to an LDIF file.
   *
   * @return  <CODE>true</CODE> if this backend provides an LDIF
   *          export mechanism, or <CODE>false</CODE> if not.
   */
  public abstract boolean supportsLDIFExport();



  /**
   * Exports the contents of this backend to LDIF.  This method should
   * only be called if <CODE>supportsLDIFExport</CODE> returns
   * <CODE>true</CODE>.  Note that the server will not explicitly
   * initialize this backend before calling this method.
   *
   * @param  configEntry   The configuration entry for this backend.
   * @param  baseDNs       The set of base DNs configured for this
   *                       backend.
   * @param  exportConfig  The configuration to use when performing
   *                       the export.
   *
   * @throws  DirectoryException  If a problem occurs while performing
   *                              the LDIF export.
   */
  public abstract void exportLDIF(ConfigEntry configEntry,
                                  DN[] baseDNs,
                                  LDIFExportConfig exportConfig)
         throws DirectoryException;



  /**
   * Indicates whether this backend provides a mechanism to import its
   * data from an LDIF file.
   *
   * @return  <CODE>true</CODE> if this backend provides an LDIF
   *          import mechanism, or <CODE>false</CODE> if not.
   */
  public abstract boolean supportsLDIFImport();



  /**
   * Imports information from an LDIF file into this backend.  This
   * method should only be called if <CODE>supportsLDIFImport</CODE>
   * returns <CODE>true</CODE>.  Note that the server will not
   * explicitly initialize this backend before calling this method.
   *
   * @param  configEntry   The configuration entry for this backend.
   * @param  baseDNs       The set of base DNs configured for this
   *                       backend.
   * @param  importConfig  The configuration to use when performing
   *                       the import.
   *
   * @throws  DirectoryException  If a problem occurs while performing
   *                              the LDIF import.
   */
  public abstract void importLDIF(ConfigEntry configEntry,
                                  DN[] baseDNs,
                                  LDIFImportConfig importConfig)
         throws DirectoryException;



  /**
   * Indicates whether this backend provides a backup mechanism of any
   * kind.  This method is used by the backup process when backing up
   * all backends to determine whether this backend is one that should
   * be skipped.  It should only return <CODE>true</CODE> for backends
   * that it is not possible to archive directly (e.g., those that
   * don't store their data locally, but rather pass through requests
   * to some other repository).
   *
   * @return  <CODE>true</CODE> if this backend provides any kind of
   *          backup mechanism, or <CODE>false</CODE> if it does not.
   */
  public abstract boolean supportsBackup();



  /**
   * Indicates whether this backend provides a mechanism to perform a
   * backup of its contents in a form that can be restored later,
   * based on the provided configuration.
   *
   * @param  backupConfig       The configuration of the backup for
   *                            which to make the determination.
   * @param  unsupportedReason  A buffer to which a message can be
   *                            appended
   *                            explaining why the requested backup is
   *                            not supported.
   *
   * @return  <CODE>true</CODE> if this backend provides a mechanism
   *          for performing backups with the provided configuration,
   *          or <CODE>false</CODE> if not.
   */
  public abstract boolean supportsBackup(BackupConfig backupConfig,
                               StringBuilder unsupportedReason);



  /**
   * Creates a backup of the contents of this backend in a form that
   * may be restored at a later date if necessary.  This method should
   * only be called if <CODE>supportsBackup</CODE> returns
   * <CODE>true</CODE>.  Note that the server will not explicitly
   * initialize this backend before calling this method.
   *
   * @param  configEntry   The configuration entry for this backend.
   * @param  backupConfig  The configuration to use when performing
   *                       the backup.
   *
   * @throws  DirectoryException  If a problem occurs while performing
   *                              the backup.
   */
  public abstract void createBackup(ConfigEntry configEntry,
                                    BackupConfig backupConfig)
         throws DirectoryException;



  /**
   * Removes the specified backup if it is possible to do so.
   *
   * @param  backupDirectory  The backup directory structure with
   *                          which the specified backup is
   *                          associated.
   * @param  backupID         The backup ID for the backup to be
   *                          removed.
   *
   * @throws  DirectoryException  If it is not possible to remove the
   *                              specified backup for some reason
   *                              (e.g., no such backup exists or
   *                              there are other backups that are
   *                              dependent upon it).
   */
  public abstract void removeBackup(BackupDirectory backupDirectory,
                                    String backupID)
         throws DirectoryException;



  /**
   * Indicates whether this backend provides a mechanism to restore a
   * backup.
   *
   * @return  <CODE>true</CODE> if this backend provides a mechanism
   *          for restoring backups, or <CODE>false</CODE> if not.
   */
  public abstract boolean supportsRestore();



  /**
   * Restores a backup of the contents of this backend.  This method
   * should only be called if <CODE>supportsRestore</CODE> returns
   * <CODE>true</CODE>.  Note that the server will not explicitly
   * initialize this backend before calling this method.
   *
   * @param  configEntry    The configuration entry for this backend.
   * @param  restoreConfig  The configuration to use when performing
   *                        the restore.
   *
   * @throws  DirectoryException  If a problem occurs while performing
   *                              the restore.
   */
  public abstract void restoreBackup(ConfigEntry configEntry,
                                     RestoreConfig restoreConfig)
         throws DirectoryException;



  /**
   * Retrieves the unique identifier for this backend.
   *
   * @return  The unique identifier for this backend.
   */
  public String getBackendID()
  {
    assert debugEnter(CLASS_NAME, "getBackendID");

    return backendID;
  }



  /**
   * Specifies the unique identifier for this backend.
   *
   * @param  backendID  The unique identifier for this backend.
   */
  public void setBackendID(String backendID)
  {
    assert debugEnter(CLASS_NAME, "setBackendID",
                      String.valueOf(backendID));

    this.backendID = backendID;
  }



  /**
   * Indicates whether this backend holds private data or user data.
   *
   * @return  <CODE>true</CODE> if this backend holds private data, or
   *          <CODE>false</CODE> if it holds user data.
   */
  public boolean isPrivateBackend()
  {
    assert debugEnter(CLASS_NAME, "isPrivateBackend");

    return isPrivateBackend;
  }



  /**
   * Specifies whether this backend holds private data or user data.
   *
   * @param  isPrivateBackend  Specifies whether this backend holds
   *                           private data or user data.
   */
  public void setPrivateBackend(boolean isPrivateBackend)
  {
    this.isPrivateBackend = isPrivateBackend;
  }



  /**
   * Retrieves the writability mode for this backend.
   *
   * @return  The writability mode for this backend.
   */
  public WritabilityMode getWritabilityMode()
  {
    assert debugEnter(CLASS_NAME, "getWritabilityMode");

    return writabilityMode;
  }



  /**
   * Specifies the writability mode for this backend.
   *
   * @param  writabilityMode  The writability mode for this backend.
   */
  public void setWritabilityMode(WritabilityMode writabilityMode)
  {
    assert debugEnter(CLASS_NAME, "setWritabilityMode",
                      String.valueOf(writabilityMode));

    if (writabilityMode == null)
    {
      this.writabilityMode = WritabilityMode.ENABLED;
    }
    else
    {
      this.writabilityMode = writabilityMode;
    }
  }



  /**
   * Retrieves the parent backend for this backend.
   *
   * @return  The parent backend for this backend, or
   *          <CODE>null</CODE> if there is none.
   */
  public Backend getParentBackend()
  {
    assert debugEnter(CLASS_NAME, "getParentBackend");

    return parentBackend;
  }



  /**
   * Specifies the parent backend for this backend.
   *
   * @param  parentBackend  The parent backend for this backend.
   */
  public void setParentBackend(Backend parentBackend)
  {
    assert debugEnter(CLASS_NAME, "setParentBackend",
                      String.valueOf(parentBackend));

    synchronized (this)
    {
      this.parentBackend = parentBackend;
    }
  }



  /**
   * Retrieves the set of subordinate backends for this backend.
   *
   * @return  The set of subordinate backends for this backend, or an
   *          empty array if none exist.
   */
  public Backend[] getSubordinateBackends()
  {
    assert debugEnter(CLASS_NAME, "getSubordinateBackends");

    return subordinateBackends;
  }



  /**
   * Specifies the set of subordinate backends for this backend.
   *
   * @param  subordinateBackends  The set of subordinate backends for
   *                              this backend.
   */
  public void setSubordinateBackends(Backend[] subordinateBackends)
  {
    assert debugEnter(CLASS_NAME, "setSubordinateBackends",
                      String.valueOf(subordinateBackends));

    synchronized (this)
    {
      this.subordinateBackends = subordinateBackends;
    }
  }



  /**
   * Indicates whether this backend has a subordinate backend
   * registered with the provided base DN.  This may check recursively
   * if a subordinate backend has its own subordinate backends.
   *
   * @param  subSuffixDN  The DN of the sub-suffix for which to make
   *                      the determination.
   *
   * @return  <CODE>true</CODE> if this backend has a subordinate
   *          backend registered with the provided base DN, or
   *          <CODE>false</CODE> if it does not.
   */
  public boolean hasSubSuffix(DN subSuffixDN)
  {
    assert debugEnter(CLASS_NAME, "hasSubSuffix",
                      String.valueOf(subSuffixDN));

    Backend[] subBackends = subordinateBackends;
    for (Backend b : subBackends)
    {
      for (DN baseDN : b.getBaseDNs())
      {
        if (baseDN.equals(subSuffixDN))
        {
          return true;
        }
      }

      if (b.hasSubSuffix(subSuffixDN))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Removes the backend associated with the specified sub-suffix if
   * it is registered.  This may check recursively if a subordinate
   * backend has its own subordinate backends.
   *
   * @param  subSuffixDN  The DN of the sub-suffix to remove from this
   *                      backend.
   * @param  parentDN     The superior DN for the sub-suffix DN that
   *                      matches one of the subordinate base DNs for
   *                      this backend.
   *
   * @throws  ConfigException  If the sub-suffix exists but it is not
   *                           possible to remove it for some reason.
   */
  public void removeSubSuffix(DN subSuffixDN, DN parentDN)
         throws ConfigException
  {
    assert debugEnter(CLASS_NAME, "removeSubSuffix",
                      String.valueOf(subSuffixDN));

    synchronized (this)
    {
      boolean matchFound = false;
      ArrayList<Backend> subBackendList =
           new ArrayList<Backend>(subordinateBackends.length);
      for (Backend b : subordinateBackends)
      {
        boolean thisMatches = false;
        DN[] subBaseDNs = b.getBaseDNs();
        for (int i=0; i < subBaseDNs.length; i++)
        {
          if (subBaseDNs[i].equals(subSuffixDN))
          {
            if (subBaseDNs.length > 1)
            {
              int msgID =
                   MSGID_BACKEND_CANNOT_REMOVE_MULTIBASE_SUB_SUFFIX;
              String message = getMessage(msgID,
                                    String.valueOf(subSuffixDN));
              throw new ConfigException(msgID, message);
            }

            thisMatches = true;
            matchFound  = true;
            break;
          }
        }

        if (! thisMatches)
        {
          if (b.hasSubSuffix(subSuffixDN))
          {
            b.removeSubSuffix(subSuffixDN, parentDN);
          }
          else
          {
            subBackendList.add(b);
          }
        }
      }

      if (matchFound)
      {
        Backend[] newSubordinateBackends =
             new Backend[subBackendList.size()];
        subBackendList.toArray(newSubordinateBackends);
        subordinateBackends = newSubordinateBackends;
      }
    }
  }



  /**
   * Adds the provided backend to the set of subordinate backends for
   * this backend.
   *
   * @param  subordinateBackend  The backend to add to the set of
   *                             subordinate backends for this
   *                             backend.
   */
  public void addSubordinateBackend(Backend subordinateBackend)
  {
    assert debugEnter(CLASS_NAME, "addSubordinateBackend",
                      String.valueOf(subordinateBackend));

    synchronized (this)
    {
      Backend[] newSubordinateBackends =
           new Backend[subordinateBackends.length+1];

      System.arraycopy(subordinateBackends, 0, newSubordinateBackends,
                       0, subordinateBackends.length);
      newSubordinateBackends[subordinateBackends.length] =
           subordinateBackend;

      subordinateBackends = newSubordinateBackends;
    }
  }



  /**
   * Indicates whether this backend should be used to handle
   * operations for the provided entry.
   *
   * @param  entryDN  The DN of the entry for which to make the
   *                  determination.
   *
   * @return  <CODE>true</CODE> if this backend handles operations for
   *          the provided entry, or <CODE>false</CODE> if it does
   *          not.
   */
  public boolean handlesEntry(DN entryDN)
  {
    assert debugEnter(CLASS_NAME, "handlesEntry",
                      String.valueOf(entryDN));

    DN[] baseDNs = getBaseDNs();
    for (int i=0; i < baseDNs.length; i++)
    {
      if (entryDN.isDescendantOf(baseDNs[i]))
      {
        Backend[] subBackends = subordinateBackends;
        for (int j=0; j < subBackends.length; j++)
        {
          if (subBackends[j].handlesEntry(entryDN))
          {
            return false;
          }
        }

        return true;
      }
    }

    return false;
  }



  /**
   * Indicates whether a backend should be used to handle operations
   * for the provided entry given the set of base DNs and exclude DNs.
   *
   * @param  entryDN     The DN of the entry for which to make the
   *                     determination.
   * @param  baseDNs     The set of base DNs for the backend.
   * @param  excludeDNs  The set of DNs that should be excluded from
   *                     the backend.
   *
   * @return  <CODE>true</CODE> if the backend should handle
   *          operations for the provided entry, or <CODE>false</CODE>
   *          if it does not.
   */
  public static final boolean handlesEntry(DN entryDN,
                                           List<DN> baseDNs,
                                           List<DN> excludeDNs)
  {
    assert debugEnter(CLASS_NAME, "handlesEntry",
                      String.valueOf(entryDN));

    for (DN baseDN : baseDNs)
    {
      if (entryDN.isDescendantOf(baseDN))
      {
        if ((excludeDNs == null) || excludeDNs.isEmpty())
        {
          return true;
        }

        boolean isExcluded = false;
        for (DN excludeDN : excludeDNs)
        {
          if (entryDN.isDescendantOf(excludeDN))
          {
            isExcluded = true;
            break;
          }
        }

        if (! isExcluded)
        {
          return true;
        }
      }
    }

    return false;
  }
}

