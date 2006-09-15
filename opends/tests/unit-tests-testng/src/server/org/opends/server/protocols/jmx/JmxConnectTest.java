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
package org.opends.server.protocols.jmx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.opends.server.TestCaseUtils;
import org.opends.server.api.ConnectionHandler;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.JMXMBean;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.ResultCode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * A simple test for :
 *  - JMX connection establishment withou using SSL
 *  - JMX get and set
 *  - configuration change
 */
public class JmxConnectTest extends JmxTestCase
{
  /**
   * Set up the environment for performing the tests in this suite.
   *
   * @throws Exception
   *           If the environment could not be set up.
   */
  @BeforeClass
  public void setUp() throws Exception
  {
    // Make sure that the server is up and running.
    TestCaseUtils.startServer();
    synchronized (this)
    {
        this.wait(500);
    }
  }

  /**
   * Build data for the simpleConnect test.
   * 
   * @return the data.
   */
  @DataProvider(name="simpleConnect")
  Object[][] createCredentials()
  {
    return new Object[][] {
        {"cn=directory manager", "password", true},
        {"cn=directory manager", "wrongPassword", false},
        {"cn=wrong user", "password", false},
        {"invalid DN", "password", false},
        {"cn=directory manager", null, false},
        {null, "password", false},
        {null, null, false},
        };
  }

  /**
   * Check that simple (no SSL) connections to the JMX service are
   * accepted when the given
   * credentials are OK and refused when the credentials are invalid.
   *
   */
  @Test(dataProvider="simpleConnect")
  public void simpleConnect(String user, String password,
      boolean expectedResult) throws Exception
  {
    MBeanServerConnection jmxc =
      connect(user, password, TestCaseUtils.getServerJmxPort());

    assertEquals((jmxc != null), expectedResult);
  }

  /**
   * Build some data for the simpleGet test.
   */
  @DataProvider(name="simpleGet")
  Object[][] createNames()
  {
    return new Object[][] {
        {"cn=JMX Connection Handler,cn=Connection Handlers,cn=config",
            "ds-cfg-listen-port", TestCaseUtils.getServerJmxPort()},
        {"cn=JMX Connection Handler,cn=Connection Handlers,cn=config",
              "objectclass", null},
        {"cn=JMX Connection Handler,cn=Connection Handlers,cn=config",
              "ds-cfg-ssl-cert-nickname", "adm-server-cert"},
      // not working at the moment see issue 655        
      //  {"cn=JE Database,ds-cfg-backend-id=userRoot,cn=Backends,cn=config",
      //          "ds-cfg-database-cache-percent", 10},
    };
  }

  /**
   * Test simple JMX get.
   *
   */
  @Test(dataProvider="simpleGet")
  public void simpleGet(String dn, String attributeName, Object value)
     throws Exception
  {
    MBeanServerConnection jmxc =
      connect("cn=directory manager", "password",
              TestCaseUtils.getServerJmxPort());
    assertNotNull(jmxc);

    Object val = jmxGet(dn, attributeName, jmxc);
    
    if (value != null)
    {
      assertEquals(val, value);
    }
    else
    {
      assertTrue(val == null);
    }
  }

  /**
   * Test setting some config attribute through jmx.
   */
  @Test()
  public void simpleSet() throws Exception
  {
    MBeanServerConnection jmxc = connect("cn=directory manager", "password",
        TestCaseUtils.getServerJmxPort());
    assertNotNull(jmxc);
    
    Set<ObjectName> names = jmxc.queryNames(null, null);
    names.clear();
    
    final String dn = "cn=config";
    final String attribute = "ds-cfg-size-limit";
    
    Long val = (Long) jmxGet(dn, attribute, jmxc);
    
    jmxSet(dn, attribute, val + 1, jmxc);
    
    Long newVal = (Long) jmxGet(dn, attribute, jmxc);
    
    assertEquals((long)newVal, (long)val+1);
    
    jmxSet(dn, attribute, val + 1, jmxc);
  }

  /**
   * Test that disabling JMX connection handler does its job by
   *  - opening a JMX connection
   *  - changing the JMX connection handler state to disable
   *  - trying to open a new JMX connection and check that it fails.
   *
   * @throws Exception
   */
  @Test()
  public void disable() throws Exception
  {
    MBeanServerConnection jmxc = connect("cn=directory manager", "password",
        TestCaseUtils.getServerJmxPort());
    assertNotNull(jmxc);
    // This test can not pass at the moment
    // because disabling JMX through JMX is not possible
    // see Issue 620
    // disableJmx(jmxc);
    // JMXConnector jmxcDisabled = connect("cn=directory manager", "password");
    // assertNull(jmxcDisabled);
  }
  
