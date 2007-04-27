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
package org.opends.server.replication;

import static org.opends.server.loggers.Error.logError;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.opends.server.DirectoryServerTestCase;
import org.opends.server.TestCaseUtils;
import org.opends.server.replication.common.ServerState;
import org.opends.server.replication.plugin.ReplicationBroker;
import org.opends.server.replication.plugin.PersistentServerState;
import org.opends.server.schema.IntegerSyntax;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.ldap.LDAPFilter;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.ByteStringFactory;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.SearchScope;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.AttributeType;
import org.opends.server.types.LockManager;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeValue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.testng.annotations.BeforeClass;

/**
 * An abstract class that all Replication unit test should extend.
 */
@Test(groups = { "precommit", "replication" })
public abstract class ReplicationTestCase extends DirectoryServerTestCase
{

  /**
  * The internal connection used for operation
  */
  protected InternalClientConnection connection;

  /**
   * Created entries that need to be deleted for cleanup
   */
  protected LinkedList<DN> entryList = new LinkedList<DN>();
  protected LinkedList<DN> configEntryList = new LinkedList<DN>();

  protected Entry synchroServerEntry;

  protected Entry replServerEntry;

  /**
   * schema check flag
   */
  protected boolean schemaCheck;

  /**
   * The replication plugin entry
   */
  protected String synchroPluginStringDN =
    "cn=Multimaster Synchronization, cn=Synchronization Providers,cn=config";

  /**
   * Set up the environment for performing the tests in this suite.
   *
   * @throws Exception
   *         If the environment could not be set up.
   */
  @BeforeClass
  public void setUp() throws Exception
  {
    // This test suite depends on having the schema available.
    TestCaseUtils.startServer();
    schemaCheck = DirectoryServer.checkSchema();

    // Create an internal connection
    connection = InternalClientConnection.getRootConnection();
  }

  /**
   * Open a replicationServer session to the local ReplicationServer.
   *
   */
  protected ReplicationBroker openReplicationSession(
      final DN baseDn, short serverId, int window_size,
      int port, int timeout, boolean emptyOldChanges)
          throws Exception, SocketException
  {
    ServerState state;
    if (emptyOldChanges)
       state = new PersistentServerState(baseDn);
    else
       state = new ServerState();

    ReplicationBroker broker = new ReplicationBroker(
        state, baseDn, serverId, 0, 0, 0, 0, window_size, 0);
    ArrayList<String> servers = new ArrayList<String>(1);
    servers.add("localhost:" + port);
    broker.start(servers);
    if (timeout != 0)
      broker.setSoTimeout(timeout);
    TestCaseUtils.sleep(100); // give some time to the broker to connect
                              // to the replicationServer.
    if (emptyOldChanges)
    {
      /*
       * loop receiving update until there is nothing left
       * to make sure that message from previous tests have been consumed.
       */
      try
      {
        while (true)
        {
          broker.receive();
        }
      }
      catch (Exception e)
      { 
        logError(ErrorLogCategory.SYNCHRONIZATION,
            ErrorLogSeverity.NOTICE,
            "ReplicationTestCase/openChangelogSession" + e.getMessage(), 1);
      }
    }
    return broker;
  }

  /**
   * Open a new session to the ReplicationServer
   * starting with a given ServerState.
   */
  protected ReplicationBroker openReplicationSession(
      final DN baseDn, short serverId, int window_size,
      int port, int timeout, ServerState state)
          throws Exception, SocketException
  {
    ReplicationBroker broker = new ReplicationBroker(
        state, baseDn, serverId, 0, 0, 0, 0, window_size, 0);
    ArrayList<String> servers = new ArrayList<String>(1);
    servers.add("localhost:" + port);
    broker.start(servers);
    if (timeout != 0)
      broker.setSoTimeout(timeout);

    return broker;
  }

