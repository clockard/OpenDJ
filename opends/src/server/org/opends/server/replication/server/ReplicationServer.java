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
 *      Copyright 2006-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2011-2013 ForgeRock AS
 */
package org.opends.server.replication.server;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import org.opends.messages.Category;
import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import org.opends.messages.Severity;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.meta.VirtualAttributeCfgDefn;
import org.opends.server.admin.std.meta.VirtualAttributeCfgDefn.*;
import org.opends.server.admin.std.server.ReplicationServerCfg;
import org.opends.server.admin.std.server.UserDefinedVirtualAttributeCfg;
import org.opends.server.api.*;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.WorkflowImpl;
import org.opends.server.core.networkgroups.NetworkGroup;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.replication.common.*;
import org.opends.server.replication.plugin.MultimasterReplication;
import org.opends.server.replication.protocol.*;
import org.opends.server.replication.server.changelog.api.CNIndexRecord;
import org.opends.server.replication.server.changelog.api.ChangeNumberIndexDB;
import org.opends.server.replication.server.changelog.api.ChangelogException;
import org.opends.server.replication.server.changelog.je.DbHandler;
import org.opends.server.replication.server.changelog.je.DraftCNDbHandler;
import org.opends.server.replication.server.changelog.je.ReplicationDbEnv;
import org.opends.server.types.*;
import org.opends.server.util.LDIFReader;
import org.opends.server.util.ServerConstants;
import org.opends.server.util.TimeThread;
import org.opends.server.workflowelement.externalchangelog.ECLWorkflowElement;

import static org.opends.messages.ReplicationMessages.*;
import static org.opends.server.loggers.ErrorLogger.*;
import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.types.ResultCode.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;

/**
 * ReplicationServer Listener. This singleton is the main object of the
 * replication server. It waits for the incoming connections and create listener
 * and publisher objects for connection with LDAP servers and with replication
 * servers It is responsible for creating the replication server
 * replicationServerDomain and managing it
 */
