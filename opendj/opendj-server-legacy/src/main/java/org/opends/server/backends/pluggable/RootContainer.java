/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2011-2015 ForgeRock AS
 */
package org.opends.server.backends.pluggable;

import static org.opends.messages.BackendMessages.ERR_LDIF_BACKEND_CANNOT_CREATE_LDIF_READER;
import static org.opends.messages.BackendMessages.ERR_LDIF_BACKEND_ERROR_READING_LDIF;
import static org.opends.messages.JebMessages.ERR_JEB_CACHE_PRELOAD;
import static org.opends.messages.JebMessages.ERR_JEB_REMOVE_FAIL;
import static org.opends.messages.JebMessages.ERR_JEB_ENTRY_CONTAINER_ALREADY_REGISTERED;
import static org.opends.messages.JebMessages.ERR_JEB_IMPORT_PARENT_NOT_FOUND;
import static org.opends.messages.JebMessages.NOTE_JEB_IMPORT_FINAL_STATUS;
import static org.opends.messages.JebMessages.NOTE_JEB_IMPORT_PROGRESS_REPORT;
import static org.opends.messages.JebMessages.WARN_JEB_IMPORT_ENTRY_EXISTS;
import static org.opends.messages.UtilityMessages.ERR_LDIF_SKIP;
import static org.opends.server.core.DirectoryServer.getServerErrorResultCode;
import static org.opends.server.util.StaticUtils.stackTraceToSingleLineString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.forgerock.opendj.config.server.ConfigChangeResult;
import org.forgerock.opendj.config.server.ConfigException;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.PluggableBackendCfg;
import org.opends.server.api.CompressedSchema;
import org.opends.server.backends.pluggable.spi.ReadOperation;
import org.opends.server.backends.pluggable.spi.ReadableStorage;
import org.opends.server.backends.pluggable.spi.Storage;
import org.opends.server.backends.pluggable.spi.StorageRuntimeException;
import org.opends.server.backends.pluggable.spi.WriteOperation;
import org.opends.server.backends.pluggable.spi.WriteableStorage;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.DN;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.InitializationException;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.LDIFImportResult;
import org.opends.server.types.OpenDsException;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.opends.server.util.RuntimeInformation;


/**
 * Wrapper class for the JE environment. Root container holds all the entry
 * containers for each base DN. It also maintains all the openings and closings
 * of the entry containers.
 */
public class RootContainer implements ConfigurationChangeListener<PluggableBackendCfg>
{
  /** Logs the progress of the import. */
  private static final class ImportProgress implements Runnable
  {
    private final LDIFReader reader;
    private long previousCount;
    private long previousTime;

    public ImportProgress(LDIFReader reader)
    {
      this.reader = reader;
    }

    @Override
    public void run()
    {
      long latestCount = reader.getEntriesRead() + 0;
      long deltaCount = latestCount - previousCount;
      long latestTime = System.currentTimeMillis();
      long deltaTime = latestTime - previousTime;
      if (deltaTime == 0)
      {
        return;
      }
      long entriesRead = reader.getEntriesRead();
      long entriesIgnored = reader.getEntriesIgnored();
      long entriesRejected = reader.getEntriesRejected();
      float rate = 1000f * deltaCount / deltaTime;
      logger.info(NOTE_JEB_IMPORT_PROGRESS_REPORT, entriesRead, entriesIgnored, entriesRejected, rate);

      previousCount = latestCount;
      previousTime = latestTime;
    }
  }

  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  private static final int IMPORT_PROGRESS_INTERVAL = 10000;

  /** The JE database environment. */
  private Storage storage;

  /** The backend to which this entry root container belongs. */
  private final BackendImpl backend;
  /** The backend configuration. */
  private final PluggableBackendCfg config;
  /** The database environment monitor for this JE environment. */
  private DatabaseEnvironmentMonitor monitor;

  /** The base DNs contained in this root container. */
  private final ConcurrentHashMap<DN, EntryContainer> entryContainers = new ConcurrentHashMap<DN, EntryContainer>();

  /** The cached value of the next entry identifier to be assigned. */
  private AtomicLong nextid = new AtomicLong(1);

  /** The compressed schema manager for this backend. */
  private JECompressedSchema compressedSchema;

  /**
   * Creates a new RootContainer object. Each root container represents a JE
   * environment.
   *
   * @param config
   *          The configuration of the JE backend.
   * @param backend
   *          A reference to the JE back end that is creating this root
   *          container.
   */
  RootContainer(BackendImpl backend, PluggableBackendCfg config)
  {
    this.backend = backend;
    this.config = config;

    getMonitorProvider().enableFilterUseStats(config.isIndexFilterAnalyzerEnabled());
    getMonitorProvider().setMaxEntries(config.getIndexFilterAnalyzerMaxFilters());

    config.addPluggableChangeListener(this);
  }