  /**
   * Open a replicationServer session with flow control to the local
   * ReplicationServer.
   *
   */
  protected ReplicationBroker openReplicationSession(
      final DN baseDn, short serverId, int window_size,
      int port, int timeout, int maxSendQueue, int maxRcvQueue,
      boolean emptyOldChanges)
          throws Exception, SocketException
  {
    ServerState state;
    if (emptyOldChanges)
       state = new PersistentServerState(baseDn);
    else
       state = new ServerState();

    ReplicationBroker broker = new ReplicationBroker(
        state, baseDn, serverId, maxRcvQueue, 0,
        maxSendQueue, 0, window_size, 0);
    ArrayList<String> servers = new ArrayList<String>(1);
    servers.add("localhost:" + port);
    broker.start(servers);
    if (timeout != 0)
      broker.setSoTimeout(timeout);
    if (emptyOldChanges)
    {
      /*
       * loop receiving update until there is nothing left
       * to make sure that message from previous tests have been consumed.
       */
      try
      {
        while (true)
        {
          broker.receive();
        }
      }
      catch (Exception e)
      { }
    }
    return broker;
  }

  /**
   * suppress all the config entries created by the tests in this class
   */
  protected void cleanConfigEntries()
  {
    logError(ErrorLogCategory.SYNCHRONIZATION,
        ErrorLogSeverity.NOTICE,
        "ReplicationTestCase/Cleaning config entries" , 1);

    DeleteOperation op;
    // Delete entries
    try
    {
      while (true)
      {
        DN dn = configEntryList.removeLast();
             logError(ErrorLogCategory.SYNCHRONIZATION,
            ErrorLogSeverity.NOTICE,
            "cleaning config entry " + dn, 1);
        
        op = new DeleteOperation(connection, InternalClientConnection
            .nextOperationID(), InternalClientConnection.nextMessageID(), null,
            dn);

        op.run();
      }
    }
    catch (NoSuchElementException e) {
      // done
    }
  }
  
  /**
   * suppress all the real entries created by the tests in this class
   */
  protected void cleanRealEntries()
  {
  	logError(ErrorLogCategory.SYNCHRONIZATION,
        ErrorLogSeverity.NOTICE,
        "ReplicationTestCase/Cleaning entries" , 1);
  
    DeleteOperation op;
    // Delete entries
    try
    {
      while (true)
      {
        DN dn = entryList.removeLast();
        logError(ErrorLogCategory.SYNCHRONIZATION,
            ErrorLogSeverity.NOTICE,
            "cleaning entry " + dn, 1);

        op = new DeleteOperation(connection, InternalClientConnection
            .nextOperationID(), InternalClientConnection.nextMessageID(), null,
            dn);

        op.run();
      }
    }
    catch (NoSuchElementException e) {
      // done
    }
  }

  /**
   * Clean up the environment. return null;
   *
   * @throws Exception
   *           If the environment could not be set up.
   */
  @AfterClass
  public void classCleanUp() throws Exception
  {
    DirectoryServer.setCheckSchema(schemaCheck);

    cleanConfigEntries();
    cleanRealEntries();
  }

  /**
   * Configure the replication for this test.
   */
  protected void configureReplication() throws Exception
  {
    // Add the Multimaster replication plugin
    String synchroPluginLdif = "dn: " + synchroPluginStringDN + "\n"
         + "objectClass: top\n"
         + "objectClass: ds-cfg-synchronization-provider\n"
         + "objectClass: ds-cfg-multimaster-synchronization-provider\n"
         + "ds-cfg-synchronization-provider-enabled: true\n"
         + "ds-cfg-synchronization-provider-class: " +
         "org.opends.server.replication.plugin.MultimasterReplication\n";
    Entry synchroPluginEntry = TestCaseUtils.entryFromLdifString(synchroPluginLdif);
    DirectoryServer.getConfigHandler().addEntry(synchroPluginEntry, null);
    configEntryList.add(synchroPluginEntry.getDN());
    assertNotNull(DirectoryServer.getConfigEntry(DN
        .decode(synchroPluginStringDN)),
        "Unable to add the Multimaster replication plugin");
    
    // domains container entry.
    String domainsLdif = "dn: "
      + "cn=domains," + synchroPluginStringDN + "\n"
      + "objectClass: top\n"
      + "objectClass: ds-cfg-branch\n";
    Entry domainsEntry = TestCaseUtils.entryFromLdifString(domainsLdif);
    DirectoryServer.getConfigHandler().addEntry(domainsEntry, null);
    configEntryList.add(domainsEntry.getDN());
    assertNotNull(DirectoryServer.getConfigEntry(
      DN.decode(synchroPluginStringDN)),
      "Unable to add the Multimaster replication plugin");
      

    // Add the replication server
    DirectoryServer.getConfigHandler().addEntry(replServerEntry, null);
    assertNotNull(DirectoryServer.getConfigEntry(replServerEntry.getDN()),
       "Unable to add the replication server");
    configEntryList.add(replServerEntry.getDN());

    // We also have a replicated suffix (replication domain)
    DirectoryServer.getConfigHandler().addEntry(synchroServerEntry, null);
    assertNotNull(DirectoryServer.getConfigEntry(synchroServerEntry.getDN()),
        "Unable to add the synchronized server");
    configEntryList.add(synchroServerEntry.getDN());
  }

