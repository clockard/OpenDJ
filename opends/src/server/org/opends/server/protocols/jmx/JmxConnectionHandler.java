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
package org.opends.server.protocols.jmx;



import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.messages.ProtocolMessages.*;
import static org.opends.server.util.StaticUtils.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.JMXConnectionHandlerCfg;
import org.opends.server.api.AlertGenerator;
import org.opends.server.api.ClientConnection;
import org.opends.server.api.ConnectionHandler;
import org.opends.server.api.KeyManagerProvider;
import org.opends.server.api.ServerShutdownListener;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.HostPort;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;



/**
 * This class defines a connection handler that will be used for
 * communicating with administrative clients over JMX. The connection
 * handler is responsible for accepting new connections, reading
 * requests from the clients and parsing them as operations. A single
 * request handler should be used.
 */
public final class JmxConnectionHandler extends
    ConnectionHandler<JMXConnectionHandlerCfg> implements
    ServerShutdownListener, AlertGenerator,
    ConfigurationChangeListener<JMXConnectionHandlerCfg> {

  /**
   * Key that may be placed into a JMX connection environment map to
   * provide a custom <code>javax.net.ssl.TrustManager</code> array
   * for a connection.
   */
  public static final String TRUST_MANAGER_ARRAY_KEY =
    "org.opends.server.protocol.jmx.ssl.trust.manager.array";

  // The fully-qualified name of this class.
  private static final String CLASS_NAME =
    "org.opends.server.protocols.jmx.JMXConnectionHandler";

  // The list of active client connection.
  private LinkedList<ClientConnection> connectionList;

  // The current configuration state.
  private JMXConnectionHandlerCfg currentConfig;

  // The JMX RMI Connector associated with the Connection handler.
  private RmiConnector rmiConnector;

  // The unique name for this connection handler.
  private String connectionHandlerName;

  // The protocol used to communicate with clients.
  private String protocol;

  // The set of listeners for this connection handler.
  private LinkedList<HostPort> listeners = new LinkedList<HostPort>();

  /**
   * Creates a new instance of this JMX connection handler. It must be
   * initialized before it may be used.
   */
  public JmxConnectionHandler() {
    super("JMX Connection Handler Thread");

    this.connectionList = new LinkedList<ClientConnection>();
  }



  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(
      JMXConnectionHandlerCfg config) {
    // Create variables to include in the response.
    ResultCode resultCode = ResultCode.SUCCESS;
    ArrayList<String> messages = new ArrayList<String>();

    // Determine whether or not the RMI connection needs restarting.
    boolean rmiConnectorRestart = false;
    boolean portChanged = false;

    if (currentConfig.getListenPort() != config.getListenPort()) {
      rmiConnectorRestart = true;
      portChanged = true;
    }

    if (currentConfig.isUseSSL() != config.isUseSSL()) {
      rmiConnectorRestart = true;
    }

    if (!currentConfig.getSSLCertNickname().equals(
        config.getSSLCertNickname())) {
      rmiConnectorRestart = true;
    }

    // Save the configuration.
    currentConfig = config;

    // Restart the connector if required.
    if (rmiConnectorRestart) {
      if (config.isUseSSL()) {
        protocol = "JMX+SSL";
      } else {
        protocol = "JMX";
      }

      listeners.clear();
      listeners.add(new HostPort(config.getListenPort()));

      rmiConnector.finalizeConnectionHandler(true, portChanged);
      try
      {
        rmiConnector.initialize();
      }
      catch (RuntimeException e)
      {
        resultCode = ResultCode.OPERATIONS_ERROR;
        messages.add(e.getMessage());
      }
    }

    // Return configuration result.
    return new ConfigChangeResult(resultCode, false, messages);
  }



  /**
   * Closes this connection handler so that it will no longer accept
   * new client connections. It may or may not disconnect existing
   * client connections based on the provided flag.
   *
   * @param finalizeReason
   *          The reason that this connection handler should be
   *          finalized.
   * @param closeConnections
   *          Indicates whether any established client connections
   *          associated with the connection handler should also be
   *          closed.
   */
  public void finalizeConnectionHandler(String finalizeReason,
      boolean closeConnections) {
    // Make sure that we don't get notified of any more changes.
    currentConfig.removeJMXChangeListener(this);

    // We should also close the RMI registry.
    rmiConnector.finalizeConnectionHandler(closeConnections, true);
  }



  /**
   * Retrieves information about the set of alerts that this generator
   * may produce. The map returned should be between the notification
   * type for a particular notification and the human-readable
   * description for that notification. This alert generator must not
   * generate any alerts with types that are not contained in this
   * list.
   *
   * @return Information about the set of alerts that this generator
   *         may produce.
   */
  public LinkedHashMap<String, String> getAlerts() {
    LinkedHashMap<String, String> alerts = new LinkedHashMap<String, String>();

    return alerts;
  }



  /**
   * Retrieves the fully-qualified name of the Java class for this
   * alert generator implementation.
   *
   * @return The fully-qualified name of the Java class for this alert
   *         generator implementation.
   */
  public String getClassName() {
    return CLASS_NAME;
  }



  /**
   * Retrieves the set of active client connections that have been
   * established through this connection handler.
   *
   * @return The set of active client connections that have been
   *         established through this connection handler.
   */
  public Collection<ClientConnection> getClientConnections() {
    return connectionList;
  }



  /**
   * Retrieves the DN of the configuration entry with which this alert
   * generator is associated.
   *
   * @return The DN of the configuration entry with which this alert
   *         generator is associated.
   */
  public DN getComponentEntryDN() {
    return currentConfig.dn();
  }



  /**
   * Retrieves the DN of the key manager provider that should be used
   * for operations associated with this connection handler which need
   * access to a key manager.
   *
   * @return The DN of the key manager provider that should be used
   *         for operations associated with this connection handler
   *         which need access to a key manager, or {@code null} if no
   *         key manager provider has been configured for this
   *         connection handler.
   */
  public DN getKeyManagerProviderDN() {
    return currentConfig.getKeyManagerProviderDN();
  }



  /**
   * Get the JMX connection handler's listen port.
   *
   * @return Returns the JMX connection handler's listen port.
   */
  public int getListenPort() {
    return currentConfig.getListenPort();
  }



  /**
   * Get the JMX connection handler's RMI connector.
   *
   * @return Returns the JMX connection handler's RMI connector.
   */
  public RmiConnector getRMIConnector() {
    return rmiConnector;
  }



  /**
   * {@inheritDoc}
   */
  public String getShutdownListenerName() {
    return connectionHandlerName;
  }



  /**
   * Retrieves the nickname of the server certificate that should be
   * used in conjunction with this JMX connection handler.
   *
   * @return The nickname of the server certificate that should be
   *         used in conjunction with this JMX connection handler.
   */
  public String getSSLServerCertNickname() {
    return currentConfig.getSSLCertNickname();
  }



  /**
   * {@inheritDoc}
   */
  public void initializeConnectionHandler(JMXConnectionHandlerCfg config)
         throws ConfigException, InitializationException
  {
    // Validate the key manager provider DN.
    DN keyManagerProviderDN = config.getKeyManagerProviderDN();
    if (keyManagerProviderDN != null) {
      KeyManagerProvider provider = DirectoryServer
          .getKeyManagerProvider(keyManagerProviderDN);
      if (provider == null) {
        int msgID = MSGID_JMX_CONNHANDLER_INVALID_KEYMANAGER_DN;
        String message = getMessage(msgID, String
            .valueOf(config.dn()), String
            .valueOf(keyManagerProviderDN));
        throw new ConfigException(msgID, message);
      }
    }

    // Issue warning if there is not key manager by SSL is enabled.
    if (config.isUseSSL() && keyManagerProviderDN == null) {
      // TODO: give a more useful feedback message.
      logError(ErrorLogCategory.CONFIGURATION,
          ErrorLogSeverity.SEVERE_WARNING,
          MSGID_JMX_CONNHANDLER_CANNOT_DETERMINE_USE_SSL);
      int msgID = MSGID_JMX_CONNHANDLER_CANNOT_DETERMINE_USE_SSL;
      String message = getMessage(msgID,
          String.valueOf(currentConfig.dn()), "");
      throw new ConfigException(msgID, message);
    }

    // Configuration is ok.
    currentConfig = config;


    // Attempt to bind to the listen port to verify whether the connection
    // handler will be able to start.
    ServerSocket s = null;
    try
    {
      s = new ServerSocket();
      s.setReuseAddress(true);
      s.bind(new InetSocketAddress(config.getListenPort()));
    }
    catch (Exception e)
    {
      int    msgID   = MSGID_JMX_CONNHANDLER_CANNOT_BIND;
      String message = getMessage(msgID, String.valueOf(config.dn()),
                                  config.getListenPort(),
                                  getExceptionMessage(e));
      logError(ErrorLogCategory.CONNECTION_HANDLING,
               ErrorLogSeverity.SEVERE_ERROR, message, msgID);
      throw new InitializationException(msgID, message);
    }
    finally
    {
      try
      {
        s.close();
      } catch (Exception e) {}
    }

    if (config.isUseSSL()) {
      protocol = "JMX+SSL";
    } else {
      protocol = "JMX";
    }

    listeners.clear();
    listeners.add(new HostPort("0.0.0.0", config.getListenPort()));
    connectionHandlerName = "JMX Connection Handler " + config.getListenPort();

    // Create the associated RMI Connector.
    rmiConnector = new RmiConnector(DirectoryServer.getJMXMBeanServer(), this);

    // Register this as a change listener.
    config.addJMXChangeListener(this);
  }



  /**
   * {@inheritDoc}
   */
  public String getConnectionHandlerName() {
    return connectionHandlerName;
  }



  /**
   * {@inheritDoc}
   */
  public String getProtocol() {
    return protocol;
  }



  /**
   * {@inheritDoc}
   */
  public Collection<HostPort> getListeners() {
    return listeners;
  }



  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
      JMXConnectionHandlerCfg config,
      List<String> unacceptableReasons) {
    boolean isAcceptable = true;

    //  Validate the key manager provider DN.
    DN keyManagerProviderDN = config.getKeyManagerProviderDN();
    if (keyManagerProviderDN != null) {
      KeyManagerProvider provider = DirectoryServer
          .getKeyManagerProvider(keyManagerProviderDN);
      if (provider == null) {
        int msgID = MSGID_JMX_CONNHANDLER_INVALID_KEYMANAGER_DN;
        unacceptableReasons.add(getMessage(msgID, String
            .valueOf(config.dn()), String
            .valueOf(keyManagerProviderDN)));
        isAcceptable = false;
      }
    }

    if (config.isUseSSL() && keyManagerProviderDN == null) {
      // TODO: give a more useful feedback message.
      int msgID = MSGID_JMX_CONNHANDLER_CANNOT_DETERMINE_USE_SSL;
      unacceptableReasons.add(getMessage(msgID, String.valueOf(config
          .dn()), ""));
      isAcceptable = false;
    }

    return isAcceptable;
  }



  /**
   * Determines whether or not clients are allowed to connect over JMX
   * using SSL.
   *
   * @return Returns <code>true</code> if clients are allowed to
   *         connect over JMX using SSL.
   */
  public boolean isUseSSL() {
    return currentConfig.isUseSSL();
  }



  /**
   * {@inheritDoc}
   */
  public void processServerShutdown(String reason) {
    // We should also close the RMI registry.
    rmiConnector.finalizeConnectionHandler(true, true);
  }



  /**
   * Registers a client connection with this JMX connection handler.
   *
   * @param connection
   *          The client connection.
   */
  public void registerClientConnection(ClientConnection connection) {
    connectionList.add(connection);
  }



  /**
   * {@inheritDoc}
   */
  public void run() {
    try
    {
      rmiConnector.initialize();
    }
    catch (RuntimeException e)
    {
    }
  }



  /**
   * {@inheritDoc}
   */
  public void toString(StringBuilder buffer) {
    buffer.append(connectionHandlerName);
  }
}