  /**
   * Returns the underlying storage engine.
   *
   * @return the underlying storage engine
   */
  Storage getStorage()
  {
    return storage;
  }

  /**
   * Imports information from an LDIF file into this backend. This method should
   * only be called if {@code supportsLDIFImport} returns {@code true}. <p>Note
   * that the server will not explicitly initialize this backend before calling
   * this method.
   *
   * @param importConfig
   *          The configuration to use when performing the import.
   * @return information about the result of the import processing.
   * @throws DirectoryException
   *           If a problem occurs while performing the LDIF import.
   */
  LDIFImportResult importLDIF(LDIFImportConfig importConfig) throws DirectoryException
  {
    RuntimeInformation.logInfo();
    if (Importer.mustClearBackend(importConfig, config))
    {
      try
      {
        Storage storage = backend.newStorageInstance();
        storage.initialize(config);
        storage.removeStorageFiles();
      }
      catch (Exception e)
      {
        LocalizableMessage m = ERR_JEB_REMOVE_FAIL.get(e.getMessage());
        throw new DirectoryException(DirectoryServer.getServerErrorResultCode(), m, e);
      }
    }
    try
    {
      open();

      ScheduledThreadPoolExecutor timerService = new ScheduledThreadPoolExecutor(1);
      try
      {
        final LDIFReader reader;
        try
        {
          reader = new LDIFReader(importConfig);
        }
        catch (Exception e)
        {
          LocalizableMessage m = ERR_LDIF_BACKEND_CANNOT_CREATE_LDIF_READER.get(stackTraceToSingleLineString(e));
          throw new DirectoryException(DirectoryServer.getServerErrorResultCode(), m, e);
        }

        long importCount = 0;
        final long startTime = System.currentTimeMillis();
        timerService.scheduleAtFixedRate(new ImportProgress(reader),
            IMPORT_PROGRESS_INTERVAL, IMPORT_PROGRESS_INTERVAL, TimeUnit.MILLISECONDS);
        while (true)
        {
          final Entry entry;
          try
          {
            entry = reader.readEntry();
            if (entry == null)
            {
              break;
            }
          }
          catch (LDIFException le)
          {
            if (!le.canContinueReading())
            {
              LocalizableMessage m = ERR_LDIF_BACKEND_ERROR_READING_LDIF.get(stackTraceToSingleLineString(le));
              throw new DirectoryException(DirectoryServer.getServerErrorResultCode(), m, le);
            }
            continue;
          }

          final DN dn = entry.getName();
          final EntryContainer ec = getEntryContainer(dn);
          if (ec == null)
          {
            final LocalizableMessage m = ERR_LDIF_SKIP.get(dn);
            logger.error(m);
            reader.rejectLastEntry(m);
            continue;
          }

          try
          {
            ec.addEntry(entry, null);
            importCount++;
          }
          catch (DirectoryException e)
          {
            switch (e.getResultCode().asEnum())
            {
            case ENTRY_ALREADY_EXISTS:
              if (importConfig.replaceExistingEntries())
              {
                final Entry oldEntry = ec.getEntry(entry.getName());
                ec.replaceEntry(oldEntry, entry, null);
              }
              else
              {
                reader.rejectLastEntry(WARN_JEB_IMPORT_ENTRY_EXISTS.get());
              }
              break;
            case NO_SUCH_OBJECT:
              reader.rejectLastEntry(ERR_JEB_IMPORT_PARENT_NOT_FOUND.get(dn.parent()));
              break;
            default:
              // Not sure why it failed.
              reader.rejectLastEntry(e.getMessageObject());
              break;
            }
          }
        }
        final long finishTime = System.currentTimeMillis();

        waitForShutdown(timerService);

        final long importTime = finishTime - startTime;
        float rate = 0;
        if (importTime > 0)
        {
          rate = 1000f * reader.getEntriesRead() / importTime;
        }
        logger.info(NOTE_JEB_IMPORT_FINAL_STATUS, reader.getEntriesRead(), importCount, reader.getEntriesIgnored(),
            reader.getEntriesRejected(), 0, importTime / 1000, rate);
        return new LDIFImportResult(reader.getEntriesRead(), reader.getEntriesRejected(), reader.getEntriesIgnored());
      }
      finally
      {
        close();

        // if not already stopped, then stop it
        waitForShutdown(timerService);
      }
    }
    catch (DirectoryException e)
    {
      logger.traceException(e);
      throw e;
    }
    catch (OpenDsException e)
    {
      logger.traceException(e);
      throw new DirectoryException(getServerErrorResultCode(), e.getMessageObject());
    }
    catch (Exception e)
    {
      logger.traceException(e);
      throw new DirectoryException(getServerErrorResultCode(), LocalizableMessage.raw(e.getMessage()));
    }
  }

