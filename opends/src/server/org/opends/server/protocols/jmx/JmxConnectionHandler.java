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
 *      Copyright 2006-2009 Sun Microsystems, Inc.
 *      Portions copyright 2013-2014 ForgeRock AS
 */
package org.opends.server.protocols.jmx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opends.messages.Message;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.ConnectionHandlerCfg;
import org.opends.server.admin.std.server.JMXConnectionHandlerCfg;
import org.opends.server.api.ClientConnection;
import org.opends.server.api.ConnectionHandler;
import org.opends.server.api.ServerShutdownListener;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.*;
import org.opends.server.util.StaticUtils;

import static org.opends.messages.ProtocolMessages.*;
import static org.opends.server.loggers.ErrorLogger.*;
import static org.opends.server.types.HostPort.*;
import static org.opends.server.util.StaticUtils.*;

/**
 * This class defines a connection handler that will be used for
 * communicating with administrative clients over JMX. The connection
 * handler is responsible for accepting new connections, reading
 * requests from the clients and parsing them as operations. A single
 * request handler should be used.
 */
public final class JmxConnectionHandler extends
    ConnectionHandler<JMXConnectionHandlerCfg> implements
    ServerShutdownListener,
    ConfigurationChangeListener<JMXConnectionHandlerCfg> {

  /**
   * Key that may be placed into a JMX connection environment map to
   * provide a custom {@code javax.net.ssl.TrustManager} array
   * for a connection.
   */
  public static final String TRUST_MANAGER_ARRAY_KEY =
    "org.opends.server.protocol.jmx.ssl.trust.manager.array";

  /** The fully-qualified name of this class. */
  private static final String CLASS_NAME =
    "org.opends.server.protocols.jmx.JMXConnectionHandler";

  /** The list of active client connection. */
  private final List<ClientConnection> connectionList;

  /** The current configuration state. */
  private JMXConnectionHandlerCfg currentConfig;

  /** The JMX RMI Connector associated with the Connection handler. */
  private RmiConnector rmiConnector;

  /** The unique name for this connection handler. */
  private String connectionHandlerName;

  /** The protocol used to communicate with clients. */
  private String protocol;

  /** The set of listeners for this connection handler. */
  private final List<HostPort> listeners = new LinkedList<HostPort>();

  /**
   * Creates a new instance of this JMX connection handler. It must be
   * initialized before it may be used.
   */
  public JmxConnectionHandler() {
    super("JMX Connection Handler Thread");

    this.connectionList = new CopyOnWriteArrayList<ClientConnection>();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigChangeResult applyConfigurationChange(
      JMXConnectionHandlerCfg config) {
    // Create variables to include in the response.
    ResultCode resultCode = ResultCode.SUCCESS;
    final List<Message> messages = new ArrayList<Message>();

    // Determine whether or not the RMI connection needs restarting.
    boolean rmiConnectorRestart = false;
    boolean portChanged = false;

    if (currentConfig.getListenPort() != config.getListenPort()) {
      rmiConnectorRestart = true;
      portChanged = true;
    }

    if (currentConfig.getRmiPort() != config.getRmiPort())
    {
      rmiConnectorRestart = true;
    }
    if (currentConfig.isUseSSL() != config.isUseSSL()) {
      rmiConnectorRestart = true;
    }

    if (((currentConfig.getSSLCertNickname() != null) &&
          !currentConfig.getSSLCertNickname().equals(
          config.getSSLCertNickname())) ||
        ((config.getSSLCertNickname() != null) &&
          !config.getSSLCertNickname().equals(
          currentConfig.getSSLCertNickname()))) {
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
      listeners.add(HostPort.allAddresses(config.getListenPort()));

      rmiConnector.finalizeConnectionHandler(portChanged);
      try
      {
        rmiConnector.initialize();
      }
      catch (RuntimeException e)
      {
        resultCode = DirectoryServer.getServerErrorResultCode();
        messages.add(Message.raw(e.getMessage()));
      }
    }

    // If the port number has changed then update the JMX port information
    // stored in the system properties.
    if (portChanged)
    {
      String key = protocol + "_port";
      String value = String.valueOf(config.getListenPort());
      System.clearProperty(key);
      System.setProperty(key, value);
    }

    // Return configuration result.
    return new ConfigChangeResult(resultCode, false, messages);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeConnectionHandler(Message finalizeReason) {
    // Make sure that we don't get notified of any more changes.
    currentConfig.removeJMXChangeListener(this);

    // We should also close the RMI registry.
    rmiConnector.finalizeConnectionHandler(true);
  }


  /**
   * Retrieves the set of active client connections that have been
   * established through this connection handler.
   *
   * @return The set of active client connections that have been
   *         established through this connection handler.
   */
  @Override
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
  @Override
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
   * Get the JMX connection handler's rmi port.
   *
   * @return Returns the JMX connection handler's rmi port.
   */
  public int getRmiPort() {
    return currentConfig.getRmiPort();
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
  @Override
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
  @Override
  public void initializeConnectionHandler(JMXConnectionHandlerCfg config)
         throws ConfigException, InitializationException
  {
    // Configuration is ok.
    currentConfig = config;

    final List<Message> reasons = new LinkedList<Message>();
    if (!isPortConfigurationAcceptable(String.valueOf(config.dn()),
        config.getListenPort(), reasons))
    {
      Message message = reasons.get(0);
      logError(message);
      throw new InitializationException(message);
    }

    if (config.isUseSSL()) {
      protocol = "JMX+SSL";
    } else {
      protocol = "JMX";
    }

    listeners.clear();
    listeners.add(HostPort.allAddresses(config.getListenPort()));
    connectionHandlerName = "JMX Connection Handler " + config.getListenPort();

    // Create a system property to store the JMX port the server is
    // listening to. This information can be displayed with jinfo.
    System.setProperty(
      protocol + "_port", String.valueOf(config.getListenPort()));

    // Create the associated RMI Connector.
    rmiConnector = new RmiConnector(DirectoryServer.getJMXMBeanServer(), this);

    // Register this as a change listener.
    config.addJMXChangeListener(this);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getConnectionHandlerName() {
    return connectionHandlerName;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getProtocol() {
    return protocol;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<HostPort> getListeners() {
    return listeners;
  }


  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean isConfigurationAcceptable(ConnectionHandlerCfg configuration,
                                           List<Message> unacceptableReasons)
  {
    JMXConnectionHandlerCfg config = (JMXConnectionHandlerCfg) configuration;

    if ((currentConfig == null ||
        (!currentConfig.isEnabled() && config.isEnabled()) ||
        currentConfig.getListenPort() != config.getListenPort()) &&
        !isPortConfigurationAcceptable(String.valueOf(config.dn()),
          config.getListenPort(), unacceptableReasons))
    {
      return false;
    }

    if (config.getRmiPort() != 0 &&
        (currentConfig == null ||
        (!currentConfig.isEnabled() && config.isEnabled()) ||
        currentConfig.getRmiPort() != config.getRmiPort()) &&
        !isPortConfigurationAcceptable(String.valueOf(config.dn()),
          config.getRmiPort(), unacceptableReasons))
    {
      return false;
    }

    return isConfigurationChangeAcceptable(config, unacceptableReasons);
  }

  /**
   * Attempt to bind to the port to verify whether the connection
   * handler will be able to start.
   * @return true is the port is free to use, false otherwise.
   */
  private boolean isPortConfigurationAcceptable(String configDN,
                      int newPort, List<Message> unacceptableReasons) {
    try {
      if (StaticUtils.isAddressInUse(
          new InetSocketAddress(newPort).getAddress(), newPort, true)) {
        throw new IOException(ERR_CONNHANDLER_ADDRESS_INUSE.get().toString());
      }
    } catch (Exception e) {
      Message message = ERR_CONNHANDLER_CANNOT_BIND.get("JMX", configDN,
              WILDCARD_ADDRESS, newPort, getExceptionMessage(e));
      unacceptableReasons.add(message);
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isConfigurationChangeAcceptable(
      JMXConnectionHandlerCfg config,
      List<Message> unacceptableReasons) {
    // All validation is performed by the admin framework.
    return true;
  }



  /**
   * Determines whether or not clients are allowed to connect over JMX
   * using SSL.
   *
   * @return Returns {@code true} if clients are allowed to
   *         connect over JMX using SSL.
   */
  public boolean isUseSSL() {
    return currentConfig.isUseSSL();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void processServerShutdown(Message reason) {
    // We should also close the RMI registry.
    rmiConnector.finalizeConnectionHandler(true);
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
   * Unregisters a client connection from this JMX connection handler.
   *
   * @param connection
   *          The client connection.
   */
  public void unregisterClientConnection(ClientConnection connection) {
    connectionList.remove(connection);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    try
    {
      rmiConnector.initialize();
    }
    catch (RuntimeException ignore)
    {
      // Already caught and logged
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void toString(StringBuilder buffer) {
    buffer.append(connectionHandlerName);
  }
}
