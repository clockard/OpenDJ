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

import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.config.ConfigParam;
import com.sleepycat.je.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import java.io.File;
import java.io.FilenameFilter;

import org.opends.server.monitors.DatabaseEnvironmentMonitor;
import org.opends.server.types.DebugLogLevel;
import org.opends.server.types.DN;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.FilePermission;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.ResultCode;
import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.loggers.debug.DebugLogger.*;
import org.opends.server.loggers.debug.DebugTracer;
import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.messages.JebMessages.*;
import org.opends.server.api.Backend;
import org.opends.server.admin.std.server.JEBackendCfg;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.core.DirectoryServer;

/**
 * Wrapper class for the JE environment. Root container holds all the entry
 * containers for each base DN. It also maintains all the openings and closings
 * of the entry containers.
 */
public class RootContainer
     implements ConfigurationChangeListener<JEBackendCfg>
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();


  /**
   * The JE database environment.
   */
  private Environment env;

  /**
   * The backend configuration.
   */
  private Config config;

  /**
   * The backend to which this entry root container belongs.
   */
  private Backend backend;

  /**
   * The database environment monitor for this JE environment.
   */
  private DatabaseEnvironmentMonitor monitor;

  /**
   * The base DNs contained in this entryContainer.
   */
  private ConcurrentHashMap<DN, EntryContainer> entryContainers;

  /**
   * The cached value of the next entry identifier to be assigned.
   */
  private AtomicLong nextid = new AtomicLong(1);

  /**
   * Creates a new RootContainer object. Each root container represents a JE
   * environment.
   *
   * @param config The configuration of the JE backend.
   * @param backend A reference to the JE back end that is creating this
   *                root container.
   */
  public RootContainer(Config config, Backend backend)
  {
    this.env = null;
    this.monitor = null;
    this.entryContainers = new ConcurrentHashMap<DN, EntryContainer>();
    this.backend = backend;
    this.config = config;
  }

  /**
   * Helper method to apply database directory permissions and create a new
   * JE environment.
   *
   * @param backendDirectory The environment home directory for JE.
   * @param backendPermission The file permissions for the environment home
   *                          directory.
   * @param envConfig The JE environment configuration.
   * @throws DatabaseException If an error occurs when creating the environment.
   */
  private void open(File backendDirectory,
                    FilePermission backendPermission,
                    EnvironmentConfig envConfig) throws DatabaseException
  {
    // Get the backend database backendDirectory permissions and apply
    if(FilePermission.canSetPermissions())
    {
      try
      {
        if(!FilePermission.setPermissions(backendDirectory, backendPermission))
        {
          int msgID = MSGID_JEB_UNABLE_SET_PERMISSIONS;
          String message = getMessage(msgID, backendPermission.toString(),
                                      backendDirectory.toString());
          logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.MILD_WARNING,
                   message, msgID);
        }
      }
      catch(Exception e)
      {
        // Log an warning that the permissions were not set.
        int msgID = MSGID_JEB_SET_PERMISSIONS_FAILED;
        String message = getMessage(msgID, backendDirectory.toString(),
                                    e.toString());
        logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.SEVERE_WARNING,
                 message, msgID);
      }
    }

    // Open the database environment
    env = new Environment(backendDirectory,
                          envConfig);

    if (debugEnabled())
    {
      TRACER.debugInfo("JE (%s) environment opened with the following " +
          "config: %n%s", JEVersion.CURRENT_VERSION.toString(),
                          env.getConfig().toString());

          // Get current size of heap in bytes
    long heapSize = Runtime.getRuntime().totalMemory();

    // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
    // Any attempt will result in an OutOfMemoryException.
    long heapMaxSize = Runtime.getRuntime().maxMemory();

    // Get amount of free memory within the heap in bytes. This size will
      // increase
    // after garbage collection and decrease as new objects are created.
    long heapFreeSize = Runtime.getRuntime().freeMemory();

      TRACER.debugInfo("Current size of heap: %d bytes", heapSize);
      TRACER.debugInfo("Max size of heap: %d bytes", heapMaxSize);
      TRACER.debugInfo("Free memory in heap: %d bytes", heapFreeSize);
    }
  }

  /**
   * Opens the root container.
   *
   * @throws DatabaseException If an error occurs when opening the container.
   */
  public void open() throws DatabaseException
  {
    open(config.getBackendDirectory(),
         config.getBackendPermission(),
         config.getEnvironmentConfig());
  }

  /**
   * Opens the root container using the configuration parameters provided. Any
   * configuration parameters provided will override the parameters in the
   * JE configuration object.
   *
   * @param backendDirectory The environment home directory for JE.
   * @param backendPermission he file permissions for the environment home
   *                          directory.
   * @param readOnly Open the container in read only mode.
   * @param allowCreate Allow creating new entries in the container.
   * @param transactional Use transactions on operations.
   * @param txnNoSync Use asynchronous transactions.
   * @param isLocking Create the environment with locking.
   * @param runCheckPointer Start the checkpointer.
   * @throws DatabaseException If an error occurs when openinng the container.
   */
  public void open(File backendDirectory,
                   FilePermission backendPermission,
                   boolean readOnly,
                   boolean allowCreate,
                   boolean transactional,
                   boolean txnNoSync,
                   boolean isLocking,
                   boolean runCheckPointer) throws DatabaseException
  {
    EnvironmentConfig envConfig;
    if(config.getEnvironmentConfig() != null)
    {
      envConfig = config.getEnvironmentConfig();
    }
    else
    {
      envConfig = new EnvironmentConfig();
    }
    envConfig.setReadOnly(readOnly);
    envConfig.setAllowCreate(allowCreate);
    envConfig.setTransactional(transactional);
    envConfig.setTxnNoSync(txnNoSync);
    envConfig.setConfigParam("je.env.isLocking", String.valueOf(isLocking));
    envConfig.setConfigParam("je.env.runCheckpointer",
                             String.valueOf(runCheckPointer));

    open(backendDirectory, backendPermission, envConfig);
  }

  /**
   * Opens the entry container for a base DN. If the entry container does not
   * exist for the base DN, it will be created. The entry container will be
   * opened with the same mode as the root container. Any entry containers
   * opened in a read only root container will also be read only. Any entry
   * containers opened in a non transactional root container will also be non
   * transactional.
   *
   * @param baseDN The base DN of the entry container to open.
   * @return The opened entry container.
   * @throws DatabaseException If an error occurs while opening the entry
   *                           container.
   */
  public EntryContainer openEntryContainer(DN baseDN) throws DatabaseException
  {
    EntryContainer ec = new EntryContainer(baseDN, backend, config, env, this);
    EntryContainer ec1=this.entryContainers.get(baseDN);
    //If an entry container for this baseDN is already open we don't allow
    //another to be opened.
      if (ec1 != null)
          throw new DatabaseException("Entry container for baseDN " +
                  baseDN.toString() + " already is open.");
    if(env.getConfig().getReadOnly())
    {
      ec.openReadOnly();
    }
    else if(!env.getConfig().getTransactional())
    {
      ec.openNonTransactional(true);
    }
    else
    {
      ec.open();
    }
    this.entryContainers.put(baseDN, ec);

    return ec;
  }

  /**
   * Opens the entry containers for multiple base DNs.
   *
   * @param baseDNs The base DNs of the entry containers to open.
   * @throws DatabaseException If an error occurs while opening the entry
   *                           container.
   */
  public void openEntryContainers(DN[] baseDNs) throws DatabaseException
  {
    EntryID id;
    EntryID highestID = null;
    for(DN baseDN : baseDNs)
    {
      EntryContainer ec = openEntryContainer(baseDN);
      id = ec.getHighestEntryID();
      if(highestID == null || id.compareTo(highestID) > 0)
      {
        highestID = id;
      }
    }

    nextid = new AtomicLong(highestID.longValue() + 1);
  }

  /**
   * Close the entry container for a base DN.
   *
   * @param baseDN The base DN of the entry container to close.
   * @throws DatabaseException If an error occurs while closing the entry
   *                           container.
   */
  public void closeEntryContainer(DN baseDN) throws DatabaseException
  {
    getEntryContainer(baseDN).close();
    entryContainers.remove(baseDN);
  }

  /**
   * Close and remove a entry container for a base DN from disk.
   *
   * @param baseDN The base DN of the entry container to remove.
   * @throws DatabaseException If an error occurs while removing the entry
   *                           container.
   */
  public void removeEntryContainer(DN baseDN) throws DatabaseException
  {
    getEntryContainer(baseDN).close();
    getEntryContainer(baseDN).removeContainer();
    entryContainers.remove(baseDN);
  }

  /**
   * Get the DatabaseEnvironmentMonitor object for JE environment used by this
   * root container.
   *
   * @return The DatabaseEnvironmentMonito object.
   */
  public DatabaseEnvironmentMonitor getMonitorProvider()
  {
    if(monitor == null)
    {
      String monitorName = backend.getBackendID() + " Database Environment";
      monitor = new DatabaseEnvironmentMonitor(monitorName, env);
    }

    return monitor;
  }

  /**
   * Preload the database cache. There is no preload if the configured preload
   * time limit is zero.
   */
  public void preload()
  {
    long timeLimit = config.getPreloadTimeLimit();

    if (timeLimit > 0)
    {
      // Get a list of all the databases used by the backend.
      ArrayList<Database> dbList = new ArrayList<Database>();
      for (EntryContainer ec : entryContainers.values())
      {
        ec.listDatabases(dbList);
      }

      // Sort the list in order of priority.
      Collections.sort(dbList, new DbPreloadComparator());

      // Preload each database until we reach the time limit or the cache
      // is filled.
      try
      {
        long timeEnd = System.currentTimeMillis() + timeLimit;

        // Configure preload of Leaf Nodes (LNs) containing the data values.
        PreloadConfig preloadConfig = new PreloadConfig();
        preloadConfig.setLoadLNs(true);

        for (Database db : dbList)
        {
          // Calculate the remaining time.
          long timeRemaining = timeEnd - System.currentTimeMillis();
          if (timeRemaining <= 0)
          {
            break;
          }

          preloadConfig.setMaxMillisecs(timeRemaining);
          PreloadStats preloadStats = db.preload(preloadConfig);

          if(debugEnabled())
          {
            TRACER.debugInfo("file=" + db.getDatabaseName() +
                      " LNs=" + preloadStats.getNLNsLoaded());
          }

          // Stop if the cache is full or the time limit has been exceeded.
          if (preloadStats.getStatus() != PreloadStatus.SUCCESS)
          {
            break;
          }
        }

        // Log an informational message about the size of the cache.
        EnvironmentStats stats = env.getStats(new StatsConfig());
        long total = stats.getCacheTotalBytes();

        int msgID = MSGID_JEB_CACHE_SIZE_AFTER_PRELOAD;
        String message = getMessage(msgID, total / (1024 * 1024));
        logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.NOTICE, message,
                 msgID);
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

  /**
   * Synchronously invokes the cleaner on the database environment then forces a
   * checkpoint to delete the log files that are no longer in use.
   *
   * @throws DatabaseException If an error occurs while cleaning the database
   * environment.
   */
  private void cleanDatabase()
       throws DatabaseException
  {
    int msgID;
    String message;

    FilenameFilter filenameFilter = new FilenameFilter()
    {
      public boolean accept(File d, String name)
      {
        return name.endsWith(".jdb");
      }
    };

    File backendDirectory = env.getHome();
    int beforeLogfileCount = backendDirectory.list(filenameFilter).length;

    msgID = MSGID_JEB_CLEAN_DATABASE_START;
    message = getMessage(msgID, beforeLogfileCount, backendDirectory.getPath());
    logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.NOTICE, message,
             msgID);

    int currentCleaned = 0;
    int totalCleaned = 0;
    while ((currentCleaned = env.cleanLog()) > 0)
    {
      totalCleaned += currentCleaned;
    }

    msgID = MSGID_JEB_CLEAN_DATABASE_MARKED;
    message = getMessage(msgID, totalCleaned);
    logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.NOTICE, message,
             msgID);

    if (totalCleaned > 0)
    {
      CheckpointConfig force = new CheckpointConfig();
      force.setForce(true);
      env.checkpoint(force);
    }

    int afterLogfileCount = backendDirectory.list(filenameFilter).length;

    msgID = MSGID_JEB_CLEAN_DATABASE_FINISH;
    message = getMessage(msgID, afterLogfileCount);
    logError(ErrorLogCategory.BACKEND, ErrorLogSeverity.NOTICE, message,
             msgID);

  }

  /**
   * Close the root entryContainer.
   *
   * @throws DatabaseException If an error occurs while attempting to close
   * the entryContainer.
   */
  public void close() throws DatabaseException
  {
    for(DN baseDN : entryContainers.keySet())
    {
      entryContainers.get(baseDN).close();
      entryContainers.remove(baseDN);
    }

    if (env != null) env.close();
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
  public Set<DN> getBaseDNs()
  {
    return entryContainers.keySet();
  }

  /**
   * Return the entry container for a specific base DN.
   *
   * @param baseDN The base DN of the entry container to retrive.
   * @return The entry container for the base DN.
   */
  public EntryContainer getEntryContainer(DN baseDN)
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
   * Get the environment stats of the JE environment used in this root
   * container.
   *
   * @param statsConfig The configuration to use for the EnvironmentStats
   *                    object.
   * @return The environment status of the JE environment.
   * @throws DatabaseException If an error occurs while retriving the stats
   *                           object.
   */
  public EnvironmentStats getEnvironmentStats(StatsConfig statsConfig)
      throws DatabaseException
  {
    return env.getStats(statsConfig);
  }

  /**
   * Get the environment config of the JE environment used in this root
   * container.
   *
   * @return The environment config of the JE environment.
   * @throws DatabaseException If an error occurs while retriving the
   *                           configuration object.
   */
  public EnvironmentConfig getEnvironmentConfig() throws DatabaseException
  {
    return env.getConfig();
  }

  /**
   * Get the total number of entries in this root container.
   *
   * @return The number of entries in this root container
   * @throws DatabaseException If an error occurs while retriving the entry
   *                           count.
   */
  public long getEntryCount() throws DatabaseException
  {
    long entryCount = 0;
    for(EntryContainer ec : this.entryContainers.values())
    {
      entryCount += ec.getEntryCount();
    }

    return entryCount;
  }

  /**
   * Assign the next entry ID.
   *
   * @return The assigned entry ID.
   */
  public EntryID getNextEntryID()
  {
    return new EntryID(nextid.getAndIncrement());
  }

  /**
   * Return the lowest entry ID assigned.
   *
   * @return The lowest entry ID assigned.
   */
  public Long getLowestEntryID()
  {
    return 1L;
  }

  /**
   * Return the highest entry ID assigned.
   *
   * @return The highest entry ID assigned.
   */
  public Long getHighestEntryID()
  {
    return (nextid.get() - 1);
  }



  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
       JEBackendCfg cfg,
       List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    // This listener handles only the changes to JE properties.

    try
    {
      ConfigurableEnvironment.parseConfigEntry(cfg);
    }
    catch (Exception e)
    {
      unacceptableReasons.add(e.getMessage());
      acceptable = false;
    }

    return acceptable;
  }



  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(JEBackendCfg cfg)
  {
    ConfigChangeResult ccr;
    boolean adminActionRequired = false;
    ArrayList<String> messages = new ArrayList<String>();

    try
    {
      // Check if any JE non-mutable properties were changed.
      EnvironmentConfig oldEnvConfig = env.getConfig();
      EnvironmentConfig newEnvConfig =
           ConfigurableEnvironment.parseConfigEntry(cfg);
      Map paramsMap = EnvironmentParams.SUPPORTED_PARAMS;
      for (Object o : paramsMap.values())
      {
        ConfigParam param = (ConfigParam) o;
        if (!param.isMutable())
        {
          String oldValue = oldEnvConfig.getConfigParam(param.getName());
          String newValue = newEnvConfig.getConfigParam(param.getName());
          if (!oldValue.equalsIgnoreCase(newValue))
          {
            adminActionRequired = true;
            String configAttr = ConfigurableEnvironment.
                 getAttributeForProperty(param.getName());
            if (configAttr != null)
            {
              int msgID = MSGID_JEB_CONFIG_ATTR_REQUIRES_RESTART;
              messages.add(getMessage(msgID, configAttr));
            }
            if(debugEnabled())
            {
              TRACER.debugInfo("The change to the following property will " +
                        "take effect when the backend is restarted: " +
                        param.getName());
            }
          }
        }
      }

      // This takes care of changes to the JE environment for those
      // properties that are mutable at runtime.
      env.setMutableConfig(newEnvConfig);

      if (debugEnabled())
      {
        TRACER.debugInfo(env.getConfig().toString());
      }
    }
    catch (Exception e)
    {
      messages.add(e.getMessage());
      ccr = new ConfigChangeResult(DirectoryServer.getServerErrorResultCode(),
                                   adminActionRequired,
                                   messages);
      return ccr;
    }


    ccr = new ConfigChangeResult(ResultCode.SUCCESS, adminActionRequired,
                                 messages);
    return ccr;
  }
}