  private void waitForShutdown(ScheduledThreadPoolExecutor timerService) throws InterruptedException
  {
    timerService.shutdown();
    timerService.awaitTermination(20, TimeUnit.SECONDS);
  }

  /**
   * Opens the root container.
   *
   * @throws StorageRuntimeException
   *           If a database error occurs when creating the environment.
   * @throws ConfigException
   *           If an configuration error occurs while creating the environment.
   */
  void open() throws StorageRuntimeException, ConfigException
  {
    try
    {
      storage = backend.newStorageInstance();
      storage.initialize(config);
      storage.open();
      storage.write(new WriteOperation()
      {
        @Override
        public void run(WriteableStorage txn) throws Exception
        {
          compressedSchema = new JECompressedSchema(storage, txn);
          openAndRegisterEntryContainers(txn, config.getBaseDN());
        }
      });
    }
    catch (Exception e)
    {
      throw new StorageRuntimeException(e);
    }
  }

  /**
   * Opens the entry container for a base DN. If the entry container does not
   * exist for the base DN, it will be created. The entry container will be
   * opened with the same mode as the root container. Any entry containers
   * opened in a read only root container will also be read only. Any entry
   * containers opened in a non transactional root container will also be non
   * transactional.
   *
   * @param baseDN
   *          The base DN of the entry container to open.
   * @param txn
   *          The database transaction
   * @return The opened entry container.
   * @throws StorageRuntimeException
   *           If an error occurs while opening the entry container.
   * @throws ConfigException
   *           If an configuration error occurs while opening the entry container.
   */
  EntryContainer openEntryContainer(DN baseDN, WriteableStorage txn)
      throws StorageRuntimeException, ConfigException
  {
    EntryContainer ec = new EntryContainer(baseDN, backend, config, storage, this);
    ec.open(txn);
    return ec;
  }

  /**
   * Registers the entry container for a base DN.
   *
   * @param baseDN
   *          The base DN of the entry container to close.
   * @param entryContainer
   *          The entry container to register for the baseDN.
   * @throws InitializationException
   *           If an error occurs while opening the entry container.
   */
  void registerEntryContainer(DN baseDN, EntryContainer entryContainer) throws InitializationException
  {
    EntryContainer ec1 = this.entryContainers.get(baseDN);

    // If an entry container for this baseDN is already open we don't allow
    // another to be opened.
    if (ec1 != null)
    {
      throw new InitializationException(ERR_JEB_ENTRY_CONTAINER_ALREADY_REGISTERED.get(ec1.getDatabasePrefix(),
          baseDN));
    }

    this.entryContainers.put(baseDN, entryContainer);
  }

  /**
   * Opens the entry containers for multiple base DNs.
   *
   * @param baseDNs
   *          The base DNs of the entry containers to open.
   * @throws StorageRuntimeException
   *           If a database error occurs while opening the entry container.
   * @throws InitializationException
   *           If an initialization error occurs while opening the entry
   *           container.
   * @throws ConfigException
   *           If a configuration error occurs while opening the entry
   *           container.
   */
  private void openAndRegisterEntryContainers(WriteableStorage txn, Set<DN> baseDNs) throws StorageRuntimeException,
      InitializationException, ConfigException
  {
    EntryID highestID = null;
    for (DN baseDN : baseDNs)
    {
      EntryContainer ec = openEntryContainer(baseDN, txn);
      EntryID id = ec.getHighestEntryID(txn);
      registerEntryContainer(baseDN, ec);
      if (highestID == null || id.compareTo(highestID) > 0)
      {
        highestID = id;
      }
    }

    nextid = new AtomicLong(highestID.longValue() + 1);
  }

  /**
   * Unregisters the entry container for a base DN.
   *
   * @param baseDN
   *          The base DN of the entry container to close.
   * @return The entry container that was unregistered or NULL if a entry
   *         container for the base DN was not registered.
   */
  EntryContainer unregisterEntryContainer(DN baseDN)
  {
    return entryContainers.remove(baseDN);
  }

  /**
   * Retrieves the compressed schema manager for this backend.
   *
   * @return The compressed schema manager for this backend.
   */
  CompressedSchema getCompressedSchema()
  {
    return compressedSchema;
  }

  /**
   * Get the DatabaseEnvironmentMonitor object for JE environment used by this
   * root container.
   *
   * @return The DatabaseEnvironmentMonitor object.
   */
  DatabaseEnvironmentMonitor getMonitorProvider()
  {
    if (monitor == null)
    {
      String monitorName = backend.getBackendID() + " Database Storage";
      monitor = new DatabaseEnvironmentMonitor(monitorName, this);
    }

    return monitor;
  }