public final class ReplicationServer
  implements ConfigurationChangeListener<ReplicationServerCfg>,
             BackupTaskListener, RestoreTaskListener, ImportTaskListener,
             ExportTaskListener
{
  private int serverId;
  private String serverURL;

  private ServerSocket listenSocket;
  private Thread listenThread;
  private Thread connectThread;

  /** The list of replication server URLs configured by the administrator. */
  private Collection<String> replicationServerUrls;

  /**
   * This table is used to store the list of dn for which we are currently
   * handling servers.
   */
  private final Map<String, ReplicationServerDomain> baseDNs =
          new HashMap<String, ReplicationServerDomain>();

  private volatile boolean shutdown = false;
  private ReplicationDbEnv dbEnv;
  private int rcvWindow;
  private int queueSize;
  private String dbDirname = null;

  /**
   * The delay (in sec) after which the changes must be deleted from the
   * persistent storage.
   */
  private long purgeDelay;

  private int replicationPort;
  private boolean stopListen = false;
  private ReplSessionSecurity replSessionSecurity;

  /**
   * For the backend associated to this replication server, DN of the config
   * entry of the backend.
   */
  private DN backendConfigEntryDN;
  /** ID of the backend. */
  private static final String backendId = "replicationChanges";

  /*
   * Assured mode properties
   */
  /** Timeout (in milliseconds) when waiting for acknowledgments. */
  private long assuredTimeout = 1000;

  /** Group id. */
  private byte groupId = 1;

  /**
   * Number of pending changes for a DS, considered as threshold value to put
   * the DS in DEGRADED_STATUS. If value is 0, status analyzer is disabled.
   */
  private int degradedStatusThreshold = 5000;

  /**
   * Number of milliseconds to wait before sending new monitoring messages. If
   * value is 0, monitoring publisher is disabled.
   */
  private long monitoringPublisherPeriod = 3000;

  /**
   * The handler of the changelog database, the database stores the relation
   * between a change number and the associated cookie.
   * <p>
   * Guarded by cnIndexDBLock
   */
  private ChangeNumberIndexDB cnIndexDB;

  /**
   * The last value generated of the change number.
   * <p>
   * Guarded by cnIndexDBLock
   **/
  private long lastGeneratedChangeNumber = 0;

  /** Used for protecting {@link ChangeNumberIndexDB} related state. */
  private final Object cnIndexDBLock = new Object();

  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();

  private static String externalChangeLogWorkflowID =
    "External Changelog Workflow ID";
  private ECLWorkflowElement eclwe;
  private WorkflowImpl externalChangeLogWorkflowImpl = null;

  /**
   * This is required for unit testing, so that we can keep track of all the
   * replication servers which are running in the VM.
   */
  private static Set<Integer> localPorts = new CopyOnWriteArraySet<Integer>();

  // Monitors for synchronizing domain creation with the connect thread.
  private final Object domainTicketLock = new Object();
  private final Object connectThreadLock = new Object();
  private long domainTicket = 0L;

  /** BaseDNs excluded for ECL. */
  private Set<String> excludedBaseDNs = new HashSet<String>();

  /**
   * The weight affected to the replication server.
   * Each replication server of the topology has a weight. When combined
   * together, the weights of the replication servers of a same group can be
   * translated to a percentage that determines the quantity of directory
   * servers of the topology that should be connected to a replication server.
   * For instance imagine a topology with 3 replication servers (with the same
   * group id) with the following weights: RS1=1, RS2=1, RS3=2. This means that
   * RS1 should have 25% of the directory servers connected in the topology,
   * RS2 25%, and RS3 50%. This may be useful if the replication servers of the
   * topology have a different power and one wants to spread the load between
   * the replication servers according to their power.
   */
  private int weight = 1;

  /**
   * Holds the list of all replication servers instantiated in this VM.
   * This allows to perform clean up of the RS databases in unit tests.
   */
  private static List<ReplicationServer> allInstances =
    new ArrayList<ReplicationServer>();

  /**
   * Creates a new Replication server using the provided configuration entry.
   *
   * @param configuration The configuration of this replication server.
   * @throws ConfigException When Configuration is invalid.
   */
  public ReplicationServer(ReplicationServerCfg configuration)
    throws ConfigException
  {
    replicationPort = configuration.getReplicationPort();
    serverId = configuration.getReplicationServerId();
    replicationServerUrls = configuration.getReplicationServer();
    if (replicationServerUrls == null)
      replicationServerUrls = new ArrayList<String>();
    queueSize = configuration.getQueueSize();
    purgeDelay = configuration.getReplicationPurgeDelay();
    dbDirname = configuration.getReplicationDBDirectory();
    rcvWindow = configuration.getWindowSize();
    if (dbDirname == null)
    {
      dbDirname = "changelogDb";
    }
    // Check that this path exists or create it.
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
      MessageBuilder mb = new MessageBuilder();
      mb.append(e.getLocalizedMessage());
      mb.append(" ");
      mb.append(String.valueOf(getFileForPath(dbDirname)));
      Message msg = ERR_FILE_CHECK_CREATE_FAILED.get(mb.toString());
      throw new ConfigException(msg, e);
    }
    groupId = (byte)configuration.getGroupId();
    weight = configuration.getWeight();
    assuredTimeout = configuration.getAssuredTimeout();
    degradedStatusThreshold = configuration.getDegradedStatusThreshold();
    monitoringPublisherPeriod = configuration.getMonitoringPeriod();

    replSessionSecurity = new ReplSessionSecurity();
    initialize();
    configuration.addChangeListener(this);
    try
    {
      backendConfigEntryDN =
         DN.decode("ds-cfg-backend-id=" + backendId + ",cn=Backends,cn=config");
    } catch (Exception e) { /* do nothing */ }

    // Creates the backend associated to this ReplicationServer
    // if it does not exist.
    createBackend();

    DirectoryServer.registerBackupTaskListener(this);
    DirectoryServer.registerRestoreTaskListener(this);
    DirectoryServer.registerExportTaskListener(this);
    DirectoryServer.registerImportTaskListener(this);

    localPorts.add(replicationPort);

    // Keep track of this new instance
    allInstances.add(this);
  }

  /**
   * Get the list of every replication servers instantiated in the current VM.
   * @return The list of every replication servers instantiated in the current
   * VM.
   */
  public static List<ReplicationServer> getAllInstances()
  {
    return allInstances;
  }

  /**
   * The run method for the Listen thread.
   * This thread accept incoming connections on the replication server
   * ports from other replication servers or from LDAP servers
   * and spawn further thread responsible for handling those connections
   */

  void runListen()
  {
    Message listenMsg = NOTE_REPLICATION_SERVER_LISTENING.get(
        getServerId(),
        listenSocket.getInetAddress().getHostAddress(),
        listenSocket.getLocalPort());
    logError(listenMsg);

    while (!shutdown && !stopListen)
    {
      // Wait on the replicationServer port.
      // Read incoming messages and create LDAP or ReplicationServer listener
      // and Publisher.

      try
      {
        Session session;
        Socket newSocket = null;
        try
        {
          newSocket = listenSocket.accept();
          newSocket.setTcpNoDelay(true);
          newSocket.setKeepAlive(true);
          int timeoutMS = MultimasterReplication.getConnectionTimeoutMS();
          session = replSessionSecurity.createServerSession(newSocket,
              timeoutMS);
          if (session == null) // Error, go back to accept
            continue;
        }
        catch (Exception e)
        {
          // If problems happen during the SSL handshake, it is necessary
          // to close the socket to free the associated resources.
          if (newSocket != null)
            newSocket.close();
          continue;
        }

        ReplicationMsg msg = session.receive();

        if (msg instanceof ServerStartMsg)
        {
          DataServerHandler dsHandler = new DataServerHandler(
              session, queueSize, this, rcvWindow);
          dsHandler.startFromRemoteDS((ServerStartMsg) msg);
        }
        else if (msg instanceof ReplServerStartMsg)
        {
          ReplicationServerHandler rsHandler = new ReplicationServerHandler(
              session, queueSize, this, rcvWindow);
          rsHandler.startFromRemoteRS((ReplServerStartMsg) msg);
        }
        else if (msg instanceof ServerStartECLMsg)
        {
          ECLServerHandler eclHandler = new ECLServerHandler(
              session, queueSize, this, rcvWindow);
          eclHandler.startFromRemoteServer((ServerStartECLMsg) msg);
        }
        else
        {
          // We did not recognize the message, close session as what
          // can happen after is undetermined and we do not want the server to
          // be disturbed
          ServerHandler.closeSession(session, null, null);
          return;
        }
      }
      catch (Exception e)
      {
        // The socket has probably been closed as part of the
        // shutdown or changing the port number process.
        // Just log debug information and loop.
        // Do not log the message during shutdown.
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
        if (!shutdown) {
          Message message =
            ERR_EXCEPTION_LISTENING.get(e.getLocalizedMessage());
          logError(message);
        }
      }
    }
  }

  /**
   * This method manages the connection with the other replication servers.
   * It periodically checks that this replication server is indeed connected
   * to all the other replication servers and if not attempts to
   * make the connection.
   */
  void runConnect()
  {
    synchronized (connectThreadLock)
    {
      while (!shutdown)
      {
        /*
         * Periodically check that we are connected to all other replication
         * servers and if not establish the connection
         */
        for (ReplicationServerDomain domain : getReplicationServerDomains())
        {
          // Create a normalized set of server URLs.
          final Set<String> connectedRSUrls = getConnectedRSUrls(domain);

          /*
           * check that all replication server in the config are in the
           * connected Set. If not, create the connection
           */
          for (String rsURL : replicationServerUrls)
          {
            final int separator = rsURL.lastIndexOf(':');
            final String hostname = rsURL.substring(0, separator);
            final int port = Integer.parseInt(rsURL.substring(separator + 1));

            final InetAddress inetAddress;
            try
            {
              inetAddress = InetAddress.getByName(hostname);
            }
            catch (UnknownHostException e)
            {
              // If the host name cannot be resolved then no chance of
              // connecting anyway.
              Message message = ERR_COULD_NOT_SOLVE_HOSTNAME.get(hostname);
              logError(message);
              continue;
            }

            // Avoid connecting to self.

            // FIXME: this will need changing if we ever support listening on
            // specific addresses.
            if ((isLocalAddress(inetAddress) && port == replicationPort)
            // Don't connect to a server if it is already connected.
                || connectedRSUrls.contains(normalizeServerURL(rsURL)))
            {
              continue;
            }

            connect(rsURL, domain.getBaseDn());
          }
        }

        // Notify any threads waiting with domain tickets after each iteration.
        synchronized (domainTicketLock)
        {
          domainTicket++;
          domainTicketLock.notifyAll();
        }

        // Retry each second.
        final int randomizer = (int) (Math.random() * 100);
        try
        {
          // Releases lock, allows threads to get domain ticket.
          connectThreadLock.wait(1000 + randomizer);
        }
        catch (InterruptedException e)
        {
          // Signaled to shutdown.
          return;
        }
      }
    }
  }

  private Set<String> getConnectedRSUrls(ReplicationServerDomain domain)
  {
    Set<String> results = new HashSet<String>();
    for (ReplicationServerHandler rsHandler : domain.getConnectedRSs().values())
    {
      results.add(normalizeServerURL(rsHandler.getServerAddressURL()));
    }
    return results;
  }

  /**
   * Establish a connection to the server with the address and port.
   *
   * @param remoteServerURL  The address and port for the server, separated by a
   *                    colon.
   * @param baseDn     The baseDn of the connection
   */
  private void connect(String remoteServerURL, String baseDn)
  {
    int separator = remoteServerURL.lastIndexOf(':');
    String port = remoteServerURL.substring(separator + 1);
    String hostname = remoteServerURL.substring(0, separator);
    boolean sslEncryption =replSessionSecurity.isSslEncryption(remoteServerURL);

    if (debugEnabled())
      TRACER.debugInfo("RS " + getMonitorInstanceName() + " connects to "
          + remoteServerURL);

    Socket socket = new Socket();
    Session session = null;
    try
    {
      InetSocketAddress ServerAddr = new InetSocketAddress(
          InetAddress.getByName(hostname), Integer.parseInt(port));
      socket.setTcpNoDelay(true);
      int timeoutMS = MultimasterReplication.getConnectionTimeoutMS();
      socket.connect(ServerAddr, timeoutMS);
      session = replSessionSecurity.createClientSession(socket, timeoutMS);

      ReplicationServerHandler rsHandler = new ReplicationServerHandler(
          session, queueSize, this, rcvWindow);
      rsHandler.connect(baseDn, sslEncryption);
    }
    catch (Exception e)
    {
      close(session);
      close(socket);
    }
  }

  /**
   * initialization function for the replicationServer.
   */
  private void initialize()
  {
    shutdown = false;

    try
    {
      // Initialize the replicationServer database.
      dbEnv = new ReplicationDbEnv(getFileForPath(dbDirname).getAbsolutePath(),
          this);
      dbEnv.initializeFromChangelogStateDB();

      setServerURL();
      listenSocket = new ServerSocket();
      listenSocket.bind(new InetSocketAddress(replicationPort));

      // creates working threads: we must first connect, then start to listen.
      if (debugEnabled())
        TRACER.debugInfo("RS " +getMonitorInstanceName()+
            " creates connect thread");
      connectThread = new ReplicationServerConnectThread(this);
      connectThread.start();

      if (debugEnabled())
        TRACER.debugInfo("RS " +getMonitorInstanceName()+
            " creates listen thread");

      listenThread = new ReplicationServerListenThread(this);
      listenThread.start();

      // Creates the ECL workflow elem so that DS (LDAPReplicationDomain)
      // can know me and really enableECL.
      if (WorkflowImpl.getWorkflow(externalChangeLogWorkflowID) != null)
      {
        // Already done. Nothing to do
        return;
      }
      eclwe = new ECLWorkflowElement(this);

      if (debugEnabled())
        TRACER.debugInfo("RS " +getMonitorInstanceName()+
            " successfully initialized");
    } catch (ChangelogException e)
    {
      Message message = ERR_COULD_NOT_READ_DB.get(
              getFileForPath(dbDirname).getAbsolutePath(),
              e.getLocalizedMessage());
      logError(message);
    } catch (UnknownHostException e)
    {
      Message message = ERR_UNKNOWN_HOSTNAME.get();
      logError(message);
    } catch (IOException e)
    {
      Message message =
          ERR_COULD_NOT_BIND_CHANGELOG.get(replicationPort, e.getMessage());
      logError(message);
    } catch (DirectoryException e)
    {
      //FIXME:DirectoryException is raised by initializeECL => fix err msg
      Message message = Message.raw(Category.SYNC, Severity.SEVERE_ERROR,
      "Directory Exception raised by ECL initialization: " + e.getMessage());
      logError(message);
    }
  }

  /**
   * Enable the ECL access by creating a dedicated workflow element.
   * @throws DirectoryException when an error occurs.
   */
  public void enableECL() throws DirectoryException
  {
    if (externalChangeLogWorkflowImpl!=null)
    {
      // do nothing if ECL is already enabled
      return;
    }

    // Create the workflow for the base DN and register the workflow with
    // the server.
    externalChangeLogWorkflowImpl = new WorkflowImpl(
        externalChangeLogWorkflowID,
        DN.decode(ServerConstants.DN_EXTERNAL_CHANGELOG_ROOT),
        eclwe.getWorkflowElementID(),
        eclwe);
    externalChangeLogWorkflowImpl.register();

    NetworkGroup defaultNetworkGroup = NetworkGroup.getDefaultNetworkGroup();
    defaultNetworkGroup.registerWorkflow(externalChangeLogWorkflowImpl);

    // FIXME:ECL should the ECL Workflow be registered in adminNetworkGroup?
    NetworkGroup adminNetworkGroup = NetworkGroup.getAdminNetworkGroup();
    adminNetworkGroup.registerWorkflow(externalChangeLogWorkflowImpl);

    // FIXME:ECL should the ECL Workflow be registered in internalNetworkGroup?
    NetworkGroup internalNetworkGroup = NetworkGroup.getInternalNetworkGroup();
    internalNetworkGroup.registerWorkflow(externalChangeLogWorkflowImpl);

    enableECLVirtualAttr("lastexternalchangelogcookie",
        new LastCookieVirtualProvider());
    enableECLVirtualAttr("firstchangenumber",
        new FirstChangeNumberVirtualAttributeProvider());
    enableECLVirtualAttr("lastchangenumber",
        new LastChangeNumberVirtualAttributeProvider());
    enableECLVirtualAttr("changelog",
        new ChangelogBaseDNVirtualAttributeProvider());
  }

  private static void enableECLVirtualAttr(String attrName,
      VirtualAttributeProvider<UserDefinedVirtualAttributeCfg> provider)
  throws DirectoryException
  {
    Set<DN> baseDNs = new HashSet<DN>(0);
    Set<DN> groupDNs = new HashSet<DN>(0);
    Set<SearchFilter> filters = new HashSet<SearchFilter>(0);
    VirtualAttributeCfgDefn.ConflictBehavior conflictBehavior =
      ConflictBehavior.VIRTUAL_OVERRIDES_REAL;

    try
    {

      // To avoid the configuration in cn=config just
      // create a rule and register it into the DirectoryServer
      provider.initializeVirtualAttributeProvider(null);

      AttributeType attributeType = DirectoryServer.getAttributeType(
          attrName, false);

      SearchFilter filter =
        SearchFilter.createFilterFromString("objectclass=*");
      filters.add(filter);

      baseDNs.add(DN.decode(""));
      VirtualAttributeRule rule =
        new VirtualAttributeRule(attributeType, provider,
              baseDNs, SearchScope.BASE_OBJECT,
              groupDNs, filters, conflictBehavior);

      DirectoryServer.registerVirtualAttribute(rule);
    }
    catch (Exception e)
    {
      Message message =
        NOTE_ERR_UNABLE_TO_ENABLE_ECL_VIRTUAL_ATTR.get(attrName, e.toString());
      throw new DirectoryException(ResultCode.OPERATIONS_ERROR, message, e);
    }
  }

  private void shutdownECL()
  {
    WorkflowImpl eclwf = (WorkflowImpl) WorkflowImpl
        .getWorkflow(externalChangeLogWorkflowID);

    // do it only if not already done by another RS (unit test case)
    // if (DirectoryServer.getWorkflowElement(externalChangeLogWorkflowID)
    if (eclwf != null)
    {
      // FIXME:ECL should the ECL Workflow be registered in
      // internalNetworkGroup?
      NetworkGroup internalNetworkGroup = NetworkGroup
          .getInternalNetworkGroup();
      internalNetworkGroup.deregisterWorkflow(externalChangeLogWorkflowID);

      // FIXME:ECL should the ECL Workflow be registered in adminNetworkGroup?
      NetworkGroup adminNetworkGroup = NetworkGroup.getAdminNetworkGroup();
      adminNetworkGroup.deregisterWorkflow(externalChangeLogWorkflowID);

      NetworkGroup defaultNetworkGroup = NetworkGroup.getDefaultNetworkGroup();
      defaultNetworkGroup.deregisterWorkflow(externalChangeLogWorkflowID);

      eclwf.deregister();
      eclwf.finalizeWorkflow();
    }

    eclwe = (ECLWorkflowElement) DirectoryServer
        .getWorkflowElement("EXTERNAL CHANGE LOG");
    if (eclwe != null)
    {
      DirectoryServer.deregisterWorkflowElement(eclwe);
      eclwe.finalizeWorkflowElement();
    }

    shutdownCNIndexDB();
  }

  private void shutdownCNIndexDB()
  {
    synchronized (cnIndexDBLock)
    {
      if (cnIndexDB != null)
      {
        try
        {
          cnIndexDB.shutdown();
        }
        catch (ChangelogException ignored)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.WARNING, ignored);
          }
        }
      }
    }
  }

  /**
   * Get the ReplicationServerDomain associated to the base DN given in
   * parameter.
   *
   * @param baseDn The base Dn for which the ReplicationServerDomain must be
   * returned.
   * @return The ReplicationServerDomain associated to the base DN given in
   *         parameter.
   */
  public ReplicationServerDomain getReplicationServerDomain(String baseDn)
  {
    return getReplicationServerDomain(baseDn, false);
  }

  /**
   * Get the ReplicationServerDomain associated to the base DN given in
   * parameter.
   *
   * @param baseDn The base Dn for which the ReplicationServerDomain must be
   * returned.
   * @param create Specifies whether to create the ReplicationServerDomain if
   *        it does not already exist.
   * @return The ReplicationServerDomain associated to the base DN given in
   *         parameter.
   */
  public ReplicationServerDomain getReplicationServerDomain(String baseDn,
      boolean create)
  {
    synchronized (baseDNs)
    {
      ReplicationServerDomain domain = baseDNs.get(baseDn);
      if (domain == null && create) {
        domain = new ReplicationServerDomain(baseDn, this);
        baseDNs.put(baseDn, domain);
      }
      return domain;
    }
  }

  /**
   * Waits for connections to this ReplicationServer.
   */
  public void waitConnections()
  {
    // Acquire a domain ticket and wait for a complete cycle of the connect
    // thread.
    final long myDomainTicket;
    synchronized (connectThreadLock)
    {
      // Connect thread must be waiting.
      synchronized (domainTicketLock)
      {
        // Determine the ticket which will be used in the next connect thread
        // iteration.
        myDomainTicket = domainTicket + 1;
      }

      // Wake up connect thread.
      connectThreadLock.notify();
    }

    // Wait until the connect thread has processed next connect phase.
    synchronized (domainTicketLock)
    {
      while (myDomainTicket > domainTicket && !shutdown)
      {
        try
        {
          // Wait with timeout so that we detect shutdown.
          domainTicketLock.wait(500);
        }
        catch (InterruptedException e)
        {
          // Can't do anything with this.
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Shutdown the Replication Server service and all its connections.
   */
  public void shutdown()
  {
    localPorts.remove(replicationPort);

    if (shutdown)
      return;

    shutdown = true;

    // shutdown the connect thread
    if (connectThread != null)
    {
      connectThread.interrupt();
    }

    // shutdown the listener thread
    try
    {
      if (listenSocket != null)
      {
        listenSocket.close();
      }
    } catch (IOException e)
    {
      // replication Server service is closing anyway.
    }

    // shutdown the listen thread
    if (listenThread != null)
    {
      listenThread.interrupt();
    }

    // shutdown all the replication domains
    for (ReplicationServerDomain domain : getReplicationServerDomains())
    {
      domain.shutdown();
    }

    shutdownECL();

    if (dbEnv != null)
    {
      dbEnv.shutdown();
    }

    // Remove this instance from the global instance list
    allInstances.remove(this);
  }


  /**
   * Creates a new DB handler for this ReplicationServer and the serverId and DN
   * given in parameter.
   *
   * @param serverId
   *          The serverId for which the dbHandler must be created.
   * @param baseDn
   *          The DN for which the dbHandler must be created.
   * @return The new DB handler for this ReplicationServer and the serverId and
   *         DN given in parameter.
   * @throws ChangelogException
   *           in case of underlying database problem.
   */
  public DbHandler newDbHandler(int serverId, String baseDn)
      throws ChangelogException
  {
    return new DbHandler(serverId, baseDn, this, dbEnv, queueSize);
  }



  /**
   * Clears the generationId for the replicationServerDomain related to the
   * provided baseDn.
   *
   * @param baseDn
   *          The baseDn for which to delete the generationId.
   */
  public void clearGenerationId(String baseDn)
  {
    try
    {
      dbEnv.clearGenerationId(baseDn);
    }
    catch (Exception ignored)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.WARNING, ignored);
      }
    }

    synchronized (cnIndexDBLock)
    {
      if (cnIndexDB != null)
      {
        try
        {
          cnIndexDB.clear(baseDn);
        }
        catch (Exception ignored)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.WARNING, ignored);
          }
        }
      }
    }
  }

  /**
   * Retrieves the time after which changes must be deleted from the
   * persistent storage (in milliseconds).
   *
   * @return  The time after which changes must be deleted from the
   *          persistent storage (in milliseconds).
   */
  public long getTrimAge()
  {
    return purgeDelay * 1000;
  }

  /**
   * Check if the provided configuration is acceptable for add.
   *
   * @param configuration The configuration to check.
   * @param unacceptableReasons When the configuration is not acceptable, this
   *                            table is use to return the reasons why this
   *                            configuration is not acceptable.
   *
   * @return true if the configuration is acceptable, false other wise.
   */
  public static boolean isConfigurationAcceptable(
      ReplicationServerCfg configuration, List<Message> unacceptableReasons)
  {
    int port = configuration.getReplicationPort();

    try
    {
      ServerSocket tmpSocket = new ServerSocket();
      tmpSocket.bind(new InetSocketAddress(port));
      tmpSocket.close();
      return true;
    }
    catch (Exception e)
    {
      Message message = ERR_COULD_NOT_BIND_CHANGELOG.get(port, e.getMessage());
      unacceptableReasons.add(message);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigChangeResult applyConfigurationChange(
      ReplicationServerCfg configuration)
  {
    // Some of those properties change don't need specific code.
    // They will be applied for next connections. Some others have immediate
    // effect

    disconnectRemovedReplicationServers(configuration.getReplicationServer());

    replicationServerUrls = configuration.getReplicationServer();
    if (replicationServerUrls == null)
      replicationServerUrls = new ArrayList<String>();

    queueSize = configuration.getQueueSize();
    long newPurgeDelay = configuration.getReplicationPurgeDelay();
    if (newPurgeDelay != purgeDelay)
    {
      purgeDelay = newPurgeDelay;
      // propagate
      for (ReplicationServerDomain domain : getReplicationServerDomains())
      {
        domain.setPurgeDelay(purgeDelay*1000);
      }
    }

    rcvWindow = configuration.getWindowSize();
    assuredTimeout = configuration.getAssuredTimeout();

    // changing the listen port requires to stop the listen thread
    // and restart it.
    int newPort = configuration.getReplicationPort();
    if (newPort != replicationPort)
    {
      stopListen = true;
      try
      {
        listenSocket.close();
        listenThread.join();
        stopListen = false;

        replicationPort = newPort;
        setServerURL();
        listenSocket = new ServerSocket();
        listenSocket.bind(new InetSocketAddress(replicationPort));

        listenThread = new ReplicationServerListenThread(this);
        listenThread.start();
      }
      catch (IOException e)
      {
        logError(ERR_COULD_NOT_CLOSE_THE_SOCKET.get(e.toString()));
      }
      catch (InterruptedException e)
      {
        logError(ERR_COULD_NOT_STOP_LISTEN_THREAD.get(e.toString()));
      }
    }

    // Update threshold value for status analyzers (stop them if requested
    // value is 0)
    if (degradedStatusThreshold != configuration.getDegradedStatusThreshold())
    {
      degradedStatusThreshold = configuration.getDegradedStatusThreshold();
      for (ReplicationServerDomain domain : getReplicationServerDomains())
      {
        domain.updateDegradedStatusThreshold(degradedStatusThreshold);
      }
    }

    // Update period value for monitoring publishers (stop them if requested
    // value is 0)
    if (monitoringPublisherPeriod != configuration.getMonitoringPeriod())
    {
      monitoringPublisherPeriod = configuration.getMonitoringPeriod();
      for (ReplicationServerDomain domain : getReplicationServerDomains())
      {
        domain.updateMonitoringPeriod(monitoringPublisherPeriod);
      }
    }

    // Changed the group id ?
    byte newGroupId = (byte) configuration.getGroupId();
    if (newGroupId != groupId)
    {
      groupId = newGroupId;
      // Have a new group id: Disconnect every servers.
      for (ReplicationServerDomain domain : getReplicationServerDomains())
      {
        domain.stopAllServers(true);
      }
    }

    // Set a potential new weight
    if (weight != configuration.getWeight())
    {
      weight = configuration.getWeight();
      // Broadcast the new weight the the whole topology. This will make some
      // DSs reconnect (if needed) to other RSs according to the new weight of
      // this RS.
      broadcastConfigChange();
    }

    final String newDir = configuration.getReplicationDBDirectory();
    if (newDir != null && !dbDirname.equals(newDir))
    {
      return new ConfigChangeResult(ResultCode.SUCCESS, true);
    }

    return new ConfigChangeResult(ResultCode.SUCCESS, false);
  }

  /**
   * Try and set a sensible URL for this replication server. Since we are
   * listening on all addresses there are a couple of potential candidates: 1) a
   * matching server url in the replication server's configuration, 2) hostname
   * local address.
   */
  private void setServerURL() throws UnknownHostException
  {
    /*
     * First try the set of configured replication servers to see if one of them
     * is this replication server (this should always be the case).
     */
    for (String rsUrl : replicationServerUrls)
    {
      /*
       * No need validate the string format because the admin framework has
       * already done it.
       */
      final int index = rsUrl.lastIndexOf(":");
      final String hostname = rsUrl.substring(0, index);
      final int port = Integer.parseInt(rsUrl.substring(index + 1));

      if (port == replicationPort && isLocalAddress(hostname))
      {
        serverURL = rsUrl;
        return;
      }
    }

    // Fall-back to the machine hostname.
    serverURL = InetAddress.getLocalHost().getHostName() + ":"
        + replicationPort;
  }

  /**
   * Broadcast a configuration change that just happened to the whole topology
   * by sending a TopologyMsg to every entity in the topology.
   */
  private void broadcastConfigChange()
  {
    for (ReplicationServerDomain domain : getReplicationServerDomains())
    {
      domain.sendTopoInfoToAll();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isConfigurationChangeAcceptable(
      ReplicationServerCfg configuration, List<Message> unacceptableReasons)
  {
    return true;
  }

  /**
   * Get the value of generationId for the replication replicationServerDomain
   * associated with the provided baseDN.
   *
   * @param baseDN The baseDN of the replicationServerDomain.
   * @return The value of the generationID.
   */
  public long getGenerationId(String baseDN)
  {
    ReplicationServerDomain rsd = getReplicationServerDomain(baseDN);
    if (rsd!=null)
      return rsd.getGenerationId();
    return -1;
  }

  /**
   * Get the serverId for this replication server.
   *
   * @return The value of the serverId.
   *
   */
  public int getServerId()
  {
    return serverId;
  }

  /**
   * Get the queueSize for this replication server.
   *
   * @return The maximum size of the queues for this Replication Server
   *
   */
  public int getQueueSize()
  {
    return queueSize;
  }

  /**
   * Creates the backend associated to this replication server.
   * @throws ConfigException
   */
  private void createBackend()
  throws ConfigException
  {
    try
    {
      String ldif = makeLdif(
          "dn: ds-cfg-backend-id="+backendId+",cn=Backends,cn=config",
          "objectClass: top",
          "objectClass: ds-cfg-backend",
          "ds-cfg-base-dn: dc="+backendId,
          "ds-cfg-enabled: true",
          "ds-cfg-writability-mode: enabled",
          "ds-cfg-java-class: " +
            "org.opends.server.replication.server.ReplicationBackend",
          "ds-cfg-backend-id: " + backendId);

      LDIFImportConfig ldifImportConfig = new LDIFImportConfig(
          new StringReader(ldif));
      LDIFReader reader = new LDIFReader(ldifImportConfig);
      Entry backendConfigEntry = reader.readEntry();
      if (!DirectoryServer.getConfigHandler().entryExists(backendConfigEntryDN))
      {
        // Add the replication backend
        DirectoryServer.getConfigHandler().addEntry(backendConfigEntry, null);
      }
      ldifImportConfig.close();
    }
    catch(Exception e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(e.getLocalizedMessage());
      Message msg = ERR_CHECK_CREATE_REPL_BACKEND_FAILED.get(mb.toString());
      throw new ConfigException(msg, e);
    }
  }

  private static String makeLdif(String... lines)
  {
    StringBuilder buffer = new StringBuilder();
    for (String line : lines) {
      buffer.append(line).append(EOL);
    }
    // Append an extra line so we can append LDIF Strings.
    buffer.append(EOL);
    return buffer.toString();
  }

  /**
   * Do what needed when the config object related to this replication server
   * is deleted from the server configuration.
   */
  public void remove()
  {
    if (debugEnabled())
      TRACER.debugInfo("RS " + getMonitorInstanceName() + " starts removing");

    shutdown();
    removeBackend();

    DirectoryServer.deregisterBackupTaskListener(this);
    DirectoryServer.deregisterRestoreTaskListener(this);
    DirectoryServer.deregisterExportTaskListener(this);
    DirectoryServer.deregisterImportTaskListener(this);
  }

  /**
   * Removes the backend associated to this Replication Server that has been
   * created when this replication server was created.
   */
  protected void removeBackend()
  {
    try
    {
      if (DirectoryServer.getConfigHandler().entryExists(backendConfigEntryDN))
      {
        // Delete the replication backend
        DirectoryServer.getConfigHandler().deleteEntry(backendConfigEntryDN,
            null);
      }
    }
    catch(Exception e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(e.getLocalizedMessage());
      Message msg = ERR_DELETE_REPL_BACKEND_FAILED.get(mb.toString());
      logError(msg);
    }
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void processBackupBegin(Backend backend, BackupConfig config)
  {
    // Nothing is needed at the moment
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processBackupEnd(Backend backend, BackupConfig config,
                               boolean successful)
  {
    // Nothing is needed at the moment
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processRestoreBegin(Backend backend, RestoreConfig config)
  {
    if (backend.getBackendID().equals(backendId))
      shutdown();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processRestoreEnd(Backend backend, RestoreConfig config,
                                boolean successful)
  {
    if (backend.getBackendID().equals(backendId))
      initialize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processImportBegin(Backend backend, LDIFImportConfig config)
  {
    // Nothing is needed at the moment
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processImportEnd(Backend backend, LDIFImportConfig config,
                               boolean successful)
  {
    // Nothing is needed at the moment
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processExportBegin(Backend backend, LDIFExportConfig config)
  {
    if (debugEnabled())
      TRACER.debugInfo("RS " + getMonitorInstanceName() + " Export starts");
    if (backend.getBackendID().equals(backendId))
    {
      // Retrieves the backend related to this replicationServerDomain
      ReplicationBackend b =
      (ReplicationBackend)DirectoryServer.getBackend(backendId);
      b.setServer(this);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processExportEnd(Backend backend, LDIFExportConfig config,
                               boolean successful)
  {
    // Nothing is needed at the moment
  }

  /**
   * Returns an iterator on the list of replicationServerDomain.
   * Returns null if none.
   * @return the iterator.
   */
  public Iterator<ReplicationServerDomain> getDomainIterator()
  {
    return getReplicationServerDomains().iterator();
  }

  /**
   * Clears the Db associated with that server.
   */
  public void clearDb()
  {
    for (ReplicationServerDomain rsd : getReplicationServerDomains())
    {
      rsd.clearDbs();
    }

    synchronized (cnIndexDBLock)
    {
      if (cnIndexDB != null)
      {
        try
        {
          cnIndexDB.clear();
        }
        catch (Exception ignored)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.WARNING, ignored);
          }
        }

        shutdownCNIndexDB();

        lastGeneratedChangeNumber = 0;
        cnIndexDB = null;
      }
    }
  }

  /**
   * Get the assured mode timeout.
   * @return The assured mode timeout.
   */
  public long getAssuredTimeout()
  {
    return assuredTimeout;
  }

  /**
   * Get The replication server group id.
   * @return The replication server group id.
   */
  public byte getGroupId()
  {
    return groupId;
  }

  /**
   * Get the threshold value for status analyzer.
   * @return The threshold value for status analyzer.
   */
  public int getDegradedStatusThreshold()
  {
    return degradedStatusThreshold;
  }

  /**
   * Get the monitoring publisher period value.
   * @return the monitoring publisher period value.
   */
  public long getMonitoringPublisherPeriod()
  {
    return monitoringPublisherPeriod;
  }

  /**
   * Compute the list of replication servers that are not any
   * more connected to this Replication Server and stop the
   * corresponding handlers.
   * @param newReplServers the list of the new replication servers configured.
   */
  private void disconnectRemovedReplicationServers(
      Collection<String> newReplServers)
  {
    Collection<String> serversToDisconnect = new ArrayList<String>();

    for (String rsUrl : replicationServerUrls)
    {
      if (!newReplServers.contains(rsUrl))
      {
        try
        {
          // translate the server name into IP address and keep the port number
          String[] host = rsUrl.split(":");
          serversToDisconnect.add(
              InetAddress.getByName(host[0]).getHostAddress() + ":" + host[1]);
        }
        catch (IOException e)
        {
          logError(ERR_COULD_NOT_SOLVE_HOSTNAME.get(rsUrl));
        }
      }
    }

    if (serversToDisconnect.isEmpty())
      return;

    for (ReplicationServerDomain domain: getReplicationServerDomains())
    {
      domain.stopReplicationServers(serversToDisconnect);
    }
  }

  /**
   * Retrieves a printable name for this Replication Server Instance.
   *
   * @return A printable name for this Replication Server Instance.
   */
  public String getMonitorInstanceName()
  {
    return "Replication Server " + replicationPort + " " + serverId;
  }

  /**
   * Retrieves the port used by this ReplicationServer.
   *
   * @return The port used by this ReplicationServer.
   */
  public int getReplicationPort()
  {
    return replicationPort;
  }

  /**
   * Create a new session to get the ECL.
   * @param msg The message that specifies the ECL request.
   * @return Returns the created session.
   * @throws DirectoryException When an error occurs.
   */
  public ExternalChangeLogSession createECLSession(StartECLSessionMsg msg)
  throws DirectoryException
  {
    return new ExternalChangeLogSessionImpl(this, msg);
  }

  /**
   * Getter on the server URL.
   * @return the server URL.
   */
  public String getServerURL()
  {
    return this.serverURL;
  }

  /**
   * WARNING : only use this methods for tests purpose.
   *
   * Add the Replication Server given as a parameter in the list
   * of local replication servers.
   *
   * @param server The server to be added.
   */
  public static void onlyForTestsAddlocalReplicationServer(String server)
  {
    int separator = server.lastIndexOf(':');
    if (separator == -1)
      return ;
    int port = Integer.parseInt(server.substring(separator + 1));
    localPorts.add(port);
  }

  /**
   * WARNING : only use this methods for tests purpose.
   *
   * Clear the list of local Replication Servers
   *
   */
  public static void onlyForTestsClearLocalReplicationServerList()
  {
    localPorts.clear();
  }

  /**
   * Returns {@code true} if the provided port is one of the ports that this
   * replication server is listening on.
   *
   * @param port
   *          The port to be checked.
   * @return {@code true} if the provided port is one of the ports that this
   *         replication server is listening on.
   */
  public static boolean isLocalReplicationServerPort(int port)
  {
    return localPorts.contains(port);
  }

  /**
   * Excluded a list of domain from eligibility computation.
   * @param excludedBaseDNs the provided list of baseDNs excluded from
   *                          the computation of eligibleCSN.
   */
  public void disableEligibility(Set<String> excludedBaseDNs)
  {
    this.excludedBaseDNs = excludedBaseDNs;
  }

  /**
   * Returns the eligible CSN cross domains - relies on the eligible CSN from
   * each domain.
   * @return the cross domain eligible CSN.
   */
  public CSN getEligibleCSN()
  {
    String debugLog = "";

    // traverse the domains and get the eligible CSN from each domain
    // store the oldest one as the cross domain eligible CSN
    CSN eligibleCSN = null;
    for (ReplicationServerDomain domain : getReplicationServerDomains())
    {
      if (contains(excludedBaseDNs, domain.getBaseDn()))
        continue;

      final CSN domainEligibleCSN = domain.getEligibleCSN();
      if (eligibleCSN == null
          ||(domainEligibleCSN != null && domainEligibleCSN.older(eligibleCSN)))
      {
        eligibleCSN = domainEligibleCSN;
      }

      if (debugEnabled())
      {
        final String dates = domainEligibleCSN == null ?
            "" : new Date(domainEligibleCSN.getTime()).toString();
        debugLog += "[baseDN=" + domain.getBaseDn()
            + "] [eligibleCSN=" + domainEligibleCSN + ", " + dates + "]";
      }
    }

    if (eligibleCSN==null )
    {
      eligibleCSN = new CSN(TimeThread.getTime(), 0, 0);
    }

    if (debugEnabled()) {
      TRACER.debugInfo("In " + this + " getEligibleCSN() ends with " +
        " the following domainEligibleCSN for each domain :" + debugLog +
        " thus CrossDomainEligibleCSN=" + eligibleCSN +
        "  ts=" + new Date(eligibleCSN.getTime()).toString());
    }
    return eligibleCSN;
  }

  private boolean contains(Set<String> col, String elem)
  {
    return col != null && col.contains(elem);
  }

  /**
   * Get (or create) a handler on the {@link ChangeNumberIndexDB} for external
   * changelog.
   *
   * @return the handler.
   * @throws DirectoryException
   *           when needed.
   */
  public ChangeNumberIndexDB getChangeNumberIndexDB() throws DirectoryException
  {
    synchronized (cnIndexDBLock)
    {
      try
      {
        if (cnIndexDB == null)
        {
          cnIndexDB = new DraftCNDbHandler(this, this.dbEnv);
          final CNIndexRecord lastCNRecord = cnIndexDB.getLastRecord();
          // initialization of the lastGeneratedChangeNumebr from the DB content
          // if DB is empty => last record does not exist => default to 0
          lastGeneratedChangeNumber =
              (lastCNRecord != null) ? lastCNRecord.getChangeNumber() : 0;
        }
        return cnIndexDB;
      }
      catch (Exception e)
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
        Message message = ERR_CHANGENUMBER_DATABASE.get(e.getMessage());
        throw new DirectoryException(OPERATIONS_ERROR, message, e);
      }
    }
  }

  /**
   * Generate a new change number.
   *
   * @return The generated change number
   */
  public long getNewChangeNumber()
  {
    synchronized (cnIndexDBLock)
    {
      return ++lastGeneratedChangeNumber;
    }
  }

  /**
   * Get first and last change number.
   *
   * @param crossDomainEligibleCSN
   *          The provided crossDomainEligibleCSN used as the upper limit for
   *          the last change number
   * @param excludedBaseDNs
   *          The baseDNs that are excluded from the ECL.
   * @return The first and last change numbers.
   * @throws DirectoryException
   *           When it happens.
   */
  public long[] getECLChangeNumberLimits(CSN crossDomainEligibleCSN,
      Set<String> excludedBaseDNs) throws DirectoryException
  {
    /* The content of the DraftCNdb depends on the SEARCH operations done before
     * requesting the change number. If no operations, DraftCNdb is empty.
     * The limits we want to get are the "potential" limits if a request was
     * done, the DraftCNdb is probably not complete to do that.
     *
     * The first change number is :
     *  - the first record from the DraftCNdb
     *  - if none because DraftCNdb empty,
     *      then
     *        if no change in replchangelog then return 0
     *        else return 1 (change number that WILL be returned to next search)
     *
     * The last change number is :
     *  - initialized with the last record from the DraftCNdb (0 if none)
     *    and consider the genState associated
     *  - to the last change number, we add the count of updates in the
     *     replchangelog FROM that genState TO the crossDomainEligibleCSN
     *     (this diff is done domain by domain)
     */

    final ChangeNumberIndexDB cnIndexDB = getChangeNumberIndexDB();
    try
    {
      boolean dbEmpty = true;
      long firstChangeNumber = 0;
      long lastChangeNumber = 0;

      final CNIndexRecord firstCNRecord = cnIndexDB.getFirstRecord();
      final CNIndexRecord lastCNRecord = cnIndexDB.getLastRecord();

      Map<String, ServerState> domainsServerStateForLastCN = null;
      CSN csnForLastCN = null;
      String domainForLastCN = null;
      if (firstCNRecord != null)
      {
        if (lastCNRecord == null)
        {
          // Edge case: DB was cleaned or closed in between call to getFirst*()
          // and getLast*(). The only remaining solution is to fail fast.
          throw new ChangelogException(
              ERR_READING_FIRST_THEN_LAST_IN_CHANGENUMBER_DATABASE.get());
        }

        dbEmpty = false;
        firstChangeNumber = firstCNRecord.getChangeNumber();
        lastChangeNumber = lastCNRecord.getChangeNumber();

        // Get the generalized state associated with the current last change
        // number and initializes from it the startStates table
        String lastCNGenState = lastCNRecord.getPreviousCookie();
        if (lastCNGenState != null && lastCNGenState.length() > 0)
        {
          domainsServerStateForLastCN = MultiDomainServerState
              .splitGenStateToServerStates(lastCNGenState);
        }

        csnForLastCN = lastCNRecord.getCSN();
        domainForLastCN = lastCNRecord.getBaseDN();
      }

      long newestDate = 0;
      for (ReplicationServerDomain rsd : getReplicationServerDomains())
      {
        if (contains(excludedBaseDNs, rsd.getBaseDn()))
          continue;

        // for this domain, have the state in the replchangelog
        // where the last change number update is
        long ec;
        if (domainsServerStateForLastCN == null)
        {
          // Count changes of this domain from the beginning of the changelog
          CSN trimCSN = new CSN(rsd.getLatestDomainTrimDate(), 0, 0);
          ec = rsd.getEligibleCount(
              rsd.getStartState().duplicateOnlyOlderThan(trimCSN),
              crossDomainEligibleCSN);
        }
        else
        {
          // There are records in the draftDB (so already returned to clients)
          // BUT
          // There is nothing related to this domain in the last draft record
          // (may be this domain was disabled when this record was returned).
          // In that case, are counted the changes from
          // the date of the most recent change from this last draft record
          if (newestDate == 0)
          {
            newestDate = csnForLastCN.getTime();
          }

          // And count changes of this domain from the date of the
          // lastseqnum record (that does not refer to this domain)
          CSN csnx = new CSN(newestDate, csnForLastCN.getSeqnum(), 0);
          ec = rsd.getEligibleCount(csnx, crossDomainEligibleCSN);

          if (domainForLastCN.equalsIgnoreCase(rsd.getBaseDn()))
            ec--;
        }

        // cumulates on domains
        lastChangeNumber += ec;

        // CNIndexDB is empty and there are eligible updates in the replication
        // changelog then init first change number
        if (ec > 0 && firstChangeNumber == 0)
          firstChangeNumber = 1;
      }

      if (dbEmpty)
      {
        // The database was empty, just keep increasing numbers since last time
        // we generated one change number.
        firstChangeNumber += lastGeneratedChangeNumber;
        lastChangeNumber += lastGeneratedChangeNumber;
      }
      return new long[] { firstChangeNumber, lastChangeNumber };
    }
    catch (ChangelogException e)
    {
      throw new DirectoryException(ResultCode.OPERATIONS_ERROR, e);
    }
  }

  /**
   * Returns the last (newest) cookie value.
   * @param excludedBaseDNs The list of baseDNs excluded from ECL.
   * @return the last cookie value.
   */
  public MultiDomainServerState getLastECLCookie(Set<String> excludedBaseDNs)
  {
    disableEligibility(excludedBaseDNs);

    // Initialize start state for all running domains with empty state
    MultiDomainServerState result = new MultiDomainServerState();
    for (ReplicationServerDomain rsd : getReplicationServerDomains())
    {
      if (contains(excludedBaseDNs, rsd.getBaseDn())
          || rsd.getDbServerState().isEmpty())
        continue;

      result.update(rsd.getBaseDn(), rsd.getEligibleState(getEligibleCSN()));
    }
    return result;
  }

  /**
   * Gets the weight.
   * @return the weight
   */
  public int getWeight()
  {
    return weight;
  }



  private Collection<ReplicationServerDomain> getReplicationServerDomains()
  {
    synchronized (baseDNs)
    {
      return new ArrayList<ReplicationServerDomain>(baseDNs.values());
    }
  }

  /**
   * Get the replication server DB directory.
   * This is useful for tests to be able to do some cleanup. Might even be
   * useful for the server some day.
   *
   * @return the Database directory name
   */
  public String getDbDirName()
  {
    return dbDirname;
  }


  private String normalizeServerURL(final String url)
  {
    final int separator = url.lastIndexOf(':');
    final String portString = url.substring(separator + 1);
    final String hostname = url.substring(0, separator);
    try
    {
      final InetAddress inetAddress = InetAddress.getByName(hostname);
      if (isLocalAddress(inetAddress))
      {
        // It does not matter whether we use an IP or hostname here.
        return InetAddress.getLocalHost().getHostAddress() + ":" + portString;
      }
      return inetAddress.getHostAddress() + ":" + portString;
    }
    catch (UnknownHostException e)
    {
      // This should not happen, but if it does then just default to the
      // original URL.
      Message message = ERR_COULD_NOT_SOLVE_HOSTNAME.get(hostname);
      logError(message);

      return url;
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return "RS(" + serverId + ") on " + serverURL + ", domains="
        + baseDNs.keySet();
  }

  /**
   * Initializes the generationId for the specified replication domain.
   *
   * @param baseDn
   *          the replication domain
   * @param generationId
   *          the the generationId value for initialization
   */
  public void initDomainGenerationID(String baseDn, long generationId)
  {
    getReplicationServerDomain(baseDn, true).initGenerationID(generationId);
  }

  /**
   * Adds the specified serverId to the specified replication domain.
   *
   * @param serverId
   *          the server Id to add to the replication domain
   * @param baseDn
   *          the replication domain where to add the serverId
   * @throws ChangelogException
   *           If a database error happened.
   */
  public void addServerIdToDomain(int serverId, String baseDn)
      throws ChangelogException
  {
    DbHandler dbHandler = newDbHandler(serverId, baseDn);
    getReplicationServerDomain(baseDn, true).setDbHandler(serverId, dbHandler);
  }
}
