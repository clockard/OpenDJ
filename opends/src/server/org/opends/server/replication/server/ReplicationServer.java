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
package org.opends.server.replication.server;

import static org.opends.server.loggers.Error.logError;
import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.messages.ReplicationMessages.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.getFileForPath;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.ChangelogServerCfg;
import org.opends.server.api.ConfigurableComponent;
import org.opends.server.api.DirectoryThread;
import org.opends.server.config.ConfigAttribute;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.replication.protocol.SocketSession;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.ResultCode;

import com.sleepycat.je.DatabaseException;

/**
 * ReplicationServer Listener.
 *
 * This singleton is the main object of the replication server
 * It waits for the incoming connections and create listener
 * and publisher objects for
 * connection with LDAP servers and with replication servers
 *
 * It is responsible for creating the replication server cache and managing it
 */
public class ReplicationServer
  implements Runnable, ConfigurableComponent,
             ConfigurationChangeListener<ChangelogServerCfg>
{
  private short serverId;
  private String serverURL;

  private ServerSocket listenSocket;
  private Thread myListenThread;
  private Thread myConnectThread;

  private boolean runListen = true;

  /* The list of replication servers configured by the administrator */
  private Collection<String> replicationServers;

  /* This table is used to store the list of dn for which we are currently
   * handling servers.
   */
  private HashMap<DN, ReplicationCache> baseDNs =
          new HashMap<DN, ReplicationCache>();

  private String localURL = "null";
  private boolean shutdown = false;
  private short changelogServerId;
  private DN configDn;
  private List<ConfigAttribute> configAttributes =
          new ArrayList<ConfigAttribute>();
  private ReplicationDbEnv dbEnv;
  private int rcvWindow;
  private int queueSize;
  private String dbDirname = null;
  private long trimAge; // the time (in sec) after which the  changes must
                        // de deleted from the persistent storage.

  /**
   * Creates a new Replication server using the provided configuration entry.
   *
   * @param configuration The configuration of this replication server.
   * @throws ConfigException When Configuration is invalid.
   */
  public ReplicationServer(ChangelogServerCfg configuration)
         throws ConfigException
  {
    shutdown = false;
    runListen = true;
    int changelogPort = configuration.getChangelogPort();
    changelogServerId = (short) configuration.getChangelogServerId();
    replicationServers = configuration.getChangelogServer();
    if (replicationServers == null)
      replicationServers = new ArrayList<String>();
    queueSize = configuration.getQueueSize();
    trimAge = configuration.getChangelogPurgeDelay();
    dbDirname = configuration.getChangelogDbDirectory();
    rcvWindow = configuration.getWindowSize();
    if (dbDirname == null)
    {
      dbDirname = "changelogDb";
    }
    // Chech that this path exists or create it.
    File f = getFileForPath(dbDirname);
    try
    {
      if (!f.exists())
      {
        f.mkdir();
      }
    }
    catch (Exception e)
    {
      throw new ConfigException(MSGID_FILE_CHECK_CREATE_FAILED,
          e.getMessage() + " " + getFileForPath(dbDirname));
    }

    initialize(changelogServerId, changelogPort);
    configuration.addChangeListener(this);
  }

  /**
   * {@inheritDoc}
   */
  public DN getConfigurableComponentEntryDN()
  {
    return configDn;
  }

  /**
   * {@inheritDoc}
   */
  public List<ConfigAttribute> getConfigurationAttributes()
  {
    return configAttributes ;
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasAcceptableConfiguration(ConfigEntry configEntry,
      List<String> unacceptableReasons)
  {
    // TODO NYI
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyNewConfiguration(ConfigEntry configEntry,
      boolean detailedResults)
  {
    // TODO NYI
    return null;
  }

  /**
   * spawn the listen thread and the connect thread.
   * Used a a workaround because there can be only one run method
   */
  public void run()
  {
    if (runListen)
    {
      runListen = false;
      runListen();
    }
    else
      runConnect();
  }

  /**
   * The run method for the Listen thread.
   * This thread accept incoming connections on the replication server
   * ports from other replication servers or from LDAP servers
   * and spawn further thread responsible for handling those connections
   */

  private void runListen()
  {
    Socket newSocket = null;
    while (shutdown == false)
    {
      // Wait on the replicationServer port.
      // Read incoming messages and create LDAP or ReplicationServer listener
      // and Publisher.

      try
      {
        newSocket =  listenSocket.accept();
        newSocket.setReceiveBufferSize(1000000);
        newSocket.setTcpNoDelay(true);
        ServerHandler handler = new ServerHandler(
                                     new SocketSession(newSocket), queueSize);
        handler.start(null, serverId, serverURL, rcvWindow, this);
      } catch (IOException e)
      {
        // ignore
        // TODO add some logging to allow problem debugging
      }
    }
  }

  /**
   * This method manages the connection with the other replication servers.
   * It periodically checks that this replication server is indeed connected
   * to all the other replication servers and if not attempts to
   * make the connection.
   */
  private void runConnect()
  {
    while (shutdown == false)
    {
      /*
       * periodically check that we are connected to all other
       * replication servers and if not establish the connection
       */
      for (ReplicationCache replicationCache: baseDNs.values())
      {
        Set<String> connectedChangelogs = replicationCache.getChangelogs();
        /*
         * check that all replication server in the config are in the connected
         * Set. If not create the connection
         */
        for (String serverURL : replicationServers)
        {
          if ((serverURL.compareTo(this.serverURL) != 0) &&
              (!connectedChangelogs.contains(serverURL)))
          {
            this.connect(serverURL, replicationCache.getBaseDn());
          }
        }
      }
      try
      {
        synchronized (this)
        {
          /* check if we are connected every second */
          wait(1000);
        }
      } catch (InterruptedException e)
      {
        // ignore error, will try to connect again or shutdown
      }
    }
  }

  /**
   * Establish a connection to the server with the address and port.
   *
   * @param serverURL  The address and port for the server, separated by a
   *                    colon.
   * @param baseDn     The baseDn of the connection
   */
  private void connect(String serverURL, DN baseDn)
  {
    String token[] = serverURL.split(":");
    String hostname = token[0];
    String port = token[1];

    try
    {
      InetSocketAddress ServerAddr = new InetSocketAddress(
                     InetAddress.getByName(hostname), Integer.parseInt(port));
      Socket socket = new Socket();
      socket.setReceiveBufferSize(1000000);
      socket.setTcpNoDelay(true);
      socket.connect(ServerAddr, 500);

      ServerHandler handler = new ServerHandler(
                                      new SocketSession(socket), queueSize);
     handler.start(baseDn, serverId, this.serverURL, rcvWindow, this);
    }
    catch (IOException e)
    {
      // ignore
    }

  }

  /**
   * initialization function for the replicationServer.
   *
   * @param  changelogId       The unique identifier for this replicationServer.
   * @param  changelogPort     The port on which the replicationServer should
   *                           listen.
   *
   */
  private void initialize(short changelogId, int changelogPort)
  {
    try
    {
      /*
       * Initialize the replicationServer database.
       */
      dbEnv = new ReplicationDbEnv(getFileForPath(dbDirname).getAbsolutePath(),
          this);

      /*
       * create replicationServer cache
       */
      serverId = changelogId;

      /*
       * Open replicationServer socket
       */
      String localhostname = InetAddress.getLocalHost().getHostName();
      String localAdddress = InetAddress.getLocalHost().getHostAddress();
      serverURL = localhostname + ":" + String.valueOf(changelogPort);
      localURL = localAdddress + ":" + String.valueOf(changelogPort);
      listenSocket = new ServerSocket();
      listenSocket.setReceiveBufferSize(1000000);
      listenSocket.bind(new InetSocketAddress(changelogPort));

      /*
       * create working threads
       */
      myListenThread = new DirectoryThread(this, "Replication Server Listener");
      myListenThread.start();
      myConnectThread = new DirectoryThread(this, "Replication Server Connect");
      myConnectThread.start();

    } catch (DatabaseException e)
    {
      int msgID = MSGID_COULD_NOT_INITIALIZE_DB;
      String message = getMessage(msgID, dbDirname);
      logError(ErrorLogCategory.SYNCHRONIZATION, ErrorLogSeverity.SEVERE_ERROR,
               message, msgID);
    } catch (ReplicationDBException e)
    {
      int msgID = MSGID_COULD_NOT_READ_DB;
      String message = getMessage(msgID, dbDirname);
      message += getMessage(e.getMessageID());
      logError(ErrorLogCategory.SYNCHRONIZATION, ErrorLogSeverity.SEVERE_ERROR,
               message, msgID);
    } catch (UnknownHostException e)
    {
      int msgID = MSGID_UNKNOWN_HOSTNAME;
      String message = getMessage(msgID);
      logError(ErrorLogCategory.SYNCHRONIZATION, ErrorLogSeverity.SEVERE_ERROR,
               message, msgID);
    } catch (IOException e)
    {
      int msgID = MSGID_COULD_NOT_BIND_CHANGELOG;
      String message = getMessage(msgID, changelogPort, e.getMessage());
      logError(ErrorLogCategory.SYNCHRONIZATION, ErrorLogSeverity.SEVERE_ERROR,
               message, msgID);
    }
  }

  /**
   * Get the ReplicationCache associated to the base DN given in parameter.
   *
   * @param baseDn The base Dn for which the ReplicationCache must be returned.
   * @return The ReplicationCache associated to the base DN given in parameter.
   */
  public ReplicationCache getReplicationCache(DN baseDn)
  {
    ReplicationCache replicationCache;

    synchronized (baseDNs)
    {
      replicationCache = baseDNs.get(baseDn);
      if (replicationCache == null)
        replicationCache = new ReplicationCache(baseDn, this);
      baseDNs.put(baseDn, replicationCache);
    }

    return replicationCache;
  }

  /**
   * Shutdown the Replication Server service and all its connections.
   */
  public void shutdown()
  {
    shutdown = true;

    // shutdown the connect thread
    try
    {
      myConnectThread.interrupt();
    } catch (NullPointerException e)
    {
      // FIXME To be investigated the conditions
      // where myConnectThread can be null here
    }

    // shutdown the listener thread
    try
    {
      listenSocket.close();
    } catch (IOException e)
    {
      // replication Server service is closing anyway.
    }

    // shutdown all the ChangelogCaches
    for (ReplicationCache replicationCache : baseDNs.values())
    {
      replicationCache.shutdown();
    }

    dbEnv.shutdown();
  }


  /**
   * Creates a new DB handler for this ReplicationServer and the serverId and
   * DN given in parameter.
   *
   * @param id The serverId for which the dbHandler must be created.
   * @param baseDn The DN for which the dbHandler muste be created.
   * @return The new DB handler for this ReplicationServer and the serverId and
   *         DN given in parameter.
   * @throws DatabaseException in case of underlying database problem.
   */
  public DbHandler newDbHandler(short id, DN baseDn) throws DatabaseException
  {
    return new DbHandler(id, baseDn, this, dbEnv);
  }

  /**
   * Retrieves the time after which changes must be deleted from the
   * persistent storage (in milliseconds).
   *
   * @return  The time after which changes must be deleted from the
   *          persistent storage (in milliseconds).
   */
  public long getTrimage()
  {
    return trimAge * 1000;
  }

  /**
   * Check if the provided configuration is acceptable for add.
   *
   * @param configuration The configuration to check.
   * @param unacceptableReasons When the configuration is not acceptable, this
   *                            table is use to return the reasons why this
   *                            configuration is not acceptbale.
   *
   * @return true if the configuration is acceptable, false other wise.
   */
  public static boolean isConfigurationAcceptable(
      ChangelogServerCfg configuration, List<String> unacceptableReasons)
  {
    int port = configuration.getChangelogPort();

    try
    {
      ServerSocket tmpSocket = new ServerSocket();
      tmpSocket.bind(new InetSocketAddress(port));
      tmpSocket.close();
    }
    catch (Exception e)
    {
      String message = getMessage(MSGID_COULD_NOT_BIND_CHANGELOG, port,
                                  e.getMessage());
      unacceptableReasons.add(message);
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(
      ChangelogServerCfg configuration)
  {
    // TODO : implement this
    return new ConfigChangeResult(ResultCode.SUCCESS, false);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
      ChangelogServerCfg configuration, List<String> unacceptableReasons)
  {
    // TODO : implement this
    return true;
  }
}
