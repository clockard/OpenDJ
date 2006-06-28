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
package org.opends.server.protocols.ldap;



import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.ReentrantLock;

import org.opends.server.api.MonitorProvider;
import org.opends.server.core.DirectoryServer;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.ProtocolMessages.*;
import static org.opends.server.protocols.ldap.LDAPConstants.*;



/**
 * This class defines a data structure that will be used to keep track of
 * various metrics related to LDAP communication that the server has conducted.
 * The statistics that will be tracked include:
 *
 * <UL>
 *   <LI>The total number of LDAP client connections accepted by the
 *       server.</LI>
 *   <LI>The total number of LDAP client connections that have been closed.</LI>
 *   <LI>The total number of LDAP messages read, both overall and broken down
 *       by message type.</LI>
 *   <LI>The total number of LDAP messages written, both overall and broken down
 *       by message type.</LI>
 *   <LI>The total number of bytes read from LDAP clients.</LI>
 *   <LI>The total number of bytes written to LDAP clients.</LI>
 * </UL>
 *
 * <BR><BR>
 * This class may also be used in a hierarchical form if it is desirable to
 * get specific and general statistics at the same time (e.g., information
 * about the interaction with a specific client or aggregated for all clients).
 */
public class LDAPStatistics
       extends MonitorProvider
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.protocols.ldap.LDAPStatistics";



  // The statistics maintained by this class.
  private long abandonRequests;
  private long addRequests;
  private long addResponses;
  private long bindRequests;
  private long bindResponses;
  private long bytesRead;
  private long bytesWritten;
  private long compareRequests;
  private long compareResponses;
  private long connectionsClosed;
  private long connectionsEstablished;
  private long deleteRequests;
  private long deleteResponses;
  private long extendedRequests;
  private long extendedResponses;
  private long messagesRead;
  private long messagesWritten;
  private long modifyRequests;
  private long modifyResponses;
  private long modifyDNRequests;
  private long modifyDNResponses;
  private long operationsAbandoned;
  private long operationsCompleted;
  private long operationsInitiated;
  private long searchRequests;
  private long searchResultEntries;
  private long searchResultReferences;
  private long searchResultsDone;
  private long unbindRequests;

  // The parent that should also be updated whenever statistics in this object
  // are updated.
  private LDAPStatistics parent;

  // The locks used to provide threadsafe access to this class.  In this case,
  // read and write refer to the type of LDAP communication, not access to the
  // protected data.
  private ReentrantLock abandonLock;
  private ReentrantLock connectLock;
  private ReentrantLock disconnectLock;
  private ReentrantLock readLock;
  private ReentrantLock writeLock;

  // The instance name for this monitor provider instance.
  private String instanceName;




  /**
   * Creates a new instance of this class with no parent.
   *
   * @param  instanceName  The name for this monitor provider instance.
   */
  public LDAPStatistics(String instanceName)
  {
    this(instanceName, null);

    DirectoryServer.registerMonitorProvider(this);
  }



  /**
   * Creates a new instance of this class with the specified parent.
   *
   * @param  instanceName  The name for this monitor provider instance.
   * @param  parent        The parent object that should also be updated
   *                       whenever this class is updated.  It may be null if
   *                       there should not be a parent.
   */
  public LDAPStatistics(String instanceName, LDAPStatistics parent)
  {
    super("LDAP Statistics Monitor Provider");

    assert debugConstructor(CLASS_NAME, String.valueOf(parent));

    this.instanceName = instanceName;
    this.parent       = parent;

    abandonLock    = new ReentrantLock();
    connectLock    = new ReentrantLock();
    disconnectLock = new ReentrantLock();
    readLock       = new ReentrantLock();
    writeLock      = new ReentrantLock();

    abandonRequests        = 0;
    addRequests            = 0;
    addResponses           = 0;
    bindRequests           = 0;
    bindResponses          = 0;
    bytesRead              = 0;
    bytesWritten           = 0;
    compareRequests        = 0;
    compareResponses       = 0;
    connectionsClosed      = 0;
    connectionsEstablished = 0;
    deleteRequests         = 0;
    deleteResponses        = 0;
    extendedRequests       = 0;
    extendedResponses      = 0;
    messagesRead           = 0;
    messagesWritten        = 0;
    modifyRequests         = 0;
    modifyResponses        = 0;
    modifyDNRequests       = 0;
    modifyDNResponses      = 0;
    operationsAbandoned    = 0;
    operationsCompleted    = 0;
    operationsInitiated    = 0;
    searchRequests         = 0;
    searchResultEntries    = 0;
    searchResultReferences = 0;
    searchResultsDone      = 0;
    unbindRequests         = 0;
  }



  /**
   * Initializes this monitor provider based on the information in the provided
   * configuration entry.
   *
   * @param  configEntry  The configuration entry that contains the information
   *                      to use to initialize this monitor provider.
   *
   * @throws  ConfigException  If an unrecoverable problem arises in the
   *                           process of performing the initialization.
   */
  public void initializeMonitorProvider(ConfigEntry configEntry)
         throws ConfigException
  {
    assert debugEnter(CLASS_NAME, "initializeMonitorProvider");

    // Throw an exception, because this monitor is not intended to be
    // dynamically loaded from the configuration.  Rather, it should be
    // explicitly created and registered by the LDAP connection handler or an
    // LDAP client connection.
    int    msgID   = MSGID_LDAP_STATS_INVALID_MONITOR_INITIALIZATION;
    String message = getMessage(msgID, String.valueOf(configEntry.getDN()));
    throw new ConfigException(msgID, message);
  }



  /**
   * Retrieves the name of this monitor provider.  It should be unique among all
   * monitor providers, including all instances of the same monitor provider.
   *
   * @return  The name of this monitor provider.
   */
  public String getMonitorInstanceName()
  {
    assert debugEnter(CLASS_NAME, "getMonitorInstanceName");

    return instanceName;
  }



  /**
   * Retrieves the length of time in milliseconds that should elapse between
   * calls to the <CODE>updateMonitorData()</CODE> method.  A negative or zero
   * return value indicates that the <CODE>updateMonitorData()</CODE> method
   * should not be periodically invoked.
   *
   * @return  The length of time in milliseconds that should elapse between
   *          calls to the <CODE>updateMonitorData()</CODE> method.
   */
  public long getUpdateInterval()
  {
    assert debugEnter(CLASS_NAME, "getUpdateInterval");


    // This monitor should not run periodically.
    return -1;
  }



  /**
   * Performs any processing periodic processing that may be desired to update
   * the information associated with this monitor.  Note that best-effort
   * attempts will be made to ensure that calls to this method come
   * <CODE>getUpdateInterval()</CODE> milliseconds apart, but no guarantees will
   * be made.
   */
  public void updateMonitorData()
  {
    assert debugEnter(CLASS_NAME, "updateMonitorData");

    // No implementation is required since this does not do periodic updates.
  }



  /**
   * Retrieves a set of attributes containing monitor data that should be
   * returned to the client if the corresponding monitor entry is requested.
   *
   * @return  A set of attributes containing monitor data that should be
   *          returned to the client if the corresponding monitor entry is
   *          requested.
   */
  public ArrayList<Attribute> getMonitorData()
  {
    assert debugEnter(CLASS_NAME, "getMonitorData");

    ArrayList<Attribute> attrs = new ArrayList<Attribute>(29);

    long tmpAbandonRequests;
    long tmpAddRequests;
    long tmpAddResponses;
    long tmpBindRequests;
    long tmpBindResponses;
    long tmpBytesRead;
    long tmpBytesWritten;
    long tmpCompareRequests;
    long tmpCompareResponses;
    long tmpConnectionsClosed;
    long tmpConnectionsEstablished;
    long tmpDeleteRequests;
    long tmpDeleteResponses;
    long tmpExtendedRequests;
    long tmpExtendedResponses;
    long tmpMessagesRead;
    long tmpMessagesWritten;
    long tmpModifyRequests;
    long tmpModifyResponses;
    long tmpModifyDNRequests;
    long tmpModifyDNResponses;
    long tmpOperationsAbandoned;
    long tmpOperationsCompleted;
    long tmpOperationsInitiated;
    long tmpSearchRequests;
    long tmpSearchEntries;
    long tmpSearchReferences;
    long tmpSearchResultsDone;
    long tmpUnbindRequests;

    // Quickly grab the locks and store consistent copies of the information.
    // Note that when grabbing multiple locks, it is essential that they are all
    // acquired in the same order to prevent deadlocks.
    abandonLock.lock();

    try
    {
      connectLock.lock();

      try
      {
        disconnectLock.lock();

        try
        {
          writeLock.lock();

          try
          {
            readLock.lock();

            try
            {
              tmpAbandonRequests        = abandonRequests;
              tmpAddRequests            = addRequests;
              tmpAddResponses           = addResponses;
              tmpBindRequests           = bindRequests;
              tmpBindResponses          = bindResponses;
              tmpBytesRead              = bytesRead;
              tmpBytesWritten           = bytesWritten;
              tmpCompareRequests        = compareRequests;
              tmpCompareResponses       = compareResponses;
              tmpConnectionsClosed      = connectionsClosed;
              tmpConnectionsEstablished = connectionsEstablished;
              tmpDeleteRequests         = deleteRequests;
              tmpDeleteResponses        = deleteResponses;
              tmpExtendedRequests       = extendedRequests;
              tmpExtendedResponses      = extendedResponses;
              tmpMessagesRead           = messagesRead;
              tmpMessagesWritten        = messagesWritten;
              tmpModifyRequests         = modifyRequests;
              tmpModifyResponses        = modifyResponses;
              tmpModifyDNRequests       = modifyDNRequests;
              tmpModifyDNResponses      = modifyDNResponses;
              tmpOperationsAbandoned    = operationsAbandoned;
              tmpOperationsCompleted    = operationsCompleted;
              tmpOperationsInitiated    = operationsInitiated;
              tmpSearchRequests         = searchRequests;
              tmpSearchEntries          = searchResultEntries;
              tmpSearchReferences       = searchResultReferences;
              tmpSearchResultsDone      = searchResultsDone;
              tmpUnbindRequests         = unbindRequests;
            }
            catch (Exception e)
            {
              assert debugException(CLASS_NAME, "getMonitorData", e);

              return attrs;
            }
            finally
            {
              readLock.unlock();
            }
          }
          catch (Exception e)
          {
            assert debugException(CLASS_NAME, "getMonitorData", e);

            return attrs;
          }
          finally
          {
            writeLock.unlock();
          }
        }
        catch (Exception e)
        {
          assert debugException(CLASS_NAME, "getMonitorData", e);

          return attrs;
        }
        finally
        {
          disconnectLock.unlock();
        }
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "getMonitorData", e);

        return attrs;
      }
      finally
      {
        connectLock.unlock();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "getMonitorData", e);

      return attrs;
    }
    finally
    {
      abandonLock.unlock();
    }


    // Construct the list of attributes to return.
    attrs.add(createAttribute("connectionsEstablished",
                              String.valueOf(tmpConnectionsEstablished)));
    attrs.add(createAttribute("connectionsClosed",
                              String.valueOf(tmpConnectionsClosed)));
    attrs.add(createAttribute("bytesRead", String.valueOf(tmpBytesRead)));
    attrs.add(createAttribute("bytesWritten", String.valueOf(tmpBytesWritten)));
    attrs.add(createAttribute("ldapMessagesRead",
                              String.valueOf(tmpMessagesRead)));
    attrs.add(createAttribute("ldapMessagesWritten",
                              String.valueOf(tmpMessagesWritten)));
    attrs.add(createAttribute("operationsAbandoned",
                              String.valueOf(tmpOperationsAbandoned)));
    attrs.add(createAttribute("operationsInitiated",
                              String.valueOf(tmpOperationsInitiated)));
    attrs.add(createAttribute("operationsCompleted",
                              String.valueOf(tmpOperationsCompleted)));
    attrs.add(createAttribute("abandonRequests",
                              String.valueOf(tmpAbandonRequests)));
    attrs.add(createAttribute("addRequests", String.valueOf(tmpAddRequests)));
    attrs.add(createAttribute("addResponses", String.valueOf(tmpAddResponses)));
    attrs.add(createAttribute("bindRequests", String.valueOf(tmpBindRequests)));
    attrs.add(createAttribute("bindResponses",
                              String.valueOf(tmpBindResponses)));
    attrs.add(createAttribute("compareRequests",
                              String.valueOf(tmpCompareRequests)));
    attrs.add(createAttribute("compareResponses",
                              String.valueOf(tmpCompareResponses)));
    attrs.add(createAttribute("deleteRequests",
                              String.valueOf(tmpDeleteRequests)));
    attrs.add(createAttribute("deleteResponses",
                              String.valueOf(tmpDeleteResponses)));
    attrs.add(createAttribute("extendedRequests",
                              String.valueOf(tmpExtendedRequests)));
    attrs.add(createAttribute("extendedResponses",
                              String.valueOf(tmpExtendedResponses)));
    attrs.add(createAttribute("modifyRequests",
                              String.valueOf(tmpModifyRequests)));
    attrs.add(createAttribute("modifyResponses",
                              String.valueOf(tmpModifyResponses)));
    attrs.add(createAttribute("modifyDNRequests",
                              String.valueOf(tmpModifyDNRequests)));
    attrs.add(createAttribute("modifyDNResponses",
                              String.valueOf(tmpModifyDNResponses)));
    attrs.add(createAttribute("searchRequests",
                              String.valueOf(tmpSearchRequests)));
    attrs.add(createAttribute("searchResultEntries",
                              String.valueOf(tmpSearchEntries)));
    attrs.add(createAttribute("searchResultReferences",
                              String.valueOf(tmpSearchReferences)));
    attrs.add(createAttribute("searchResultsDone",
                              String.valueOf(tmpSearchResultsDone)));
    attrs.add(createAttribute("unbindRequests",
                              String.valueOf(tmpUnbindRequests)));

    return attrs;
  }



  /**
   * Clears any statistical information collected to this point.
   */
  public void clearStatistics()
  {
    assert debugEnter(CLASS_NAME, "clearStatistics");


    // Quickly grab the locks and store consistent copies of the information.
    // Note that when grabbing multiple locks, it is essential that they are all
    // acquired in the same order to prevent deadlocks.
    abandonLock.lock();

    try
    {
      connectLock.lock();

      try
      {
        disconnectLock.lock();

        try
        {
          writeLock.lock();

          try
          {
            readLock.lock();

            try
            {
              abandonRequests        = 0;
              addRequests            = 0;
              addResponses           = 0;
              bindRequests           = 0;
              bindResponses          = 0;
              bytesRead              = 0;
              bytesWritten           = 0;
              compareRequests        = 0;
              compareResponses       = 0;
              connectionsClosed      = 0;
              connectionsEstablished = 0;
              deleteRequests         = 0;
              deleteResponses        = 0;
              extendedRequests       = 0;
              extendedResponses      = 0;
              messagesRead           = 0;
              messagesWritten        = 0;
              modifyRequests         = 0;
              modifyResponses        = 0;
              modifyDNRequests       = 0;
              modifyDNResponses      = 0;
              operationsAbandoned    = 0;
              operationsCompleted    = 0;
              operationsInitiated    = 0;
              searchRequests         = 0;
              searchResultEntries    = 0;
              searchResultReferences = 0;
              searchResultsDone      = 0;
              unbindRequests         = 0;
            }
            catch (Exception e)
            {
              assert debugException(CLASS_NAME, "clearStatistics", e);
            }
            finally
            {
              readLock.unlock();
            }
          }
          catch (Exception e)
          {
            assert debugException(CLASS_NAME, "clearStatistics", e);
          }
          finally
          {
            writeLock.unlock();
          }
        }
        catch (Exception e)
        {
          assert debugException(CLASS_NAME, "clearStatistics", e);
        }
        finally
        {
          disconnectLock.unlock();
        }
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "clearStatistics", e);
      }
      finally
      {
        connectLock.unlock();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "clearStatistics", e);
    }
    finally
    {
      abandonLock.unlock();
    }
  }



  /**
   * Updates the appropriate set of counters to indicate that a new connection
   * has been established.
   */
  public void updateConnect()
  {
    assert debugEnter(CLASS_NAME, "updateConnect");

    connectLock.lock();

    try
    {
      connectionsEstablished++;
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "updateConnect", e);
    }
    finally
    {
      connectLock.unlock();
    }

    // Update the parent if there is one.
    if (parent != null)
    {
      parent.updateConnect();
    }
  }



  /**
   * Updates the appropriate set of counters to indicate that a connection has
   * been closed.
   */
  public void updateDisconnect()
  {
    assert debugEnter(CLASS_NAME, "updateDisconnect");

    disconnectLock.lock();

    try
    {
      connectionsClosed++;
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "updateDisconnect", e);
    }
    finally
    {
      disconnectLock.unlock();
    }

    // Update the parent if there is one.
    if (parent != null)
    {
      parent.updateDisconnect();
    }
  }



  /**
   * Updates the appropriate set of counters to indicate that the specified
   * number of bytes have been read by the client.
   *
   * @param  bytesRead  The number of bytes read by the client.
   */
  public void updateBytesRead(int bytesRead)
  {
    assert debugEnter(CLASS_NAME, "updateBytesRead", String.valueOf(bytesRead));

    readLock.lock();

    try
    {
      this.bytesRead += bytesRead;
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "updateBytesRead", e);
    }
    finally
    {
      readLock.unlock();
    }

    // Update the parent if there is one.
    if (parent != null)
    {
      parent.updateBytesRead(bytesRead);
    }
  }



  /**
   * Updates the appropriate set of counters based on the provided message that
   * has been read from the client.
   *
   * @param  message  The message that was read from the client.
   */
  public void updateMessageRead(LDAPMessage message)
  {
    assert debugEnter(CLASS_NAME, "updateMessageRead", String.valueOf(message),
                      String.valueOf(bytesRead));

    readLock.lock();

    try
    {
      messagesRead++;
      operationsInitiated++;

      switch (message.getProtocolOp().getType())
      {
        case OP_TYPE_ABANDON_REQUEST:
          abandonRequests++;
          break;
        case OP_TYPE_ADD_REQUEST:
          addRequests++;
          break;
        case OP_TYPE_BIND_REQUEST:
          bindRequests++;
          break;
        case OP_TYPE_COMPARE_REQUEST:
          compareRequests++;
          break;
        case OP_TYPE_DELETE_REQUEST:
          deleteRequests++;
          break;
        case OP_TYPE_EXTENDED_REQUEST:
          extendedRequests++;
          break;
        case OP_TYPE_MODIFY_REQUEST:
          modifyRequests++;
          break;
        case OP_TYPE_MODIFY_DN_REQUEST:
          modifyDNRequests++;
          break;
        case OP_TYPE_SEARCH_REQUEST:
          searchRequests++;
          break;
        case OP_TYPE_UNBIND_REQUEST:
          unbindRequests++;
          break;
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "updateMessagesRead", e);
    }
    finally
    {
      readLock.unlock();
    }

    // Update the parent if there is one.
    if (parent != null)
    {
      parent.updateMessageRead(message);
    }
  }



  /**
   * Updates the appropriate set of counters based on the provided message that
   * has been written to the client.
   *
   * @param  message       The message that was written to the client.
   * @param  bytesWritten  The size of the message written in bytes.
   */
  public void updateMessageWritten(LDAPMessage message, int bytesWritten)
  {
    assert debugEnter(CLASS_NAME, "updateMessageWritten",
                      String.valueOf(message),  String.valueOf(bytesWritten));

    writeLock.lock();

    try
    {
      this.bytesWritten += bytesWritten;
      messagesWritten++;

      switch (message.getProtocolOp().getType())
      {
        case OP_TYPE_ADD_RESPONSE:
          addResponses++;
          operationsCompleted++;
          break;
        case OP_TYPE_BIND_RESPONSE:
          bindResponses++;
          operationsCompleted++;
          break;
        case OP_TYPE_COMPARE_RESPONSE:
          compareResponses++;
          operationsCompleted++;
          break;
        case OP_TYPE_DELETE_RESPONSE:
          deleteResponses++;
          operationsCompleted++;
          break;
        case OP_TYPE_EXTENDED_RESPONSE:
          extendedResponses++;

          // We don't want to include unsolicited notifications as "completed"
          // operations.
          if (message.getMessageID() > 0)
          {
            operationsCompleted++;
          }
          break;
        case OP_TYPE_MODIFY_RESPONSE:
          modifyResponses++;
          operationsCompleted++;
          break;
        case OP_TYPE_MODIFY_DN_RESPONSE:
          modifyDNResponses++;
          operationsCompleted++;
          break;
        case OP_TYPE_SEARCH_RESULT_ENTRY:
          searchResultEntries++;
          break;
        case OP_TYPE_SEARCH_RESULT_REFERENCE:
          searchResultReferences++;
          break;
        case OP_TYPE_SEARCH_RESULT_DONE:
          searchResultsDone++;
          operationsCompleted++;
          break;
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "updateMessageWritten", e);
    }
    finally
    {
      writeLock.unlock();
    }

    // Update the parent if there is one.
    if (parent != null)
    {
      parent.updateMessageWritten(message, bytesWritten);
    }
  }



  /**
   * Updates the appropriate set of counters to indicate that an operation was
   * abandoned without sending a response to the client.
   */
  public void updateAbandonedOperation()
  {
    assert debugEnter(CLASS_NAME, "updateAbandonedOperation");

    abandonLock.lock();

    try
    {
      operationsAbandoned++;
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "updateAbandonedOperation", e);
    }
    finally
    {
      abandonLock.unlock();
    }

    // Update the parent if there is one.
    if (parent != null)
    {
      parent.updateAbandonedOperation();
    }
  }



  /**
   * Constructs an attribute using the provided information.  It will have the
   * default syntax.
   *
   * @param  name   The name to use for the attribute.
   * @param  value  The value to use for the attribute.
   *
   * @return  the constructed attribute.
   */
  private Attribute createAttribute(String name, String value)
  {
    assert debugEnter(CLASS_NAME, "createAttribute", String.valueOf(name),
                      String.valueOf(value));

    AttributeType attrType = DirectoryServer.getDefaultAttributeType(name);

    ASN1OctetString encodedValue = new ASN1OctetString(value);
    LinkedHashSet<AttributeValue> values = new LinkedHashSet<AttributeValue>(1);

    try
    {
      values.add(new AttributeValue(encodedValue,
                                    attrType.normalize(encodedValue)));
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "createAttribute", e);

      values.add(new AttributeValue(encodedValue, encodedValue));
    }

    return new Attribute(attrType, name, values);
  }
}

