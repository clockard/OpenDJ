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
package org.opends.server.extensions;
import org.opends.messages.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.config.ConfigParam;
import com.sleepycat.je.config.EnvironmentParams;
import org.opends.messages.MessageBuilder;
import org.opends.server.api.Backend;
import org.opends.server.api.EntryCache;
import org.opends.server.admin.std.server.EntryCacheCfg;
import org.opends.server.admin.std.server.FileSystemEntryCacheCfg;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.backends.jeb.ConfigurableEnvironment;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.EntryEncodeConfig;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchFilter;
import org.opends.server.types.FilePermission;
import org.opends.server.types.DebugLogLevel;
import org.opends.server.types.OpenDsException;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.util.ServerConstants;

import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.config.ConfigConstants.*;
import static org.opends.messages.ExtensionMessages.*;
import static org.opends.server.util.StaticUtils.*;
import static org.opends.messages.ConfigMessages.*;

/**
 * This class defines a Directory Server entry cache that uses JE database to
 * keep track of the entries. Intended use is when JE database resides in the
 * memory based file system which has obvious performance benefits, although
 * any file system will do for this cache to function. Entries are maintained
 * either by FIFO (default) or LRU (configurable) based list implementation.
 * <BR><BR>
 * Cache sizing is based on the size or percentage of free space availble in
 * the file system, such that if enough memory is free, then adding an entry
 * to the cache will not require purging, but if more than a specified
 * percentage of the file system available space is already consumed, then
 * one or more entries will need to be removed in order to make room for a
 * new entry.  It is also possible to configure a maximum number of entries
 * for the cache. If this is specified, then the number of entries will not
 * be allowed to exceed this value, but it may not be possible to hold this
 * many entries if the available memory fills up first.
 * <BR><BR>
 * Other configurable parameters for this cache include the maximum length of
 * time to block while waiting to acquire a lock, and a set of filters that may
 * be used to define criteria for determining which entries are stored in the
 * cache.  If a filter list is provided, then only entries matching at least
 * one of the given filters will be stored in the cache.
 * <BR><BR>
 * JE environment cache size can also be configured either as percentage of
 * the free memory available in the JVM or as explicit size in bytes.
 * <BR><BR>
 * This cache has a persistence property which, if enabled, allows for the
 * contents of the cache to stay persistent across server or cache restarts.
 */