  /**
   * Retrieve the number of replayed updates for a given replication
   * domain from the monitor entry.
   * @return The number of replayed updates.
   * @throws Exception If an error occurs.
   */
  protected long getReplayedUpdatesCount(DN syncDN) throws Exception
  {
    String monitorFilter =
         "(&(cn=synchronization*)(base-dn=" + syncDN + "))";

    InternalSearchOperation op;
    op = connection.processSearch(
         ByteStringFactory.create("cn=monitor"),
         SearchScope.SINGLE_LEVEL,
         LDAPFilter.decode(monitorFilter));
    SearchResultEntry entry = op.getSearchEntries().getFirst();

    AttributeType attrType =
         DirectoryServer.getDefaultAttributeType("replayed-updates");
    return entry.getAttributeValue(attrType, IntegerSyntax.DECODER).longValue();
  }

  /**
   * Check that the entry with the given dn has the given valueString value
   * for the given attrTypeStr attribute type.
   */
  protected boolean checkEntryHasAttribute(DN dn, String attrTypeStr,
      String valueString, int timeout, boolean hasAttribute) throws Exception
  {
    boolean found;
    int count = timeout/100;
    if (count<1)
      count=1;

    do
    {
      Entry newEntry;
      Lock lock = null;
      for (int j=0; j < 3; j++)
      {
        lock = LockManager.lockRead(dn);
        if (lock != null)
        {
          break;
        }
      }

      if (lock == null)
      {
        throw new Exception("could not lock entry " + dn);
      }

      try
      {
        newEntry = DirectoryServer.getEntry(dn);


        if (newEntry == null)
          fail("The entry " + dn +
          " has incorrectly been deleted from the database.");
        List<Attribute> tmpAttrList = newEntry.getAttribute(attrTypeStr);
        Attribute tmpAttr = tmpAttrList.get(0);

        AttributeType attrType =
          DirectoryServer.getAttributeType(attrTypeStr, true);
        found = tmpAttr.hasValue(new AttributeValue(attrType, valueString));

      }
      finally
      {
        LockManager.unlock(dn, lock);
      }

      if (found != hasAttribute)
        Thread.sleep(100);
    } while ((--count > 0) && (found != hasAttribute));
    return found;
  }

  /**
   * Retrieves an entry from the local Directory Server.
   * @throws Exception When the entry cannot be locked.
   */
  protected Entry getEntry(DN dn, int timeout, boolean exist) throws Exception
  {
    int count = timeout/200;
    if (count<1)
      count=1;
    Thread.sleep(50);
    boolean found = DirectoryServer.entryExists(dn);
    while ((count> 0) && (found != exist))
    {
      Thread.sleep(200);

      found = DirectoryServer.entryExists(dn);
      count--;
    }

    Lock lock = null;
    for (int i=0; i < 3; i++)
    {
      lock = LockManager.lockRead(dn);
      if (lock != null)
      {
        break;
      }
    }

    if (lock == null)
    {
      throw new Exception("could not lock entry " + dn);
    }

    try
    {
      Entry entry = DirectoryServer.getEntry(dn);
      if (entry == null)
        return null;
      else
        return entry.duplicate(true);
    }
    finally
    {
      LockManager.unlock(dn, lock);
    }
  }

}