  /**
   * Test changing JMX port through LDAP
   * @throws Exception
   */
  @Test()
  public void changePort() throws Exception
  {
    // Connect to the JMX service and get the current port
    final String dn =
      "cn=JMX Connection Handler,cn=Connection Handlers,cn=config";
    final String attribute = "ds-cfg-listen-port";
    
    MBeanServerConnection jmxc = connect("cn=directory manager", "password",
      TestCaseUtils.getServerJmxPort());
    assertNotNull(jmxc);
    Long initJmxPort = (Long) jmxGet(dn, attribute, jmxc);
    assertNotNull(initJmxPort);
    
    // Get the Jmx connection handler from the core server
    List<ConnectionHandler> handlers = DirectoryServer.getConnectionHandlers();
    assertNotNull(handlers);
    JmxConnectionHandler jmxConnectionHandler = null;
    for (ConnectionHandler handler : handlers)
    {
      if (handler instanceof JmxConnectionHandler)
      {
         jmxConnectionHandler = (JmxConnectionHandler) handler;
        break;
      }
    }
    assertNotNull(jmxConnectionHandler);
    
    // change the configuration of the connection handler to use 
    // the current port + 1 as the new port number
    ConfigEntry config = new ConfigEntry(TestCaseUtils.makeEntry(
        "dn: cn=JMX Connection Handler,cn=Connection Handlers,cn=config",
        "objectClass: top",
        "objectClass: ds-cfg-connection-handler",
        "objectClass: ds-cfg-jmx-connection-handler",
        "ds-cfg-ssl-cert-nickname: adm-server-cert",
        "ds-cfg-connection-handler-class: org.opends.server.protocols.jmx.JmxConnectionHandler",
        "ds-cfg-connection-handler-enabled: true",
        "ds-cfg-use-ssl: false",
        "ds-cfg-listen-port: " + (initJmxPort+1),
        "cn: JMX Connection Handler"
         ), null);
    ArrayList<String> reasons = new ArrayList<String>();
    assertTrue(
        jmxConnectionHandler.hasAcceptableConfiguration(config, reasons));
    ConfigChangeResult configResult =
      jmxConnectionHandler.applyNewConfiguration(config, false);
    assertEquals(configResult.getResultCode(), ResultCode.SUCCESS);
    
    // connect the the JMX service using the new port
    jmxc = connect("cn=directory manager", "password", initJmxPort+1);
    assertNotNull(jmxc);
    Long val = (Long) jmxGet(dn, attribute, jmxc);
    assertEquals((long) val, (long) initJmxPort+1);
    
    // re-establish the initial configuration of the JMX service 
    config = new ConfigEntry(TestCaseUtils.makeEntry(
        "dn: cn=JMX Connection Handler,cn=Connection Handlers,cn=config",
        "objectClass: top",
        "objectClass: ds-cfg-connection-handler",
        "objectClass: ds-cfg-jmx-connection-handler",
        "ds-cfg-ssl-cert-nickname: adm-server-cert",
        "ds-cfg-connection-handler-class: org.opends.server.protocols.jmx.JmxConnectionHandler",
        "ds-cfg-connection-handler-enabled: true",
        "ds-cfg-use-ssl: false",
        "ds-cfg-listen-port: " + initJmxPort,
        "cn: JMX Connection Handler"
         ), null);
    assertTrue(
        jmxConnectionHandler.hasAcceptableConfiguration(config, reasons));
    configResult =
      jmxConnectionHandler.applyNewConfiguration(config, false);
    assertEquals(configResult.getResultCode(), ResultCode.SUCCESS);
  }
  
  
  /**
   * Connect to the JMX service.
   */
  private MBeanServerConnection connect(
      String user, String password, long jmxPort)
      throws MalformedURLException, IOException
  {
    HashMap<String, String[]> env = new HashMap<String, String[]>();

    // Provide the credentials required by the server to successfully
    // perform user authentication
    //
    String[] credentials;
    if ((user == null) && (password == null))
    {
      credentials = null;
    }
    else
      credentials = new String[] { user , password };
    env.put("jmx.remote.credentials", credentials);

    // Create an RMI connector client and
    // connect it to the RMI connector server
    //

    JMXServiceURL url = new JMXServiceURL(
      "service:jmx:rmi:///jndi/rmi://localhost:"+ jmxPort +
          "/org.opends.server.protocols.jmx.client-unknown");

    JMXConnector jmxc = null;
    try
    {
      jmxc = JMXConnectorFactory.connect(url, env);
      return jmxc.getMBeanServerConnection();
    } catch (SecurityException e)
    {
      return null;
    }
   
  }

  /**
   * disable the JMX front-end thorugh JMX operation.

  private void disableJmx(JMXConnector jmxc)
     throws Exception
  {
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    // Get status of the JMX connection handler
    String jmxName = JMXMBean.getJmxName(
      DN.decode("cn=JMX Connection Handler,cn=Connection Handlers,cn=config"));
    ObjectName name = ObjectName.getInstance(jmxName);
    Attribute status = (Attribute) mbsc.getAttribute(name,
                      "ds-cfg-connection-handler-enabled");
    if (status != null)
      status.getValue();
    Attribute attr = new Attribute("ds-cfg-connection-handler-enabled", false);
    mbsc.setAttribute(name, attr);
    status = (Attribute) mbsc.getAttribute(name,
         "ds-cfg-connection-handler-enabled");

    status = null;
  }
  */

  /**
   * Get an attrbiue value through JMX.
   */
  private Object jmxGet(String dn, String attributeName,
                        MBeanServerConnection mbsc)
    throws Exception
  {
    String jmxName = JMXMBean.getJmxName(DN.decode(dn));
    ObjectName name = ObjectName.getInstance(jmxName);
    Attribute status = (Attribute) mbsc.getAttribute(name, attributeName);
    if (status == null)
      return null;
    else
      return status.getValue();
  }

  /**
   * Set an attrbiue value through JMX.
   */
  private void jmxSet(String dn, String attributeName,
                      Object value, MBeanServerConnection mbsc)
        throws Exception
  {
    String jmxName = JMXMBean.getJmxName(DN.decode(dn));
    ObjectName name = ObjectName.getInstance(jmxName);
    Attribute attr = new Attribute(attributeName, value);
   
    mbsc.setAttribute(name, attr);
  }
}