  /**
   * Preload the database cache. There is no preload if the configured preload
   * time limit is zero.
   *
   * @param timeLimit
   *          The time limit for the preload process.
   */
  void preload(long timeLimit)
  {
    if (timeLimit > 0)
    {
      // Get a list of all the databases used by the backend.
      ArrayList<DatabaseContainer> dbList = new ArrayList<DatabaseContainer>();
      for (EntryContainer ec : entryContainers.values())
      {
        ec.sharedLock.lock();
        try
        {
          ec.listDatabases(dbList);
        }
        finally
        {
          ec.sharedLock.unlock();
        }
      }

      // Sort the list in order of priority.
      Collections.sort(dbList, new DbPreloadComparator());

      // Preload each database until we reach the time limit or the cache
      // is filled.
      try
      {
        throw new UnsupportedOperationException("Not implemented exception");
      }
      catch (StorageRuntimeException e)
      {
        logger.traceException(e);

        logger.error(ERR_JEB_CACHE_PRELOAD, backend.getBackendID(),
            stackTraceToSingleLineString(e.getCause() != null ? e.getCause() : e));
      }
    }
  }

  /**
   * Closes this root container.
   *
   * @throws StorageRuntimeException
   *           If an error occurs while attempting to close the root container.
   */
  void close() throws StorageRuntimeException
  {
    for (DN baseDN : entryContainers.keySet())
    {
      EntryContainer ec = unregisterEntryContainer(baseDN);
      ec.exclusiveLock.lock();
      try
      {
        ec.close();
      }
      finally
      {
        ec.exclusiveLock.unlock();
      }
    }

    compressedSchema.close();
    config.removePluggableChangeListener(this);

    if (storage != null)
    {
      storage.close();
      storage = null;
    }
  }

  /**
   * Return all the entry containers in this root container.
   *
   * @return The entry containers in this root container.
   */
  public Collection<EntryContainer> getEntryContainers()
  {
    return entryContainers.values();
  }

  /**
   * Returns all the baseDNs this root container stores.
   *
   * @return The set of DNs this root container stores.
   */
  Set<DN> getBaseDNs()
  {
    return entryContainers.keySet();
  }

  /**
   * Return the entry container for a specific base DN.
   *
   * @param baseDN
   *          The base DN of the entry container to retrieve.
   * @return The entry container for the base DN.
   */
  EntryContainer getEntryContainer(DN baseDN)
  {
    EntryContainer ec = null;
    DN nodeDN = baseDN;

    while (ec == null && nodeDN != null)
    {
      ec = entryContainers.get(nodeDN);
      if (ec == null)
      {
        nodeDN = nodeDN.getParentDNInSuffix();
      }
    }

    return ec;
  }

  /**
   * Get the backend configuration used by this root container.
   *
   * @return The backend configuration used by this root container.
   */
  PluggableBackendCfg getConfiguration()
  {
    return config;
  }

  /**
   * Get the total number of entries in this root container.
   *
   * @return The number of entries in this root container
   * @throws StorageRuntimeException
   *           If an error occurs while retrieving the entry count.
   */
  long getEntryCount() throws StorageRuntimeException
  {
    try
    {
      return storage.read(new ReadOperation<Long>()
      {
        @Override
        public Long run(ReadableStorage txn) throws Exception
        {
          long entryCount = 0;
          for (EntryContainer ec : entryContainers.values())
          {
            ec.sharedLock.lock();
            try
            {
              entryCount += ec.getEntryCount(txn);
            }
            finally
            {
              ec.sharedLock.unlock();
            }
          }
          return entryCount;
        }
      });
    }
    catch (Exception e)
    {
      throw new StorageRuntimeException(e);
    }
  }

  /**
   * Assign the next entry ID.
   *
   * @return The assigned entry ID.
   */
  EntryID getNextEntryID()
  {
    return new EntryID(nextid.getAndIncrement());
  }

  /**
   * Resets the next entry ID counter to zero. This should only be used after
   * clearing all databases.
   */
  public void resetNextEntryID()
  {
    nextid.set(1);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConfigurationChangeAcceptable(PluggableBackendCfg configuration,
      List<LocalizableMessage> unacceptableReasons)
  {
    // Storage has also registered a change listener, delegate to it.
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public ConfigChangeResult applyConfigurationChange(PluggableBackendCfg configuration)
  {
    getMonitorProvider().enableFilterUseStats(configuration.isIndexFilterAnalyzerEnabled());
    getMonitorProvider().setMaxEntries(configuration.getIndexFilterAnalyzerMaxFilters());

    return new ConfigChangeResult();
  }

  /**
   * Returns whether this container JE database environment is open, valid and
   * can be used.
   *
   * @return {@code true} if valid, or {@code false} otherwise.
   */
  public boolean isValid() {
    return storage.isValid();
  }
}
