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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.backends.jeb;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;

import org.opends.server.admin.std.server.MonitorProviderCfg;
import org.opends.server.api.Backend;
import org.opends.server.api.MonitorProvider;
import org.opends.server.config.ConfigException;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.ModifyOperation;
import org.opends.server.core.ModifyDNOperation;
import org.opends.server.core.SearchOperation;
import org.opends.server.util.LDIFException;
import org.opends.server.util.Validator;
import static org.opends.server.util.StaticUtils.*;

import static org.opends.server.messages.BackendMessages.*;
import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.messages.JebMessages.*;
import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.loggers.debug.DebugLogger.*;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.*;
import static org.opends.server.util.ServerConstants.*;
import org.opends.server.admin.std.server.JEBackendCfg;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.Configuration;

/**
 * This is an implementation of a Directory Server Backend which stores entries
 * locally in a Sleepycat JE database.
 */
public class BackendImpl
    extends Backend
    implements ConfigurationChangeListener<JEBackendCfg>
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();


  /**
   * The configuration of this JE backend.
   */
  private JEBackendCfg cfg;

  /**
   * The root JE container to use for this backend.
   */
  private RootContainer rootContainer;

  /**
   * A count of the total operation threads currently in the backend.
   */
  private AtomicInteger threadTotalCount = new AtomicInteger(0);

  /**
   * A count of the write operation threads currently in the backend.
   */
  private AtomicInteger threadWriteCount = new AtomicInteger(0);

  /**
   * A list of monitor providers created for this backend instance.
   */
  private ArrayList<MonitorProvider> monitorProviders =
      new ArrayList<MonitorProvider>();

  /**
   * The controls supported by this backend.
   */
  private static HashSet<String> supportedControls;



  static
  {
    // Set our supported controls.
    supportedControls = new HashSet<String>();
    supportedControls.add(OID_SUBTREE_DELETE_CONTROL);
    supportedControls.add(OID_PAGED_RESULTS_CONTROL);
    supportedControls.add(OID_MANAGE_DSAIT_CONTROL);
    supportedControls.add(OID_SERVER_SIDE_SORT_REQUEST_CONTROL);
    supportedControls.add(OID_VLV_REQUEST_CONTROL);
  }



  /**
   * Begin a Backend API method that reads the database.
   */
  private void readerBegin()
  {
    threadTotalCount.getAndIncrement();
  }



  /**
   * End a Backend API method that reads the database.
   */
  private void readerEnd()
  {
    threadTotalCount.getAndDecrement();
  }



  /**
   * Begin a Backend API method that writes the database.
   */
  private void writerBegin()
  {
    threadTotalCount.getAndIncrement();
    threadWriteCount.getAndIncrement();
  }



  /**
   * End a Backend API method that writes the database.
   */
  private void writerEnd()
  {
    threadWriteCount.getAndDecrement();
    threadTotalCount.getAndDecrement();
  }



  /**
   * Wait until there are no more threads accessing the database. It is assumed
   * that new threads have been prevented from entering the database at the time
   * this method is called.
   */
  private void waitUntilQuiescent()
  {
    while (threadTotalCount.get() > 0)
    {
      // Still have threads in the database so sleep a little
      try
      {
        Thread.sleep(500);
      }
      catch (InterruptedException e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
      }
    }
  }

  /**
   * This method will attempt to checksum the current JE db environment by
   * computing the Adler-32 checksum on the latest JE log file available.
   *
   * @return  The checksum of JE db environment or zero if checksum failed.
   */
  private long checksumDbEnv() {

    File backendDirectory = getFileForPath(cfg.getBackendDirectory());
    List<File> jdbFiles = new ArrayList<File>();
    if(backendDirectory.isDirectory())
    {
      jdbFiles =
          Arrays.asList(backendDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return name.endsWith(".jdb");
            }
          }));
    }

    if ( !jdbFiles.isEmpty() ) {
      Collections.sort(jdbFiles, Collections.reverseOrder());
      FileInputStream fis = null;
      try {
        fis = new FileInputStream(jdbFiles.get(0).toString());
        CheckedInputStream cis = new CheckedInputStream(fis, new Adler32());
        byte[] tempBuf = new byte[8192];
        while (cis.read(tempBuf) >= 0) {
        }

        return cis.getChecksum().getValue();
      } catch (Exception e) {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
      } finally {
        if (fis != null) {
          try {
            fis.close();
          } catch (Exception e) {
            if (debugEnabled())
            {
              TRACER.debugCaught(DebugLogLevel.ERROR, e);
            }
          }
        }
      }
    }

    return 0;
  }

  /**
   * This method constructs a container name from a base DN. Only alphanumeric
   * characters are preserved, all other characters are replaced with an
   * underscore.
   *
   * @param dn The base DN.
   * @return The container name for the base DN.
   */
  public static String getContainerName(DN dn)
  {
    String normStr = dn.toNormalizedString();
    StringBuilder builder = new StringBuilder(normStr.length());
    for (int i = 0; i < normStr.length(); i++)
    {
      char ch = normStr.charAt(i);
      if (Character.isLetterOrDigit(ch))
      {
        builder.append(ch);
      }
      else
      {
        builder.append('_');
      }
    }
    return builder.toString();
  }



  /**
   * {@inheritDoc}
   */
  public void configureBackend(Configuration cfg)
      throws ConfigException
  {
    Validator.ensureNotNull(cfg);
    Validator.ensureTrue(cfg instanceof JEBackendCfg);

    this.cfg = (JEBackendCfg)cfg;
  }



  /**
   * {@inheritDoc}
   */
  public void initializeBackend()
      throws ConfigException, InitializationException
  {
    // Checksum this db environment and register its offline state id/checksum.
    DirectoryServer.registerOfflineBackendStateID(this.getBackendID(),
                                                  checksumDbEnv());

    if(rootContainer == null)
    {
      EnvironmentConfig envConfig =
          ConfigurableEnvironment.parseConfigEntry(cfg);

      initializeRootContainer(envConfig);
    }

    // Preload the database cache.
    rootContainer.preload(cfg.getBackendPreloadTimeLimit());

    try
    {
      // Log an informational message about the number of entries.
      int msgID = MSGID_JEB_BACKEND_STARTED;
      String message = getMessage(msgID, cfg.getBackendId(),
                                  rootContainer.getEntryCount());
      logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.NOTICE, message,
               msgID);
    }
    catch(DatabaseException databaseException)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, databaseException);
      }
      String message = getMessage(MSGID_JEB_GET_ENTRY_COUNT_FAILED,
                                  databaseException.getMessage());
      throw new InitializationException(MSGID_JEB_GET_ENTRY_COUNT_FAILED,
                                        message, databaseException);
    }

    for (DN dn : cfg.getBackendBaseDN())
    {
      try
      {
        DirectoryServer.registerBaseDN(dn, this, false, false);
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }

        int msgID = MSGID_BACKEND_CANNOT_REGISTER_BASEDN;
        String message = getMessage(msgID, String.valueOf(dn),
                                    String.valueOf(e));
        throw new InitializationException(msgID, message, e);
      }
    }

    // Register a monitor provider for the environment.
    MonitorProvider<? extends MonitorProviderCfg> monitorProvider =
        rootContainer.getMonitorProvider();
    monitorProviders.add(monitorProvider);
    DirectoryServer.registerMonitorProvider(monitorProvider);

    // Register this backend as a change listener.
    cfg.addJEChangeListener(this);
  }

  /**
   * Performs any necessary work to finalize this backend, including closing any
   * underlying databases or connections and deregistering any suffixes that it
   * manages with the Directory Server.  This may be called during the Directory
   * Server shutdown process or if a backend is disabled with the server online.
   * It must not return until the backend is closed. <BR><BR> This method may
   * not throw any exceptions.  If any problems are encountered, then they may
   * be logged but the closure should progress as completely as possible.
   */
  public void finalizeBackend()
  {
    // Deregister as a change listener.
    cfg.removeJEChangeListener(this);

    // Deregister our base DNs.
    for (DN dn : rootContainer.getBaseDNs())
    {
      try
      {
        DirectoryServer.deregisterBaseDN(dn, false);
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
      }
    }

    // Deregister our monitor providers.
    for (MonitorProvider monitor : monitorProviders)
    {
      DirectoryServer.deregisterMonitorProvider(
           monitor.getMonitorInstanceName().toLowerCase());
    }
    monitorProviders = new ArrayList<MonitorProvider>();

    // We presume the server will prevent more operations coming into this
    // backend, but there may be existing operations already in the
    // backend. We need to wait for them to finish.
    waitUntilQuiescent();

    // Close the database.
    try
    {
      rootContainer.close();
      rootContainer = null;
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      int msgID = MSGID_JEB_DATABASE_EXCEPTION;
      String message = getMessage(msgID, e.getMessage());
      logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.SEVERE_ERROR,
               message, msgID);
    }

    // Checksum this db environment and register its offline state id/checksum.
    DirectoryServer.registerOfflineBackendStateID(this.getBackendID(),
      checksumDbEnv());

    // Make sure the thread counts are zero for next initialization.
    threadTotalCount.set(0);
    threadWriteCount.set(0);
  }



  /**
   * Indicates whether the data associated with this backend may be considered
   * local (i.e., in a repository managed by the Directory Server) rather than
   * remote (i.e., in an external repository accessed by the Directory Server
   * but managed through some other means).
   *
   * @return <CODE>true</CODE> if the data associated with this backend may be
   *         considered local, or <CODE>false</CODE> if it is remote.
   */
  public boolean isLocal()
  {
    return true;
  }



  /**
   * Indicates whether this backend provides a mechanism to export the data it
   * contains to an LDIF file.
   *
   * @return <CODE>true</CODE> if this backend provides an LDIF export
   *         mechanism, or <CODE>false</CODE> if not.
   */
  public boolean supportsLDIFExport()
  {
    return true;
  }



  /**
   * Indicates whether this backend provides a mechanism to import its data from
   * an LDIF file.
   *
   * @return <CODE>true</CODE> if this backend provides an LDIF import
   *         mechanism, or <CODE>false</CODE> if not.
   */
  public boolean supportsLDIFImport()
  {
    return true;
  }



  /**
   * Indicates whether this backend provides a backup mechanism of any kind.
   * This method is used by the backup process when backing up all backends to
   * determine whether this backend is one that should be skipped.  It should
   * only return <CODE>true</CODE> for backends that it is not possible to
   * archive directly (e.g., those that don't store their data locally, but
   * rather pass through requests to some other repository).
   *
   * @return <CODE>true</CODE> if this backend provides any kind of backup
   *         mechanism, or <CODE>false</CODE> if it does not.
   */
  public boolean supportsBackup()
  {
    return true;
  }



  /**
   * Indicates whether this backend provides a mechanism to perform a backup of
   * its contents in a form that can be restored later, based on the provided
   * configuration.
   *
   * @param backupConfig      The configuration of the backup for which to make
   *                          the determination.
   * @param unsupportedReason A buffer to which a message can be appended
   *                          explaining why the requested backup is not
   *                          supported.
   * @return <CODE>true</CODE> if this backend provides a mechanism for
   *         performing backups with the provided configuration, or
   *         <CODE>false</CODE> if not.
   */
  public boolean supportsBackup(BackupConfig backupConfig,
                                StringBuilder unsupportedReason)
  {
    return true;
  }



  /**
   * Indicates whether this backend provides a mechanism to restore a backup.
   *
   * @return <CODE>true</CODE> if this backend provides a mechanism for
   *         restoring backups, or <CODE>false</CODE> if not.
   */
  public boolean supportsRestore()
  {
    return true;
  }



  /**
   * Retrieves the OIDs of the features that may be supported by this backend.
   *
   * @return The OIDs of the features that may be supported by this backend.
   */
  public HashSet<String> getSupportedFeatures()
  {
    return new HashSet<String>();  //NYI
  }



  /**
   * Retrieves the OIDs of the controls that may be supported by this backend.
   *
   * @return The OIDs of the controls that may be supported by this backend.
   */
  public HashSet<String> getSupportedControls()
  {
    return supportedControls;
  }



  /**
   * Retrieves the set of base-level DNs that may be used within this backend.
   *
   * @return The set of base-level DNs that may be used within this backend.
   */
  public DN[] getBaseDNs()
  {
    Set<DN> dnSet = cfg.getBackendBaseDN();
    DN[] baseDNs = new DN[dnSet.size()];
    return dnSet.toArray(baseDNs);
  }



  /**
   * {@inheritDoc}
   */
  public long getEntryCount()
  {
    if (rootContainer != null)
    {
      try
      {
        return rootContainer.getEntryCount();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
      }
    }

    return -1;
  }



  /**
   * Retrieves the requested entry from this backend.  Note that the caller must
   * hold a read or write lock on the specified DN.
   *
   * @param entryDN The distinguished name of the entry to retrieve.
   * @return The requested entry, or <CODE>null</CODE> if the entry does not
   *         exist.
   * @throws DirectoryException If a problem occurs while trying to retrieve the
   *                            entry.
   */
  public Entry getEntry(DN entryDN) throws DirectoryException
  {
    readerBegin();
    EntryContainer ec = rootContainer.getEntryContainer(entryDN);
    ec.sharedLock.lock();
    Entry entry;
    try
    {
      entry = ec.getEntry(entryDN);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      ec.sharedLock.unlock();
      readerEnd();
    }

    return entry;
  }



  /**
   * Adds the provided entry to this backend.  This method must ensure that the
   * entry is appropriate for the backend and that no entry already exists with
   * the same DN.
   *
   * @param entry        The entry to add to this backend.
   * @param addOperation The add operation with which the new entry is
   *                     associated.  This may be <CODE>null</CODE> for adds
   *                     performed internally.
   * @throws DirectoryException If a problem occurs while trying to add the
   *                            entry.
   */
  public void addEntry(Entry entry, AddOperation addOperation)
      throws DirectoryException
  {
    writerBegin();
    DN entryDN = entry.getDN();
    EntryContainer ec = rootContainer.getEntryContainer(entryDN);
    ec.sharedLock.lock();
    try
    {
      ec.addEntry(entry, addOperation);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      ec.sharedLock.unlock();
      writerEnd();
    }
  }



  /**
   * Removes the specified entry from this backend.  This method must ensure
   * that the entry exists and that it does not have any subordinate entries
   * (unless the backend supports a subtree delete operation and the client
   * included the appropriate information in the request).
   *
   * @param entryDN         The DN of the entry to remove from this backend.
   * @param deleteOperation The delete operation with which this action is
   *                        associated.  This may be <CODE>null</CODE> for
   *                        deletes performed internally.
   * @throws DirectoryException If a problem occurs while trying to remove the
   *                            entry.
   */
  public void deleteEntry(DN entryDN, DeleteOperation deleteOperation)
      throws DirectoryException
  {
    writerBegin();

    EntryContainer ec = rootContainer.getEntryContainer(entryDN);
    ec.sharedLock.lock();
    try
    {
      ec.deleteEntry(entryDN, deleteOperation);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      ec.sharedLock.unlock();
      writerEnd();
    }
  }



  /**
   * Replaces the specified entry with the provided entry in this backend.  The
   * backend must ensure that an entry already exists with the same DN as the
   * provided entry.
   *
   * @param entry           The new entry to use in place of the existing entry
   *                        with the same DN.
   * @param modifyOperation The modify operation with which this action is
   *                        associated.  This may be <CODE>null</CODE> for
   *                        modifications performed internally.
   * @throws DirectoryException If a problem occurs while trying to replace the
   *                            entry.
   */
  public void replaceEntry(Entry entry, ModifyOperation modifyOperation)
      throws DirectoryException
  {
    writerBegin();

    DN entryDN = entry.getDN();
    EntryContainer ec = rootContainer.getEntryContainer(entryDN);
    ec.sharedLock.lock();

    try
    {
      ec.replaceEntry(entry, modifyOperation);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      ec.sharedLock.unlock();
      writerEnd();
    }
  }



  /**
   * Moves and/or renames the provided entry in this backend, altering any
   * subordinate entries as necessary.  This must ensure that an entry already
   * exists with the provided current DN, and that no entry exists with the
   * target DN of the provided entry.  The caller must hold write locks on both
   * the current DN and the new DN for the entry.
   *
   * @param currentDN         The current DN of the entry to be replaced.
   * @param entry             The new content to use for the entry.
   * @param modifyDNOperation The modify DN operation with which this action is
   *                          associated.  This may be <CODE>null</CODE> for
   *                          modify DN operations performed internally.
   * @throws org.opends.server.types.DirectoryException
   *          If a problem occurs while trying to perform the rename.
   * @throws org.opends.server.types.CancelledOperationException
   *          If this backend noticed and reacted to a request to cancel or
   *          abandon the modify DN operation.
   */
  public void renameEntry(DN currentDN, Entry entry,
                          ModifyDNOperation modifyDNOperation)
      throws DirectoryException, CancelledOperationException
  {
    writerBegin();
    EntryContainer currentContainer = rootContainer.getEntryContainer(
        currentDN);
    EntryContainer container = rootContainer.getEntryContainer(entry.getDN());

    if (currentContainer != container)
    {
      // FIXME: No reason why we cannot implement a move between containers
      // since the containers share the same database environment.
      int msgID = MSGID_JEB_FUNCTION_NOT_SUPPORTED;
      String msg = getMessage(MSGID_JEB_FUNCTION_NOT_SUPPORTED);
      throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM,
                                   msg, msgID);
    }
    try
    {
      currentContainer.sharedLock.lock();

      currentContainer.renameEntry(currentDN, entry, modifyDNOperation);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION, e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      currentContainer.sharedLock.unlock();
      writerEnd();
    }
  }



  /**
   * Processes the specified search in this backend.  Matching entries should be
   * provided back to the core server using the
   * <CODE>SearchOperation.returnEntry</CODE> method.
   *
   * @param searchOperation The search operation to be processed.
   * @throws org.opends.server.types.DirectoryException
   *          If a problem occurs while processing the search.
   */
  public void search(SearchOperation searchOperation)
      throws DirectoryException
  {
    readerBegin();
    EntryContainer ec = rootContainer.getEntryContainer(
        searchOperation.getBaseDN());
    ec.sharedLock.lock();

    try
    {
      ec.search(searchOperation);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION, e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    finally
    {
      ec.sharedLock.unlock();
      readerEnd();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void exportLDIF(LDIFExportConfig exportConfig)
      throws DirectoryException
  {
    // If the backend already has the root container open, we must use the same
    // underlying root container
    boolean openRootContainer = rootContainer == null;

    try
    {
      if(openRootContainer)
      {
        EnvironmentConfig envConfig =
            ConfigurableEnvironment.parseConfigEntry(cfg);

        envConfig.setReadOnly(true);
        envConfig.setAllowCreate(false);
        envConfig.setTransactional(false);
        envConfig.setTxnNoSync(false);
        envConfig.setConfigParam("je.env.isLocking", "true");
        envConfig.setConfigParam("je.env.runCheckpointer", "true");

        initializeRootContainer(envConfig);
      }


      ExportJob exportJob = new ExportJob(exportConfig);
      exportJob.exportLDIF(rootContainer);
    }
    catch (IOException ioe)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, ioe);
      }
      int msgID = MSGID_JEB_IO_ERROR;
      String message = getMessage(msgID, ioe.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }
    catch (JebException je)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, je);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   je.getMessage(),
                                   je.getMessageID());
    }
    catch (DatabaseException de)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, de);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  de.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (LDIFException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    catch (InitializationException ie)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, ie);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   ie.getMessage(),
                                   ie.getMessageID());
    }
    catch (ConfigException ce)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, ce);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   ce.getMessage(),
                                   ce.getMessageID());
    }
    finally
    {
      //If a root container was opened in this method as read only, close it
      //to leave the backend in the same state.
      if (openRootContainer && rootContainer != null)
      {
        try
        {
          rootContainer.close();
          rootContainer = null;
        }
        catch (DatabaseException e)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  public LDIFImportResult importLDIF(LDIFImportConfig importConfig)
      throws DirectoryException
  {
    // If the backend already has the root container open, we must use the same
    // underlying root container
    boolean openRootContainer = rootContainer == null;

    // If the rootContainer is open, the backend is initialized by something
    // else.
    // We can't do import while the backend is online.
    if(!openRootContainer)
    {
      String message = getMessage(MSGID_JEB_IMPORT_BACKEND_ONLINE);
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_IMPORT_BACKEND_ONLINE);
    }

    try
    {
      EnvironmentConfig envConfig =
          ConfigurableEnvironment.parseConfigEntry(cfg);
      /**
       envConfig.setConfigParam("je.env.runCleaner", "false");
       envConfig.setConfigParam("je.log.numBuffers", "2");
       envConfig.setConfigParam("je.log.bufferSize", "15000000");
       envConfig.setConfigParam("je.log.totalBufferBytes", "30000000");
       envConfig.setConfigParam("je.log.fileMax", "100000000");
       **/

      if (importConfig.appendToExistingData())
      {
        envConfig.setReadOnly(false);
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        envConfig.setTxnNoSync(true);
        envConfig.setConfigParam("je.env.isLocking", "true");
        envConfig.setConfigParam("je.env.runCheckpointer", "false");
      }
      else
      {
        // We have the writer lock on the environment, now delete the
        // environment and re-open it. Only do this when we are
        // importing to all the base DNs in the backend.

        File backendDirectory = getFileForPath(cfg.getBackendDirectory());
        EnvManager.removeFiles(backendDirectory.getPath());
        envConfig.setReadOnly(false);
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        envConfig.setTxnNoSync(false);
        envConfig.setConfigParam("je.env.isLocking", "false");
        envConfig.setConfigParam("je.env.runCheckpointer", "false");
      }

      initializeRootContainer(envConfig);

      ImportJob importJob = new ImportJob(importConfig);
      return importJob.importLDIF(rootContainer);
    }
    catch (IOException ioe)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, ioe);
      }
      int msgID = MSGID_JEB_IO_ERROR;
      String message = getMessage(msgID, ioe.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }
    catch (JebException je)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, je);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   je.getMessage(),
                                   je.getMessageID());
    }
    catch (DatabaseException de)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, de);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  de.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (InitializationException ie)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, ie);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   ie.getMessage(),
                                   ie.getMessageID());
    }
    catch (ConfigException ce)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, ce);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   ce.getMessage(),
                                   ce.getMessageID());
    }
    finally
    {
      // leave the backend in the same state.
      try
      {
        rootContainer.close();
        rootContainer = null;

        // Sync the environment to disk.
        if (debugEnabled())
        {
          int msgID = MSGID_JEB_IMPORT_CLOSING_DATABASE;
          String message = getMessage(msgID);
          TRACER.debugInfo(message);
        }
      }
      catch (DatabaseException de)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, de);
        }
      }
    }
  }



  /**
   * Verify the integrity of the backend instance.
   * @param verifyConfig The verify configuration.
   * @param statEntry Optional entry to save stats into.
   * @throws  ConfigException  If an unrecoverable problem arises during
   *                           initialization.
   * @throws  InitializationException  If a problem occurs during initialization
   *                                   that is not related to the server
   *                                   configuration.
   * @throws DirectoryException If a Directory Server error occurs.
   */
  public void verifyBackend(VerifyConfig verifyConfig, Entry statEntry)
      throws InitializationException, ConfigException, DirectoryException
  {
    // If the backend already has the root container open, we must use the same
    // underlying root container
    boolean openRootContainer = rootContainer == null;

    try
    {
      if(openRootContainer)
      {
        EnvironmentConfig envConfig =
            ConfigurableEnvironment.parseConfigEntry(cfg);

        envConfig.setReadOnly(true);
        envConfig.setAllowCreate(false);
        envConfig.setTransactional(false);
        envConfig.setTxnNoSync(false);
        envConfig.setConfigParam("je.env.isLocking", "true");
        envConfig.setConfigParam("je.env.runCheckpointer", "true");

        initializeRootContainer(envConfig);
      }

      VerifyJob verifyJob = new VerifyJob(verifyConfig);
      verifyJob.verifyBackend(rootContainer, statEntry);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      //If a root container was opened in this method as read only, close it
      //to leave the backend in the same state.
      if (openRootContainer && rootContainer != null)
      {
        try
        {
          rootContainer.close();
          rootContainer = null;
        }
        catch (DatabaseException e)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        }
      }
    }
  }


  /**
   * Rebuild index(es) in the backend instance. Note that the server will not
   * explicitly initialize this backend before calling this method.
   * @param rebuildConfig The rebuild configuration.
   * @throws  ConfigException  If an unrecoverable problem arises during
   *                           initialization.
   * @throws  InitializationException  If a problem occurs during initialization
   *                                   that is not related to the server
   *                                   configuration.
   * @throws DirectoryException If a Directory Server error occurs.
   */
  public void rebuildBackend(RebuildConfig rebuildConfig)
      throws InitializationException, ConfigException, DirectoryException
  {
    // If the backend already has the root container open, we must use the same
    // underlying root container
    boolean openRootContainer = rootContainer == null;

    // If the rootContainer is open, the backend is initialized by something
    // else.
    // We can't do any rebuild of system indexes while others are using this
    // backend. Throw error. TODO: Need to make baseDNs disablable.
    if(!openRootContainer && rebuildConfig.includesSystemIndex())
    {
      String message = getMessage(MSGID_JEB_REBUILD_BACKEND_ONLINE);
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_REBUILD_BACKEND_ONLINE);
    }

    try
    {
      if (openRootContainer)
      {
        EnvironmentConfig envConfig =
            ConfigurableEnvironment.parseConfigEntry(cfg);

        initializeRootContainer(envConfig);
      }

      RebuildJob rebuildJob = new RebuildJob(rebuildConfig);
      rebuildJob.rebuildBackend(rootContainer);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_DATABASE_EXCEPTION,
                                  e.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, MSGID_JEB_DATABASE_EXCEPTION);
    }
    catch (JebException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   e.getMessage(),
                                   e.getMessageID());
    }
    finally
    {
      //If a root container was opened in this method as read only, close it
      //to leave the backend in the same state.
      if (openRootContainer && rootContainer != null)
      {
        try
        {
          rootContainer.close();
          rootContainer = null;
        }
        catch (DatabaseException e)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  public void createBackup(BackupConfig backupConfig)
      throws DirectoryException
  {
    BackupManager backupManager =
        new BackupManager(getBackendID());
    backupManager.createBackup(cfg, backupConfig);
  }



  /**
   * {@inheritDoc}
   */
  public void removeBackup(BackupDirectory backupDirectory, String backupID)
      throws DirectoryException
  {
    BackupManager backupManager =
        new BackupManager(getBackendID());
    backupManager.removeBackup(backupDirectory, backupID);
  }



  /**
   * {@inheritDoc}
   */
  public void restoreBackup(RestoreConfig restoreConfig)
      throws DirectoryException
  {
    BackupManager backupManager =
        new BackupManager(getBackendID());
    backupManager.restoreBackup(cfg, restoreConfig);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean isConfigurationAcceptable(Configuration configuration,
                                           List<String> unacceptableReasons)
  {
    JEBackendCfg config = (JEBackendCfg) configuration;
    return isConfigurationChangeAcceptable(config, unacceptableReasons);
  }



  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
      JEBackendCfg cfg,
      List<String> unacceptableReasons)
  {
    // Make sure that the logging level value is acceptable.
    String loggingLevel = cfg.getDatabaseLoggingLevel();
    if (! (loggingLevel.equals("OFF") ||
           loggingLevel.equals("SEVERE") ||
           loggingLevel.equals("WARNING") ||
           loggingLevel.equals("INFO") ||
           loggingLevel.equals("CONFIG") ||
           loggingLevel.equals("FINE") ||
           loggingLevel.equals("FINER") ||
           loggingLevel.equals("FINEST") ||
           loggingLevel.equals("OFF")))
    {
      int    msgID   = MSGID_JEB_INVALID_LOGGING_LEVEL;
      String message = getMessage(msgID,
                                  String.valueOf(cfg.getDatabaseLoggingLevel()),
                                  String.valueOf(cfg.dn()));
      unacceptableReasons.add(message);
      return false;
    }

    return true;
  }



  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(JEBackendCfg newCfg)
  {
    ConfigChangeResult ccr;
    ResultCode resultCode = ResultCode.SUCCESS;
    ArrayList<String> messages = new ArrayList<String>();


    try
    {
      if(rootContainer != null)
      {
        DN[] baseDNs = new DN[newCfg.getBackendBaseDN().size()];
        baseDNs = newCfg.getBackendBaseDN().toArray(baseDNs);

        // Check for changes to the base DNs.
        for (DN baseDN : cfg.getBackendBaseDN())
        {
          boolean found = false;
          for (DN dn : baseDNs)
          {
            if (dn.equals(baseDN))
            {
              found = true;
            }
          }
          if (!found)
          {
            // The base DN was deleted.
            DirectoryServer.deregisterBaseDN(baseDN, false);
            rootContainer.removeEntryContainer(baseDN);
          }
        }

        for (DN baseDN : baseDNs)
        {
          if (!rootContainer.getBaseDNs().contains(baseDN))
          {
            try
            {
              // The base DN was added.
              rootContainer.openEntryContainer(baseDN);
              DirectoryServer.registerBaseDN(baseDN, this, false, false);
            }
            catch (Exception e)
            {
              if (debugEnabled())
              {
                TRACER.debugCaught(DebugLogLevel.ERROR, e);
              }

              resultCode = DirectoryServer.getServerErrorResultCode();

              int msgID   = MSGID_BACKEND_CANNOT_REGISTER_BASEDN;
              messages.add(getMessage(msgID, String.valueOf(baseDN),
                                      String.valueOf(e)));
              ccr = new ConfigChangeResult(resultCode, false, messages);
              return ccr;
            }
          }
        }
      }
      // Put the new configuration in place.
      this.cfg = newCfg;
    }
    catch (Exception e)
    {
      messages.add(stackTraceToSingleLineString(e));
      ccr = new ConfigChangeResult(DirectoryServer.getServerErrorResultCode(),
                                   false, messages);
      return ccr;
    }

    ccr = new ConfigChangeResult(resultCode, false, messages);
    return ccr;
  }

  /**
   * Returns a handle to the JE root container currently used by this backend.
   * The rootContainer could be NULL if the backend is not initialized.
   *
   * @return The RootContainer object currently used by this backend.
   */
  public RootContainer getRootContainer()
  {
    return rootContainer;
  }

  /**
   * Clears all the entries from the backend.  This method is for test cases
   * that use the JE backend.
   *
   * @throws  ConfigException  If an unrecoverable problem arises in the
   *                           process of performing the initialization.
   *
   * @throws  JebException     If an error occurs while removing the data.
   */
  public void clearBackend()
      throws ConfigException, JebException
  {
    // Determine the backend database directory.
    File backendDirectory = getFileForPath(cfg.getBackendDirectory());
    EnvManager.removeFiles(backendDirectory.getPath());
  }

  private void initializeRootContainer(EnvironmentConfig envConfig)
      throws ConfigException, InitializationException
  {
    // Open the database environment
    try
    {
      rootContainer = new RootContainer(this, cfg);
      rootContainer.open(envConfig);
    }
    catch (DatabaseException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      String message = getMessage(MSGID_JEB_OPEN_ENV_FAIL,
                                  e.getMessage());
      throw new InitializationException(MSGID_JEB_OPEN_ENV_FAIL, message, e);
    }
  }
}