public class FileSystemEntryCache
        extends EntryCache <FileSystemEntryCacheCfg>
        implements ConfigurationChangeListener <FileSystemEntryCacheCfg> {
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();

  // Permissions for cache db environment.
  private static final FilePermission CACHE_HOME_PERMISSIONS =
      new FilePermission(0700);

  // The DN of the configuration entry for this entry cache.
  private DN configEntryDN;

  // The maximum amount of space in bytes that can be consumed in the filesystem
  // before we need to start purging entries.
  private long maxAllowedMemory;

  // The maximum number of entries that may be held in the cache.
  // Atomic for additional safely and in case we decide to push
  // some locks further down later. Does not inhere in additional
  // overhead, via blocking on synchronization primitive, on most
  // modern platforms being implemented via cpu instruction set.
  private AtomicLong maxEntries;

  // The maximum percentage of memory dedicated to JE cache.
  private int jeCachePercent;

  // The maximum amount of memory in bytes dedicated to JE cache.
  private long jeCacheSize;

  // The entry cache home folder to host db environment.
  private String cacheHome;

  // The type of this cache.
  // It can be either FIFO (default) or LRU (configurable).
  private String cacheType;

  // This regulates whether we persist the cache across restarts or not.
  private boolean persistentCache;

  // The lock used to provide threadsafe access when changing the contents
  // of the cache maps.
  private ReentrantReadWriteLock cacheLock;
  private Lock cacheReadLock;
  private Lock cacheWriteLock;

  // The mapping between DNs and IDs. This is the main index map for this
  // cache, keyed to the underlying JE database where entries are stored.
  private Map<DN,Long> dnMap;

  // The mapping between entry backends/IDs and DNs to identify all
  // entries that belong to given backend since entry ID is only
  // per backend unique.
  private Map<Backend,Map<Long,DN>> backendMap;

  // Access order for this cache. FIFO by default.
  boolean accessOrder = false;

  // JE environment and database related fields for this cache.
  private Environment entryCacheEnv;
  private EnvironmentConfig entryCacheEnvConfig;
  private EnvironmentMutableConfig entryCacheEnvMutableConfig;
  private DatabaseConfig entryCacheDBConfig;

  // Statistics retrieval operation config for this JE environment.
  private StatsConfig entryCacheEnvStatsConfig = new StatsConfig();

  // The main entry cache database.
  private Database entryCacheDB;

  // Class database, catalog and binding for serialization.
  private Database entryCacheClassDB;
  private StoredClassCatalog classCatalog;
  private EntryBinding entryCacheDataBinding;

  // JE naming constants.
  private static final String ENTRYCACHEDBNAME = "EntryCacheDB";
  private static final String INDEXCLASSDBNAME = "IndexClassDB";
  private static final String INDEXKEY = "EntryCacheIndex";

  // The number of milliseconds between persistent state save/restore
  // progress reports.
  private long progressInterval = 5000;

  // Persistent state save/restore progress report counters.
  private long persistentEntriesSaved    = 0;
  private long persistentEntriesRestored = 0;

  // The configuration to use when encoding entries in the database.
  private EntryEncodeConfig encodeConfig =
    new EntryEncodeConfig(true, false, false);

  // JE native properties to configuration attributes map.
  private HashMap<String, String> configAttrMap =
    new HashMap<String, String>();

  // Currently registered configuration object.
  private FileSystemEntryCacheCfg registeredConfiguration;

  /**
   * Creates a new instance of this entry cache.
   */
  public FileSystemEntryCache() {
    super();

    // Register all JE native properties that map to
    // corresponding config attributes.
    configAttrMap.put("je.maxMemoryPercent",
      ConfigurableEnvironment.ATTR_DATABASE_CACHE_PERCENT);
    configAttrMap.put("je.maxMemory",
      ConfigurableEnvironment.ATTR_DATABASE_CACHE_SIZE);

    // All initialization should be performed in the initializeEntryCache.
  }

  /**
   * {@inheritDoc}
   */
  public void initializeEntryCache(FileSystemEntryCacheCfg configuration)
          throws ConfigException, InitializationException {

    registeredConfiguration = configuration;
    configuration.addFileSystemChangeListener (this);
    configEntryDN = configuration.dn();

    // Read and apply configuration.
    boolean applyChanges = true;
    ArrayList<Message> errorMessages = new ArrayList<Message>();
    EntryCacheCommon.ConfigErrorHandler errorHandler =
      EntryCacheCommon.getConfigErrorHandler (
          EntryCacheCommon.ConfigPhase.PHASE_INIT, null, errorMessages
          );
    if (!processEntryCacheConfig(configuration, applyChanges, errorHandler)) {
      MessageBuilder buffer = new MessageBuilder();
      if (!errorMessages.isEmpty()) {
        Iterator<Message> iterator = errorMessages.iterator();
        buffer.append(iterator.next());
        while (iterator.hasNext()) {
          buffer.append(".  ");
          buffer.append(iterator.next());
        }
      }
      Message message = ERR_FSCACHE_CANNOT_INITIALIZE.get(buffer.toString());
      throw new ConfigException(message);
    }

    // Set the cache type.
    if (cacheType.equalsIgnoreCase("LRU")) {
      accessOrder = true;
    } else {
      // Admin framework should only allow for either FIFO or LRU but
      // we set the type to default here explicitly if it is not LRU.
      cacheType = DEFAULT_FSCACHE_TYPE;
      accessOrder = false;
    }

    // Initialize the cache maps and locks.
    backendMap = new LinkedHashMap<Backend,Map<Long,DN>>();
    dnMap = new LinkedHashMapRotator<DN,Long>(16, (float) 0.75,
        accessOrder);

    cacheLock = new ReentrantReadWriteLock();
    if (accessOrder) {
      // In access-ordered linked hash maps, merely querying the map
      // with get() is a structural modification.
      cacheReadLock = cacheLock.writeLock();
    } else {
      cacheReadLock = cacheLock.readLock();
    }
    cacheWriteLock = cacheLock.writeLock();

    // Setup the cache home.
    try {
      checkAndSetupCacheHome(cacheHome);
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      // Not having any home directory for the cache db environment is a
      // fatal error as we are unable to continue any further without it.
      Message message =
          ERR_FSCACHE_HOMELESS.get();
      throw new InitializationException(message, e);
    }

    // Configure and open JE environment and cache database.
    try {
      entryCacheEnvConfig.setAllowCreate(true);
      entryCacheEnv = new Environment(new File(cacheHome), entryCacheEnvConfig);
      entryCacheEnv.setMutableConfig(entryCacheEnvMutableConfig);
      entryCacheDBConfig = new DatabaseConfig();
      entryCacheDBConfig.setAllowCreate(true);

      // Configure the JE environment statistics to return only
      // the values which do not incur some performance penalty.
      entryCacheEnvStatsConfig.setFast(true);

      // Remove old cache databases if this cache is not persistent.
      if ( !persistentCache ) {
        try {
          entryCacheEnv.removeDatabase(null, INDEXCLASSDBNAME);
        } catch (DatabaseNotFoundException e) {}
        try {
          entryCacheEnv.removeDatabase(null, ENTRYCACHEDBNAME);
        } catch (DatabaseNotFoundException e) {}
      }

      entryCacheDB = entryCacheEnv.openDatabase(null,
              ENTRYCACHEDBNAME, entryCacheDBConfig);
      entryCacheClassDB =
        entryCacheEnv.openDatabase(null, INDEXCLASSDBNAME, entryCacheDBConfig);
      // Instantiate the class catalog
      classCatalog = new StoredClassCatalog(entryCacheClassDB);
      entryCacheDataBinding =
          new SerialBinding(classCatalog, FileSystemEntryCacheIndex.class);

      // Restoration is static and not subject to the current configuration
      // constraints so that the persistent state is truly preserved and
      // restored to the exact same state where we left off when the cache
      // has been made persistent. The only exception to this is the backend
      // offline state matching where entries that belong to backend which
      // we cannot match offline state for are discarded from the cache.
      if ( persistentCache ) {
        // Retrieve cache index.
        try {
          FileSystemEntryCacheIndex entryCacheIndex;
          DatabaseEntry indexData = new DatabaseEntry();
          DatabaseEntry indexKey = new DatabaseEntry(
              INDEXKEY.getBytes("UTF-8"));

          if (OperationStatus.SUCCESS ==
              entryCacheDB.get(null, indexKey, indexData, LockMode.DEFAULT)) {
            entryCacheIndex =
                (FileSystemEntryCacheIndex)
                entryCacheDataBinding.entryToObject(indexData);
          } else {
            throw new CacheIndexNotFoundException();
          }
          // Check cache index state.
          if ((entryCacheIndex.dnMap.isEmpty()) ||
              (entryCacheIndex.backendMap.isEmpty()) ||
              (entryCacheIndex.offlineState.isEmpty())) {
            throw new CacheIndexImpairedException();
          } else {
            // Restore entry cache maps from this index.

            // Push maxEntries and make it unlimited til restoration complete.
            AtomicLong currentMaxEntries = maxEntries;
            maxEntries.set(DEFAULT_FSCACHE_MAX_ENTRIES);

            // Convert cache index maps to entry cache maps.
            Set<String> backendSet = entryCacheIndex.backendMap.keySet();
            Iterator<String> backendIterator = backendSet.iterator();

            // Start a timer for the progress report.
            final long persistentEntriesTotal = entryCacheIndex.dnMap.size();
            Timer timer = new Timer();
            TimerTask progressTask = new TimerTask() {
              // Persistent state restore progress report.
              public void run() {
                if ((persistentEntriesRestored > 0) &&
                    (persistentEntriesRestored < persistentEntriesTotal)) {
                  Message message = NOTE_FSCACHE_RESTORE_PROGRESS_REPORT.get(
                      persistentEntriesRestored, persistentEntriesTotal);
                  logError(message);
                }
              }
            };
            timer.scheduleAtFixedRate(progressTask, progressInterval,
                                      progressInterval);
            try {
              while (backendIterator.hasNext()) {
                String backend = backendIterator.next();
                Map<Long,String> entriesMap =
                    entryCacheIndex.backendMap.get(backend);
                Set<Long> entriesSet = entriesMap.keySet();
                Iterator<Long> entriesIterator = entriesSet.iterator();
                LinkedHashMap<Long,DN> entryMap = new LinkedHashMap<Long,DN>();
                while (entriesIterator.hasNext()) {
                  Long entryID = entriesIterator.next();
                  String entryStringDN = entriesMap.get(entryID);
                  DN entryDN = DN.decode(entryStringDN);
                  dnMap.put(entryDN, entryID);
                  entryMap.put(entryID, entryDN);
                  persistentEntriesRestored++;
                }
                backendMap.put(DirectoryServer.getBackend(backend), entryMap);
              }
            } finally {
              // Stop persistent state restore progress report timer.
              timer.cancel();

              // Final persistent state restore progress report.
              Message message = NOTE_FSCACHE_RESTORE_PROGRESS_REPORT.get(
                  persistentEntriesRestored, persistentEntriesTotal);
              logError(message);
            }

            // Compare last known offline states to offline states on startup.
            Map<String,Long> currentBackendsState =
                DirectoryServer.getOfflineBackendsStateIDs();
            Set<String> offlineBackendSet =
                entryCacheIndex.offlineState.keySet();
            Iterator<String> offlineBackendIterator =
                offlineBackendSet.iterator();
            while (offlineBackendIterator.hasNext()) {
              String backend = offlineBackendIterator.next();
              Long offlineId = entryCacheIndex.offlineState.get(backend);
              Long currentId = currentBackendsState.get(backend);
              if ( !(offlineId.equals(currentId)) ) {
                // Remove cache entries specific to this backend.
                clearBackend(DirectoryServer.getBackend(backend));
                // Log an error message.
                logError(WARN_FSCACHE_OFFLINE_STATE_FAIL.get(backend));
              }
            }
            // Pop max entries limit.
            maxEntries = currentMaxEntries;
          }
        } catch (CacheIndexNotFoundException e) {
          if (debugEnabled()) {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }

          // Log an error message.
          logError(NOTE_FSCACHE_INDEX_NOT_FOUND.get());

          // Clear the entry cache.
          clear();
        } catch (CacheIndexImpairedException e) {
          if (debugEnabled()) {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }

          // Log an error message.
          logError(ERR_FSCACHE_INDEX_IMPAIRED.get());

          // Clear the entry cache.
          clear();
        } catch (Exception e) {
          if (debugEnabled()) {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }

          // Log an error message.
          logError(ERR_FSCACHE_CANNOT_LOAD_PERSISTENT_DATA.get());

          // Clear the entry cache.
          clear();
        }
      }
    } catch (Exception e) {
      // If we got here it means we have failed to have a proper backend
      // for this entry cache and there is absolutely no point going any
      // farther from here.
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message =
          ERR_FSCACHE_CANNOT_INITIALIZE.get(
          (e.getCause() != null ? e.getCause().getMessage() :
            stackTraceToSingleLineString(e)));
      throw new InitializationException(message, e);
    }

  }

  /**
   * {@inheritDoc}
   */
  public void finalizeEntryCache() {

    cacheWriteLock.lock();

    registeredConfiguration.removeFileSystemChangeListener (this);

    // Store index/maps in case of persistent cache. Since the cache database
    // already exist at this point all we have to do is to serialize cache
    // index maps @see FileSystemEntryCacheIndex and put them under indexkey
    // allowing for the index to be restored and cache contents reused upon
    // the next initialization. If this cache is empty skip persisting phase.
    if (persistentCache && !dnMap.isEmpty()) {
      FileSystemEntryCacheIndex entryCacheIndex =
          new FileSystemEntryCacheIndex();
      // There must be at least one backend at this stage.
      entryCacheIndex.offlineState =
          DirectoryServer.getOfflineBackendsStateIDs();

      // Convert entry cache maps to serializable maps for the cache index.
      Set<Backend> backendSet = backendMap.keySet();
      Iterator<Backend> backendIterator = backendSet.iterator();

      // Start a timer for the progress report.
      final long persistentEntriesTotal = dnMap.size();
      Timer timer = new Timer();
      TimerTask progressTask = new TimerTask() {
        // Persistent state save progress report.
        public void run() {
          if ((persistentEntriesSaved > 0) &&
              (persistentEntriesSaved < persistentEntriesTotal)) {
            Message message = NOTE_FSCACHE_SAVE_PROGRESS_REPORT.get(
                persistentEntriesSaved, persistentEntriesTotal);
            logError(message);
          }
        }
      };
      timer.scheduleAtFixedRate(progressTask, progressInterval,
          progressInterval);

      try {
        while (backendIterator.hasNext()) {
          Backend backend = backendIterator.next();
          Map<Long,DN> entriesMap = backendMap.get(backend);
          Map<Long,String> entryMap = new LinkedHashMap<Long,String>();
          for (Long entryID : entriesMap.keySet()) {
            DN entryDN = entriesMap.get(entryID);
            entryCacheIndex.dnMap.put(entryDN.toNormalizedString(), entryID);
            entryMap.put(entryID, entryDN.toNormalizedString());
            persistentEntriesSaved++;
          }
          entryCacheIndex.backendMap.put(backend.getBackendID(), entryMap);
        }
      } finally {
        // Stop persistent state save progress report timer.
        timer.cancel();

        // Final persistent state save progress report.
        Message message = NOTE_FSCACHE_SAVE_PROGRESS_REPORT.get(
            persistentEntriesSaved, persistentEntriesTotal);
        logError(message);
      }

      // Store the index.
      try {
        DatabaseEntry indexData = new DatabaseEntry();
        entryCacheDataBinding.objectToEntry(entryCacheIndex, indexData);
        DatabaseEntry indexKey = new DatabaseEntry(INDEXKEY.getBytes("UTF-8"));
        if (OperationStatus.SUCCESS !=
            entryCacheDB.put(null, indexKey, indexData)) {
          throw new Exception();
        }
      } catch (Exception e) {
        if (debugEnabled()) {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }

        // Log an error message.
        logError(ERR_FSCACHE_CANNOT_STORE_PERSISTENT_DATA.get());
      }
    }

    // Close JE databases and environment and clear all the maps.
    try {
      backendMap.clear();
      dnMap.clear();
      if (entryCacheDB != null) {
        entryCacheDB.close();
      }
      if (entryCacheClassDB != null) {
        entryCacheClassDB.close();
      }
      if (entryCacheEnv != null) {
        // Remove cache and index dbs if this cache is not persistent.
        if ( !persistentCache ) {
          try {
            entryCacheEnv.removeDatabase(null, INDEXCLASSDBNAME);
          } catch (DatabaseNotFoundException e) {}
          try {
            entryCacheEnv.removeDatabase(null, ENTRYCACHEDBNAME);
          } catch (DatabaseNotFoundException e) {}
        }
        entryCacheEnv.cleanLog();
        entryCacheEnv.close();
      }
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      // That is ok, JE verification and repair on startup should take care of
      // this so if there are any unrecoverable errors during next startup
      // and we are unable to handle and cleanup them we will log errors then.
    } finally {
      cacheWriteLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsEntry(DN entryDN)
  {
    // Indicate whether the DN map contains the specified DN.
    boolean containsEntry = false;
    cacheReadLock.lock();
    try {
      containsEntry = dnMap.containsKey(entryDN);
    } finally {
      cacheReadLock.unlock();
    }
    return containsEntry;
  }

  /**
   * {@inheritDoc}
   */
  public Entry getEntry(DN entryDN) {
    // Get the entry from the DN map if it is present.  If not, then return
    // null.
    Entry entry = null;
    cacheReadLock.lock();
    try {
      // Use get to generate entry access.
      if (dnMap.get(entryDN) != null) {
        entry = getEntryFromDB(entryDN);
      }
    } finally {
      cacheReadLock.unlock();
    }
    return entry;
  }

  /**
   * {@inheritDoc}
   */
  public long getEntryID(DN entryDN) {
    long entryID = -1;
    cacheReadLock.lock();
    try {
      Long eid = dnMap.get(entryDN);
      if (eid != null) {
        entryID = eid.longValue();
      }
    } finally {
      cacheReadLock.unlock();
    }
    return entryID;
  }

  /**
   * {@inheritDoc}
   */
  protected DN getEntryDN(Backend backend, long entryID) {

    DN entryDN = null;
    cacheReadLock.lock();
    try {
      // Get the map for the provided backend.  If it isn't present, then
      // return null.
      Map map = backendMap.get(backend);
      if ( !(map == null) ) {
        // Get the entry DN from the map by its ID.  If it isn't present,
        // then return null.
        entryDN = (DN) map.get(entryID);
      }
    } finally {
      cacheReadLock.unlock();
    }
    return entryDN;
  }

  /**
   * {@inheritDoc}
   */
  public void putEntry(Entry entry, Backend backend, long entryID) {

    // Check exclude and include filters first.
    if (!filtersAllowCaching(entry)) {
      return;
    }

    // Obtain a lock on the cache.  If this fails, then don't do anything.
    try {
      if (!cacheWriteLock.tryLock(getLockTimeout(), TimeUnit.MILLISECONDS)) {
        return;
      }
      putEntryToDB(entry, backend, entryID);
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      return;
    } finally {
      if (cacheLock.isWriteLockedByCurrentThread()) {
        cacheWriteLock.unlock();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean putEntryIfAbsent(Entry entry, Backend backend, long entryID)
  {
    // Check exclude and include filters first.
    if (!filtersAllowCaching(entry)) {
      return true;
    }

    try {
      // Obtain a lock on the cache.  If this fails, then don't do anything.
      if (! cacheWriteLock.tryLock(getLockTimeout(), TimeUnit.MILLISECONDS)) {
        // We can't rule out the possibility of a conflict, so return false.
        return false;
      }
      // See if the entry already exists in the cache.  If it does, then we will
      // fail and not actually store the entry.
      if (dnMap.containsKey(entry.getDN())) {
        return false;
      }
      return putEntryToDB(entry, backend, entryID);
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      // We can't rule out the possibility of a conflict, so return false.
      return false;
    } finally {
      if (cacheLock.isWriteLockedByCurrentThread()) {
        cacheWriteLock.unlock();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeEntry(DN entryDN) {

    cacheWriteLock.lock();

    try {
      Long entryID = dnMap.get(entryDN);
      if (entryID == null) {
        return;
      }
      Set<Backend> backendSet = backendMap.keySet();
      Iterator<Backend> backendIterator = backendSet.iterator();
      while (backendIterator.hasNext()) {
        Map<Long,DN> map = backendMap.get(backendIterator.next());
        if ((map.get(entryID) != null) &&
            (map.get(entryID).equals(entryDN))) {
          map.remove(entryID);
          // If this backend becomes empty now
          // remove it from the backend map.
          if (map.isEmpty()) {
            backendIterator.remove();
          }
          break;
        }
      }
      dnMap.remove(entryDN);
      entryCacheDB.delete(null,
        new DatabaseEntry(entryDN.toNormalizedString().getBytes("UTF-8")));
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
    } finally {
      cacheWriteLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clear() {
    cacheWriteLock.lock();

    dnMap.clear();
    backendMap.clear();

    try {
      if ((entryCacheDB != null) && (entryCacheEnv != null) &&
          (entryCacheClassDB != null) && (entryCacheDBConfig != null)) {
        entryCacheDBConfig = entryCacheDB.getConfig();
        entryCacheDB.close();
        entryCacheClassDB.close();
        entryCacheEnv.truncateDatabase(null, ENTRYCACHEDBNAME, false);
        entryCacheEnv.truncateDatabase(null, INDEXCLASSDBNAME, false);
        entryCacheEnv.cleanLog();
        entryCacheDB = entryCacheEnv.openDatabase(null,
            ENTRYCACHEDBNAME, entryCacheDBConfig);
        entryCacheClassDB = entryCacheEnv.openDatabase(null,
            INDEXCLASSDBNAME, entryCacheDBConfig);
        // Instantiate the class catalog
        classCatalog = new StoredClassCatalog(entryCacheClassDB);
        entryCacheDataBinding =
            new SerialBinding(classCatalog, FileSystemEntryCacheIndex.class);
      }
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
    } finally {
      cacheWriteLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clearBackend(Backend backend) {

    cacheWriteLock.lock();

    Map<Long,DN> backendEntriesMap = backendMap.get(backend);

    try {
      if (backendEntriesMap == null) {
        // No entries were in the cache for this backend,
        // so we can return without doing anything.
        return;
      }
      int entriesExamined = 0;
      Iterator<Long> backendEntriesIterator =
        backendEntriesMap.keySet().iterator();
      while (backendEntriesIterator.hasNext()) {
        Long entryID = backendEntriesIterator.next();
        DN entryDN = backendEntriesMap.get(entryID);
        entryCacheDB.delete(null,
            new DatabaseEntry(entryDN.toNormalizedString().getBytes("UTF-8")));
        backendEntriesIterator.remove();
        dnMap.remove(entryDN);

        // This can take a while, so we'll periodically release and re-acquire
        // the lock in case anyone else is waiting on it so this doesn't become
        // a stop-the-world event as far as the cache is concerned.
        entriesExamined++;
        if ((entriesExamined % 1000) == 0) {
          cacheWriteLock.unlock();
          Thread.currentThread().yield();
          cacheWriteLock.lock();
        }
      }

      // This backend is empty now, remove it from the backend map.
      backendMap.remove(backend);
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
    } finally {
      cacheWriteLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clearSubtree(DN baseDN) {
    // Determine which backend should be used for the provided base DN.  If
    // there is none, then we don't need to do anything.
    Backend backend = DirectoryServer.getBackend(baseDN);
    if (backend == null)
    {
      return;
    }

    // Acquire a lock on the cache.  We should not return until the cache has
    // been cleared, so we will block until we can obtain the lock.
    cacheWriteLock.lock();

    // At this point, it is absolutely critical that we always release the lock
    // before leaving this method, so do so in a finally block.
    try
    {
      clearSubtree(baseDN, backend);
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
      // This shouldn't happen, but there's not much that we can do if it does.
    }
    finally
    {
      cacheWriteLock.unlock();
    }
  }

  /**
   * Clears all entries at or below the specified base DN that are associated
   * with the given backend.  The caller must already hold the cache lock.
   *
   * @param  baseDN   The base DN below which all entries should be flushed.
   * @param  backend  The backend for which to remove the appropriate entries.
   */
  private void clearSubtree(DN baseDN, Backend backend) {
    // See if there are any entries for the provided backend in the cache.  If
    // not, then return.
    Map<Long,DN> map = backendMap.get(backend);
    if (map == null)
    {
      // No entries were in the cache for this backend, so we can return without
      // doing anything.
      return;
    }

    // Since the provided base DN could hold a subset of the information in the
    // specified backend, we will have to do this by iterating through all the
    // entries for that backend.  Since this could take a while, we'll
    // periodically release and re-acquire the lock in case anyone else is
    // waiting on it so this doesn't become a stop-the-world event as far as the
    // cache is concerned.
    int entriesExamined = 0;
    Iterator<DN> iterator = map.values().iterator();
    while (iterator.hasNext())
    {
      DN entryDN = iterator.next();
      if (entryDN.isDescendantOf(baseDN))
      {
        iterator.remove();
        dnMap.remove(entryDN);
        try {
          entryCacheDB.delete(null,
              new DatabaseEntry(
              entryDN.toNormalizedString().getBytes("UTF-8")));
        } catch (Exception e) {
          if (debugEnabled()) {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        }
      }

      entriesExamined++;
      if ((entriesExamined % 1000) == 0)
      {
        cacheWriteLock.unlock();
        Thread.currentThread().yield();
        cacheWriteLock.lock();
      }
    }

    // If this backend becomes empty now
    // remove it from the backend map.
    if (map.isEmpty()) {
      backendMap.remove(backend);
    }

    // See if the backend has any subordinate backends.  If so, then process
    // them recursively.
    for (Backend subBackend : backend.getSubordinateBackends())
    {
      boolean isAppropriate = false;
      for (DN subBase : subBackend.getBaseDNs())
      {
        if (subBase.isDescendantOf(baseDN))
        {
          isAppropriate = true;
          break;
        }
      }

      if (isAppropriate)
      {
        clearSubtree(baseDN, subBackend);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void handleLowMemory() {
    // This is about all we can do.
    if (entryCacheEnv != null) {
      try {
        // Free some JVM memory.
        entryCacheEnv.evictMemory();
        // Free some main memory/space.
        entryCacheEnv.cleanLog();
      } catch (Exception e) {
        if (debugEnabled()) {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean isConfigurationAcceptable(EntryCacheCfg configuration,
                                           List<Message> unacceptableReasons)
  {
    FileSystemEntryCacheCfg config = (FileSystemEntryCacheCfg) configuration;
    return isConfigurationChangeAcceptable(config, unacceptableReasons);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
      FileSystemEntryCacheCfg configuration,
      List<Message> unacceptableReasons
      )
  {
    boolean applyChanges = false;
    EntryCacheCommon.ConfigErrorHandler errorHandler =
      EntryCacheCommon.getConfigErrorHandler (
          EntryCacheCommon.ConfigPhase.PHASE_ACCEPTABLE,
          unacceptableReasons,
          null
        );
    processEntryCacheConfig (configuration, applyChanges, errorHandler);

    return errorHandler.getIsAcceptable();
  }

  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(
      FileSystemEntryCacheCfg configuration
      )
  {
    boolean applyChanges = true;
    ArrayList<Message> errorMessages = new ArrayList<Message>();
    EntryCacheCommon.ConfigErrorHandler errorHandler =
      EntryCacheCommon.getConfigErrorHandler (
          EntryCacheCommon.ConfigPhase.PHASE_APPLY, null, errorMessages
          );

    // Do not apply changes unless this cache is enabled.
    if (configuration.isEnabled()) {
      processEntryCacheConfig (configuration, applyChanges, errorHandler);
    }

    boolean adminActionRequired = errorHandler.getIsAdminActionRequired();
    ConfigChangeResult changeResult = new ConfigChangeResult(
        errorHandler.getResultCode(),
        adminActionRequired,
        errorHandler.getErrorMessages()
        );

    return changeResult;
  }

  /**
   * Makes a best-effort attempt to apply the configuration contained in the
   * provided entry.  Information about the result of this processing should be
   * added to the provided message list.  Information should always be added to
   * this list if a configuration change could not be applied.  If detailed
   * results are requested, then information about the changes applied
   * successfully (and optionally about parameters that were not changed) should
   * also be included.
   *
   * @param  configuration    The entry containing the new configuration to
   *                          apply for this component.
   * @param  detailedResults  Indicates whether detailed information about the
   *                          processing should be added to the list.
   *
   * @return  Information about the result of the configuration update.
   */
  public ConfigChangeResult applyNewConfiguration(
      FileSystemEntryCacheCfg configuration,
      boolean           detailedResults
      )
  {
    // Store the current value to detect changes.
    long                  prevLockTimeout      = getLockTimeout();
    long                  prevMaxEntries       = maxEntries.longValue();
    Set<SearchFilter>     prevIncludeFilters   = getIncludeFilters();
    Set<SearchFilter>     prevExcludeFilters   = getExcludeFilters();
    long                  prevMaxAllowedMemory = maxAllowedMemory;
    int                   prevJECachePercent   = jeCachePercent;
    long                  prevJECacheSize      = jeCacheSize;
    boolean               prevPersistentCache  = persistentCache;

    // Activate the new configuration.
    ConfigChangeResult changeResult = applyConfigurationChange(configuration);

    // Add detailed messages if needed.
    ResultCode resultCode = changeResult.getResultCode();
    boolean configIsAcceptable = (resultCode == ResultCode.SUCCESS);
    if (detailedResults && configIsAcceptable)
    {
      if (maxEntries.longValue() != prevMaxEntries)
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_MAX_ENTRIES.get(maxEntries));
      }

      if (getLockTimeout() != prevLockTimeout)
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_LOCK_TIMEOUT.get(getLockTimeout()));
      }

      if (!getIncludeFilters().equals(prevIncludeFilters))
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_INCLUDE_FILTERS.get());
      }

      if (!getExcludeFilters().equals(prevExcludeFilters))
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_EXCLUDE_FILTERS.get());
      }

      if (maxAllowedMemory != prevMaxAllowedMemory)
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_MAX_MEMORY_SIZE.get(maxAllowedMemory));
      }

      if (jeCachePercent != prevJECachePercent)
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_JE_MEMORY_PCT.get(jeCachePercent));
      }

      if (jeCacheSize != prevJECacheSize)
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_JE_MEMORY_SIZE.get(jeCacheSize));
      }

      if (persistentCache != prevPersistentCache)
      {
        changeResult.addMessage(
            INFO_FSCACHE_UPDATED_IS_PERSISTENT.get(
                    String.valueOf(persistentCache)));
      }
    }

    return changeResult;
  }

  /**
   * Parses the provided configuration and configure the entry cache.
   *
   * @param configuration  The new configuration containing the changes.
   * @param applyChanges   If true then take into account the new configuration.
   * @param errorHandler   An handler used to report errors.
   *
   * @return  <CODE>true</CODE> if configuration is acceptable,
   *          or <CODE>false</CODE> otherwise.
   */
  public boolean processEntryCacheConfig(
      FileSystemEntryCacheCfg             configuration,
      boolean                             applyChanges,
      EntryCacheCommon.ConfigErrorHandler errorHandler
      )
  {
    // Local variables to read configuration.
    DN                       newConfigEntryDN;
    long                     newLockTimeout;
    long                     newMaxEntries;
    long                     newMaxAllowedMemory;
    HashSet<SearchFilter>    newIncludeFilters = null;
    HashSet<SearchFilter>    newExcludeFilters = null;
    int                      newJECachePercent;
    long                     newJECacheSize;
    boolean                  newPersistentCache;
    boolean                  newCompactEncoding;
    String                   newCacheType = DEFAULT_FSCACHE_TYPE;
    String                   newCacheHome = DEFAULT_FSCACHE_HOME;
    SortedSet<String>        newJEProperties;

    EnvironmentMutableConfig newMutableEnvConfig =
      new EnvironmentMutableConfig();
    EnvironmentConfig        newEnvConfig =
      new EnvironmentConfig();

    // Read configuration.
    newConfigEntryDN = configuration.dn();
    newLockTimeout   = configuration.getLockTimeout();

    // If the value of zero arrives make sure it is traslated
    // to the maximum possible value we can cap maxEntries to.
    newMaxEntries = configuration.getMaxEntries();
    if (newMaxEntries <= 0) {
      newMaxEntries = DEFAULT_FSCACHE_MAX_ENTRIES;
    }

    // Maximum memory/space this cache can utilize.
    newMaxAllowedMemory = configuration.getMaxMemorySize();

    // Determine JE cache percent.
    newJECachePercent = configuration.getDatabaseCachePercent();

    // Determine JE cache size.
    newJECacheSize = configuration.getDatabaseCacheSize();

    // Check if this cache is persistent.
    newPersistentCache = configuration.isPersistentCache();

    // Check if this cache should use compact encoding.
    newCompactEncoding = configuration.isBackendCompactEncoding();

    // Get native JE properties.
    newJEProperties = configuration.getJEProperty();

    switch (errorHandler.getConfigPhase())
    {
    case PHASE_INIT:
      // Determine the cache type.
      newCacheType = configuration.getCacheType().toString();

      // Determine the cache home.
      newCacheHome = configuration.getCacheDirectory();

      newIncludeFilters = EntryCacheCommon.getFilters(
          configuration.getIncludeFilter(),
          ERR_FIFOCACHE_INVALID_INCLUDE_FILTER,
          WARN_FIFOCACHE_CANNOT_DECODE_ANY_INCLUDE_FILTERS,
          errorHandler,
          configEntryDN
          );
      newExcludeFilters = EntryCacheCommon.getFilters (
          configuration.getExcludeFilter(),
          WARN_FIFOCACHE_CANNOT_DECODE_EXCLUDE_FILTER,
          WARN_FIFOCACHE_CANNOT_DECODE_ANY_EXCLUDE_FILTERS,
          errorHandler,
          configEntryDN
          );
      // JE configuration properties.
      try {
        newMutableEnvConfig.setCachePercent((newJECachePercent != 0 ?
          newJECachePercent :
          EnvironmentConfig.DEFAULT.getCachePercent()));
      } catch (Exception e) {
        if (debugEnabled()) {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
        errorHandler.reportError(
          ERR_FSCACHE_CANNOT_SET_JE_MEMORY_PCT.get(),
          false,
          DirectoryServer.getServerErrorResultCode()
          );
      }
      try {
        newMutableEnvConfig.setCacheSize(newJECacheSize);
      } catch (Exception e) {
        if (debugEnabled()) {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
        errorHandler.reportError(
          ERR_FSCACHE_CANNOT_SET_JE_MEMORY_SIZE.get(),
          false,
          DirectoryServer.getServerErrorResultCode()
          );
      }
      // JE native properties.
      try {
        newEnvConfig = ConfigurableEnvironment.setJEProperties(
          newEnvConfig, newJEProperties, configAttrMap);
      } catch (Exception e) {
        if (debugEnabled()) {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
        errorHandler.reportError(
          ERR_FSCACHE_CANNOT_SET_JE_PROPERTIES.get(e.getMessage()),
          false, DirectoryServer.getServerErrorResultCode());
      }
      break;
    case PHASE_ACCEPTABLE:  // acceptable and apply are using the same
    case PHASE_APPLY:       // error ID codes
      newIncludeFilters = EntryCacheCommon.getFilters (
          configuration.getIncludeFilter(),
          ERR_FIFOCACHE_INVALID_INCLUDE_FILTER,
          null,
          errorHandler,
          configEntryDN
          );
      newExcludeFilters = EntryCacheCommon.getFilters (
          configuration.getExcludeFilter(),
          ERR_FIFOCACHE_INVALID_EXCLUDE_FILTER,
          null,
          errorHandler,
          configEntryDN
          );
      // Iterate through native JE properties.
      try {
        Map paramsMap = EnvironmentParams.SUPPORTED_PARAMS;
        // If this entry cache is disabled then there is no open JE
        // environment to check against, skip mutable check if so.
        if (configuration.isEnabled()) {
          newMutableEnvConfig =
            ConfigurableEnvironment.setJEProperties(
            entryCacheEnv.getConfig(), newJEProperties, configAttrMap);
          EnvironmentConfig oldEnvConfig = entryCacheEnv.getConfig();
          for (String jeEntry : newJEProperties) {
            // There is no need to validate properties yet again.
            StringTokenizer st = new StringTokenizer(jeEntry, "=");
            if (st.countTokens() == 2) {
              String jePropertyName = st.nextToken();
              String jePropertyValue = st.nextToken();
              ConfigParam param = (ConfigParam) paramsMap.get(jePropertyName);
              if (!param.isMutable()) {
                String oldValue = oldEnvConfig.getConfigParam(param.getName());
                String newValue = jePropertyValue;
                if (!oldValue.equalsIgnoreCase(newValue)) {
                  Message message =
                    INFO_CONFIG_JE_PROPERTY_REQUIRES_RESTART.get(
                    jePropertyName);
                  errorHandler.reportError(message, true, ResultCode.SUCCESS,
                    true);
                  if (debugEnabled()) {
                    TRACER.debugInfo("The change to the following property " +
                      "will take effect when the component is restarted: " +
                      jePropertyName);
                  }
                }
              }
            }
          }
        } else {
          newMutableEnvConfig =
            ConfigurableEnvironment.setJEProperties(
            new EnvironmentConfig(), newJEProperties, configAttrMap);
        }
      } catch (ConfigException ce) {
        errorHandler.reportError(ce.getMessageObject(),
          false, DirectoryServer.getServerErrorResultCode());
      } catch (Exception e) {
        errorHandler.reportError(
          Message.raw(stackTraceToSingleLineString(e)),
          false, DirectoryServer.getServerErrorResultCode());
      }
      break;
    }

    if (applyChanges && errorHandler.getIsAcceptable())
    {
      switch (errorHandler.getConfigPhase()) {
      case PHASE_INIT:
        cacheType = newCacheType;
        cacheHome = newCacheHome;
        entryCacheEnvConfig = newEnvConfig;
        entryCacheEnvMutableConfig = newMutableEnvConfig;
        break;
      case PHASE_APPLY:
        try {
            newMutableEnvConfig =
              entryCacheEnv.getMutableConfig();
            newMutableEnvConfig.setCachePercent((newJECachePercent != 0 ?
              newJECachePercent :
              EnvironmentConfig.DEFAULT.getCachePercent()));
            entryCacheEnv.setMutableConfig(newMutableEnvConfig);
            entryCacheEnv.evictMemory();
        } catch (Exception e) {
            if (debugEnabled()) {
              TRACER.debugCaught(DebugLogLevel.ERROR, e);
            }
            errorHandler.reportError(
              ERR_FSCACHE_CANNOT_SET_JE_MEMORY_PCT.get(),
              false,
              DirectoryServer.getServerErrorResultCode()
              );
        }
        try {
            newMutableEnvConfig =
              entryCacheEnv.getMutableConfig();
            newMutableEnvConfig.setCacheSize(newJECacheSize);
            entryCacheEnv.setMutableConfig(newMutableEnvConfig);
            entryCacheEnv.evictMemory();
        } catch (Exception e) {
            if (debugEnabled()) {
              TRACER.debugCaught(DebugLogLevel.ERROR, e);
            }
            errorHandler.reportError(
              ERR_FSCACHE_CANNOT_SET_JE_MEMORY_SIZE.get(),
              false,
              DirectoryServer.getServerErrorResultCode()
              );
        }
        try {
          EnvironmentConfig oldEnvConfig = entryCacheEnv.getConfig();
          newEnvConfig = ConfigurableEnvironment.setJEProperties(
            oldEnvConfig, newJEProperties, configAttrMap);
          // This takes care of changes to the JE environment for those
          // properties that are mutable at runtime.
          entryCacheEnv.setMutableConfig(newEnvConfig);
        } catch (Exception e) {
          if (debugEnabled()) {
              TRACER.debugCaught(DebugLogLevel.ERROR, e);
            }
            errorHandler.reportError(
              ERR_FSCACHE_CANNOT_SET_JE_PROPERTIES.get(e.getMessage()),
              false,
              DirectoryServer.getServerErrorResultCode()
              );
        }
        break;
      }

      configEntryDN    = newConfigEntryDN;
      maxEntries       = new AtomicLong(newMaxEntries);
      maxAllowedMemory = newMaxAllowedMemory;
      persistentCache  = newPersistentCache;

      encodeConfig     = new EntryEncodeConfig(true,
        newCompactEncoding, newCompactEncoding);

      setLockTimeout(newLockTimeout);
      setIncludeFilters(newIncludeFilters);
      setExcludeFilters(newExcludeFilters);

      registeredConfiguration = configuration;
    }

    return errorHandler.getIsAcceptable();
  }

  /**
   * Retrieves and decodes the entry with the specified DN from JE backend db.
   *
   * @param  entryDN   The DN of the entry to retrieve.
   *
   * @return  The requested entry if it is present in the cache, or
   *          <CODE>null</CODE> if it is not present.
   */
  private Entry getEntryFromDB(DN entryDN)
  {
    DatabaseEntry cacheEntryKey = new DatabaseEntry();
    DatabaseEntry primaryData = new DatabaseEntry();

    try {
      // Get the primary key and data.
      cacheEntryKey.setData(entryDN.toNormalizedString().getBytes("UTF-8"));
      if (entryCacheDB.get(null, cacheEntryKey,
              primaryData,
              LockMode.DEFAULT) == OperationStatus.SUCCESS) {

        Entry entry = Entry.decode(primaryData.getData());
        entry.setDN(entryDN);
        return entry;
      } else {
        throw new Exception();
      }
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      // Log an error message.
      logError(ERR_FSCACHE_CANNOT_RETRIEVE_ENTRY.get());
    }
    return null;
  }

  /**
   * Encodes and stores the entry in the JE backend db.
   *
   * @param  entry    The entry to store in the cache.
   * @param  backend  The backend with which the entry is associated.
   * @param  entryID  The entry ID within the provided backend that uniquely
   *                  identifies the specified entry.
   *
   * @return  <CODE>false</CODE> if some problem prevented the method from
   *          completing successfully, or <CODE>true</CODE> if the entry
   *          was either stored or the cache determined that this entry
   *          should never be cached for some reason.
   */
  private boolean putEntryToDB(Entry entry, Backend backend, long entryID) {
    try {
      // See if the current fs space usage is within acceptable constraints. If
      // so, then add the entry to the cache (or replace it if it is already
      // present).  If not, then remove an existing entry and don't add the new
      // entry.
      long usedMemory = 0;

      // Zero means unlimited here.
      if (maxAllowedMemory != 0) {
        // Get approximate current total log size of JE environment in bytes.
        usedMemory =
            entryCacheEnv.getStats(entryCacheEnvStatsConfig).getTotalLogSize();

        // TODO: Check and log a warning if usedMemory hits default or
        // configurable watermark, see Issue 1735.

        if (usedMemory > maxAllowedMemory) {
          long savedMaxEntries = maxEntries.longValue();
          // Cap maxEntries artificially but dont let it go negative under
          // any circumstances.
          maxEntries.set((dnMap.isEmpty() ? 0 : dnMap.size() - 1));
          // Add the entry to the map to trigger remove of the eldest entry.
          // @see LinkedHashMapRotator.removeEldestEntry() for more details.
          dnMap.put(entry.getDN(), entryID);
          // Restore the map and maxEntries.
          dnMap.remove(entry.getDN());
          maxEntries.set(savedMaxEntries);
          // We'll always return true in this case, even tho we didn't actually
          // add the entry due to memory constraints.
          return true;
        }
      }

      // Create key.
      DatabaseEntry cacheEntryKey = new DatabaseEntry();
      cacheEntryKey.setData(
          entry.getDN().toNormalizedString().getBytes("UTF-8"));

      // Create data and put this cache entry into the database.
      if (entryCacheDB.put(null, cacheEntryKey,
          new DatabaseEntry(
          entry.encode(encodeConfig))) == OperationStatus.SUCCESS) {

        // Add the entry to the cache maps. The order in which maps
        // are populated is important since invoking put on rotator
        // map can cause the eldest map entry to be removed @see
        // LinkedHashMapRotator.removeEldestEntry() therefore every
        // cache map has to be up to date if / when that happens.
        Map<Long,DN> map = backendMap.get(backend);
        if (map == null) {
          map = new LinkedHashMap<Long,DN>();
          map.put(entryID, entry.getDN());
          backendMap.put(backend, map);
        } else {
          map.put(entryID, entry.getDN());
        }
        dnMap.put(entry.getDN(), entryID);
      }

      // We'll always return true in this case, even if we didn't actually add
      // the entry due to memory constraints.
      return true;
    } catch (Exception e) {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      // Log an error message.
      logError(
          ERR_FSCACHE_CANNOT_STORE_ENTRY.get());

      return false;
    }
  }

 /**
  * Checks if the cache home exist and if not tries to recursively create it.
  * If either is successful adjusts cache home access permissions accordingly
  * to allow only process owner or the superuser to access JE environment.
  *
  * @param  cacheHome  String representation of complete file system path.
  *
  * @throws Exception  If failed to establish cache home.
  */
  private void checkAndSetupCacheHome(String cacheHome) throws Exception {

    boolean cacheHasHome = false;
    File cacheHomeDir = new File(cacheHome);
    if (cacheHomeDir.exists() &&
        cacheHomeDir.canRead() &&
        cacheHomeDir.canWrite()) {
      cacheHasHome = true;
    } else {
      try {
        cacheHasHome = cacheHomeDir.mkdirs();
      } catch (SecurityException e) {
        cacheHasHome = false;
      }
    }
    if ( cacheHasHome ) {
      // TODO: Investigate if its feasible to employ SetFileAttributes()
      // FILE_ATTRIBUTE_TEMPORARY attribute on Windows via native code.
      if(FilePermission.canSetPermissions()) {
        try {
          if(!FilePermission.setPermissions(cacheHomeDir,
              CACHE_HOME_PERMISSIONS)) {
            throw new Exception();
          }
        } catch(Exception e) {
          // Log a warning that the permissions were not set.
          Message message = WARN_FSCACHE_SET_PERMISSIONS_FAILED.get(cacheHome);
          logError(message);
        }
      }
    } else {
      throw new Exception();
    }
  }

  /**
   * Return a verbose string representation of the current cache maps.
   * This is useful primary for debugging and diagnostic purposes such
   * as in the entry cache unit tests.
   * @return String verbose string representation of the current cache
   *                maps in the following format: dn:id:backend
   *                one cache entry map representation per line
   *                or <CODE>null</CODE> if all maps are empty.
   */
  private String toVerboseString()
  {
    String verboseString = new String();
    StringBuilder sb = new StringBuilder();

    Map<DN,Long> dnMapCopy;
    Map<Backend,Map<Long,DN>> backendMapCopy;

    // Grab write lock to prevent any modifications
    // to the cache maps until a snapshot is taken.
    cacheWriteLock.lock();
    try {
      // Examining the real maps will hold the lock
      // and can cause map modifications in case of
      // any access order maps, make copies instead.
      dnMapCopy = new LinkedHashMap<DN,Long>(dnMap);
      backendMapCopy =
        new LinkedHashMap<Backend,Map<Long,DN>>
          (backendMap);
    } finally {
      cacheWriteLock.unlock();
    }

    // Check dnMap first.
    for (DN dn : dnMapCopy.keySet()) {
      sb.append(dn.toString());
      sb.append(":");
      sb.append((dnMapCopy.get(dn) != null ?
          dnMapCopy.get(dn).toString() : null));
      sb.append(":");
      Backend backend = null;
      String backendID = null;
      Iterator<Backend> backendIterator = backendMapCopy.keySet().iterator();
      while (backendIterator.hasNext()) {
        backend = backendIterator.next();
        Map<Long, DN> map = backendMapCopy.get(backend);
        if ((map != null) &&
            (map.get(dnMapCopy.get(dn)) != null) &&
            (map.get(dnMapCopy.get(dn)).equals(dn))) {
          backendID = backend.getBackendID();
          break;
        }
      }
      sb.append(backendID);
      sb.append(ServerConstants.EOL);
    }

    // See if there is anything on backendMap that isnt reflected on dnMap
    // in case maps went out of sync.
    Backend backend = null;
    Iterator<Backend> backendIterator = backendMapCopy.keySet().iterator();
    while (backendIterator.hasNext()) {
      backend = backendIterator.next();
      Map<Long, DN> map = backendMapCopy.get(backend);
      for (Long id : map.keySet()) {
        if (!dnMapCopy.containsKey(map.get(id)) || map.get(id) == null) {
          sb.append((map.get(id) != null ? map.get(id) : null));
          sb.append(":");
          sb.append(id.toString());
          sb.append(":");
          sb.append(backend.getBackendID());
          sb.append(ServerConstants.EOL);
        }
      }
    }

    verboseString = sb.toString();

    return (verboseString.length() > 0 ? verboseString : null);
  }

 /**
  * This inner class exist solely to override <CODE>removeEldestEntry()</CODE>
  * method of the LinkedHashMap.
  *
  * @see  java.util.LinkedHashMap
  */
  private class LinkedHashMapRotator<K,V> extends LinkedHashMap<K,V> {

    static final long serialVersionUID = 5271482121415968435L;

    public LinkedHashMapRotator(int initialCapacity,
                                float loadFactor,
                                boolean accessOrder) {
      super(initialCapacity, loadFactor, accessOrder);
    }

    // This method will get called each time we add a new key/value
    // pair to the map. The eldest entry will be selected by the
    // underlying LinkedHashMap implementation based on the access
    // order configured and will follow either FIFO implementation
    // by default or LRU implementation if configured so explicitly.
    @Override protected boolean removeEldestEntry(Map.Entry eldest) {
      // Check if we hit the limit on max entries and if so remove
      // the eldest entry otherwise do nothing.
      if (size() > maxEntries.longValue()) {
        DatabaseEntry cacheEntryKey = new DatabaseEntry();
        cacheWriteLock.lock();
        try {
          // Remove the the eldest entry from supporting maps.
          DN entryDN = (DN) eldest.getKey();
          long entryID = ((Long) eldest.getValue()).longValue();
          cacheEntryKey.setData(
              entryDN.toNormalizedString().getBytes("UTF-8"));
          Set<Backend> backendSet = backendMap.keySet();
          Iterator<Backend> backendIterator = backendSet.iterator();
          while (backendIterator.hasNext()) {
            Map<Long,DN> map = backendMap.get(backendIterator.next());
            if ((map.get(entryID) != null) &&
                (map.get(entryID).equals(entryDN))) {
              map.remove(entryID);
              // If this backend becomes empty now
              // remove it from the backend map.
              if (map.isEmpty()) {
                backendIterator.remove();
              }
              break;
            }
          }
          // Remove the the eldest entry from the database.
          entryCacheDB.delete(null, cacheEntryKey);
        } catch (Exception e) {
          if (debugEnabled()) {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        } finally {
          cacheWriteLock.unlock();
        }
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * This exception should be thrown if an error occurs while
   * trying to locate and load persistent cache index from
   * the existing entry cache database.
   */
  private class CacheIndexNotFoundException extends OpenDsException {
    static final long serialVersionUID = 6444756053577853869L;
    public CacheIndexNotFoundException() {}
    public CacheIndexNotFoundException(Message message) {
      super(message);
    }
  }

  /**
   * This exception should be thrown if persistent cache index
   * found in the existing entry cache database is determined
   * to be empty, inconsistent or damaged.
   */
  private class CacheIndexImpairedException extends OpenDsException {
    static final long serialVersionUID = -369455697709478407L;
    public CacheIndexImpairedException() {}
    public CacheIndexImpairedException(Message message) {
      super(message);
    }
  }

}
