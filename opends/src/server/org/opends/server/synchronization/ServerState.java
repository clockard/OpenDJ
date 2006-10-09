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
package org.opends.server.synchronization;

import static org.opends.server.loggers.Error.logError;
import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.synchronization.SynchMessages.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.DataFormatException;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.opends.server.core.AddOperation;
import org.opends.server.types.Control;
import org.opends.server.types.DN;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.protocols.ldap.LDAPException;
import org.opends.server.protocols.ldap.LDAPFilter;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.ModificationType;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;


/**
 * ServerState class.
 * This object is used to store the last update seem on this server
 * from each server.
 * It is exchanged with the changelog servers at connection establishment time
 * It is locally saved in the database
 * TODO : should extract from this object the code that read/save
 * from/to the database on the LDAP server side and put it in a new class
 * that is only used on the LDAP server side and that encapsulate this class.
 */
public class ServerState implements Serializable
{
  private static final long serialVersionUID = 314772980474416183L;

  private HashMap<Short, ChangeNumber> list;
  transient private static final String
                                  SYNCHRONIZATION_STATE = "ds-sync-state";
  transient private DN baseDn;
  transient boolean savedStatus = true;
  transient private InternalClientConnection conn =
                                              new InternalClientConnection();
  transient private ASN1OctetString serverStateAsn1Dn;
  transient private DN serverStateDn;

  /**
   * create a new ServerState.
   * @param baseDn The baseDN for which the ServerState is created
   */
  public ServerState(DN baseDn)
  {
    list = new HashMap<Short, ChangeNumber>();
    this.baseDn = baseDn;
    serverStateAsn1Dn = new ASN1OctetString(
        "dc=ffffffff-ffffffff-ffffffff-ffffffff,"
        + baseDn.toString());
    try
    {
      serverStateDn = DN.decode(serverStateAsn1Dn);
    } catch (DirectoryException e)
    {
      // never happens
    }
  }

  /**
   * Creates a new ServerState object from its encoded form.
   *
   * @param in The byte array containing the encoded ServerState form.
   * @param pos The position in the byte array where the encoded ServerState
   *            starts.
   * @param endpos The position in the byte array where the encoded ServerState
   *               ends.
   * @throws DataFormatException If the encoded form was not correct.
   */
  public ServerState(byte[] in, int pos, int endpos)
         throws DataFormatException
  {
    try
    {
      list = new HashMap<Short, ChangeNumber>();

      while (endpos > pos)
      {
        /*
         * read the ServerId
         */
        int length = getNextLength(in, pos);
        String serverIdString = new String(in, pos, length, "UTF-8");
        short serverId = Short.valueOf(serverIdString);
        pos += length +1;

        /*
         * read the ChangeNumber
         */
        length = getNextLength(in, pos);
        String cnString = new String(in, pos, length, "UTF-8");
        ChangeNumber cn = new ChangeNumber(cnString);
        pos += length +1;

        /*
         * Add the serverid
         */
        list.put(serverId, cn);
      }

    } catch (UnsupportedEncodingException e)
    {
      throw new DataFormatException("UTF-8 is not supported by this jvm.");
    }
  }

  /**
   * Get the length of the next String encoded in the in byte array.
   *
   * @param in the byte array where to calculate the string.
   * @param pos the position whre to start from in the byte array.
   * @return the length of the next string.
   * @throws DataFormatException If the byte array does not end with null.
   */
  private int getNextLength(byte[] in, int pos) throws DataFormatException
  {
    int offset = pos;
    int length = 0;
    while (in[offset++] != 0)
    {
      if (offset >= in.length)
        throw new DataFormatException("byte[] is not a valid modify msg");
      length++;
    }
    return length;
  }

  /**
   * Update the Server State with a ChangeNumber.
   * All operations with smaller CSN and the same serverID must be committed
   * before calling this method.
   * @param changeNumber the committed ChangeNumber.
   * @return a boolean indicating if the update was meaningfull.
   */
  public boolean update(ChangeNumber changeNumber)
  {
    if (changeNumber == null)
      return false;
    synchronized(this)
    {
      Short id =  changeNumber.getServerId();
      ChangeNumber oldCN = list.get(id);
      if (oldCN == null || changeNumber.newer(oldCN))
      {
        list.put(id,changeNumber);
        savedStatus = false;
        return true;
      }
      else
      {
        return false;
      }
    }
  }

  /**
   * return a Set of String usable as a textual representation of
   * a Server state.
   * format : time seqnum id
   *
   * example :
   *  1 00000109e4666da600220001
   *  2 00000109e44567a600220002
   *
   * @return the representation of the Server state
   */
  public Set<String> toStringSet()
  {
    HashSet<String> set = new HashSet<String>();
    synchronized (this)
    {

      for (Short key  : list.keySet())
      {
        ChangeNumber change = list.get(key);
        Date date = new Date(change.getTime());
        set.add(change.toString() + " " + date.toString());
      }
    }

    return set;
  }

  /**
   * return the text representation of ServerState.
   * @return the text representation of ServerState
   */
  @Override
  public String toString()
  {
    synchronized (this)
    {
      String str = null;
      for (Short key  : list.keySet())
      {
        ChangeNumber change = list.get(key);
        str += " " + change.toString();
      }

      return str;
    }
  }

  /**
   * Get the largest ChangeNumber seen for a given LDAP server ID.
   *
   * @param serverId : the server ID
   * @return the largest ChangeNumber seen
   */
  public ChangeNumber getMaxChangeNumber(short serverId)
  {
    return list.get(serverId);
  }

  /**
   * Save this object to persistent storage.
   */
  public void save()
  {
    if ((list.size() == 0) || savedStatus)
      return;

    ArrayList<ASN1OctetString> values = new ArrayList<ASN1OctetString>();
    synchronized (this)
    {
      for (Short id : list.keySet())
      {
        ASN1OctetString value = new ASN1OctetString(list.get(id).toString());
        values.add(value);
      }
      savedStatus = true;
    }
    LDAPAttribute attr = new LDAPAttribute(SYNCHRONIZATION_STATE, values);
    LDAPModification mod = new LDAPModification(ModificationType.REPLACE, attr);
    ArrayList<LDAPModification> mods = new ArrayList<LDAPModification>(1);
    mods.add(mod);

    boolean done = false;
    while (!done)
    {
      /*
       * Generate a modify operation on the Server State Entry :
       * cn=ffffffff-ffffffff-ffffffff-ffffffff, baseDn
       */
      ModifyOperation op =
        new ModifyOperation(conn, InternalClientConnection.nextOperationID(),
            InternalClientConnection.nextMessageID(),
            new ArrayList<Control>(0), serverStateAsn1Dn,
            mods);
      op.setInternalOperation(true);
      op.setSynchronizationOperation(true);

      op.run();
      ResultCode resultCode = op.getResultCode();
      if (resultCode != ResultCode.SUCCESS)
      {
        if (resultCode == ResultCode.NO_SUCH_OBJECT)
        {
          createStateEntry();
        }
        else
        {
          savedStatus = false;
          int msgID = MSGID_ERROR_UPDATING_RUV;
          String message = getMessage(msgID,
              op.getResultCode().getResultCodeName(),
              op.toString(), op.getErrorMessage(),
              baseDn.toString());
          logError(ErrorLogCategory.SYNCHRONIZATION,
              ErrorLogSeverity.SEVERE_ERROR,
              message, msgID);
          break;
        }
      }
      else
        done = true;
    }
  }

  /**
   * Load the ServerState from the backing entry in database to memory.
   */
  public void loadState()
  {
    /*
     * Read the serverState from the database,
     * If not there create empty entry
     */
    LDAPFilter filter;
    try
    {
      filter = LDAPFilter.decode("objectclass=*");
    } catch (LDAPException e)
    {
      // can not happen
      return;
    }

    /*
     * Search the database entry that is used to periodically
     * save the ServerState
     */
    InternalSearchOperation search = conn.processSearch(serverStateAsn1Dn,
        SearchScope.BASE_OBJECT,
        DereferencePolicy.DEREF_ALWAYS, 0, 0, false,
        filter,new LinkedHashSet<String>(0));
    if (((search.getResultCode() != ResultCode.SUCCESS)) &&
        ((search.getResultCode() != ResultCode.NO_SUCH_OBJECT)))
    {
      int msgID = MSGID_ERROR_SEARCHING_RUV;
      String message = getMessage(msgID,
          search.getResultCode().getResultCodeName(),
          search.toString(), search.getErrorMessage(),
          baseDn.toString());
      logError(ErrorLogCategory.SYNCHRONIZATION, ErrorLogSeverity.SEVERE_ERROR,
          message, msgID);
    }

    SearchResultEntry resultEntry = null;
    if (search.getResultCode() == ResultCode.SUCCESS)
    {
      /*
       * Read the serverState from the SYNCHRONIZATION_STATE attribute
       */
      LinkedList<SearchResultEntry> result = search.getSearchEntries();
      resultEntry = result.getFirst();
      if (resultEntry != null)
      {
        AttributeType synchronizationStateType =
          DirectoryServer.getAttributeType(SYNCHRONIZATION_STATE);
        List<Attribute> attrs =
          resultEntry.getAttribute(synchronizationStateType);
        if (attrs != null)
        {
          Attribute attr = attrs.get(0);
          LinkedHashSet<AttributeValue> values = attr.getValues();
          for (AttributeValue value : values)
          {
            ChangeNumber changeNumber =
              new ChangeNumber(value.getStringValue());
            update(changeNumber);
          }
        }
      }

      /*
       * TODO : The ServerState is saved to the database periodically,
       * therefore in case of crash it is possible that is does not contain
       * the latest changes that have been processed and saved to the
       * database.
       * In order to make sure that we don't loose them, search all the entries
       * that have been updated after this entry.
       * This is done by using the HistoricalCsnOrderingMatchingRule
       * and an ordering index for historical attribute
       */
    }

    if ((resultEntry == null) ||
        ((search.getResultCode() != ResultCode.SUCCESS)))
    {
      createStateEntry();
    }
  }

  /**
   * Create the Entry that will be used to store the ServerState information.
   * It will be updated when the server stops and periodically.
   */
  private void createStateEntry()
  {
    ArrayList<LDAPAttribute> attrs = new ArrayList<LDAPAttribute>();

    ArrayList<ASN1OctetString> values = new ArrayList<ASN1OctetString>();
    ASN1OctetString value = new ASN1OctetString("extensibleObject");
    values.add(value);
    LDAPAttribute attr = new LDAPAttribute("objectClass", values);
    value = new ASN1OctetString("domain");
    values.add(value);
    attr = new LDAPAttribute("objectClass", values);
    attrs.add(attr);

    values = new ArrayList<ASN1OctetString>();
    value = new ASN1OctetString("ffffffff-ffffffff-ffffffff-ffffffff");
    values.add(value);
    attr = new LDAPAttribute("dc", values);
    attrs.add(attr);

    AddOperation add = conn.processAdd(serverStateAsn1Dn, attrs);
    ResultCode resultCode = add.getResultCode();
    if ((resultCode != ResultCode.SUCCESS) &&
        (resultCode != ResultCode.NO_SUCH_OBJECT))
    {
      int msgID = MSGID_ERROR_UPDATING_RUV;
      String message = getMessage(msgID,
          add.getResultCode().getResultCodeName(),
          add.toString(), add.getErrorMessage(),
          baseDn.toString());
      logError(ErrorLogCategory.SYNCHRONIZATION,
          ErrorLogSeverity.SEVERE_ERROR,
          message, msgID);
    }
  }

  /**
   * Get the Dn where the ServerState is stored.
   * @return Returns the serverStateDn.
   */
  public DN getServerStateDn()
  {
    return serverStateDn;
  }

  /**
   * Add the tail into resultByteArray at position pos.
   */
  private int addByteArray(byte[] tail, byte[] resultByteArray, int pos)
  {
    for (int i=0; i<tail.length; i++,pos++)
    {
      resultByteArray[pos] = tail[i];
    }
    resultByteArray[pos++] = 0;
    return pos;
  }

  /**
   * Encode this ServerState object and return its byte array representation.
   *
   * @return a byte array with an encoded representation of this object.
   * @throws UnsupportedEncodingException if UTF8 is not supported by the JVM.
   */
  public byte[] getBytes() throws UnsupportedEncodingException
  {
    synchronized (this)
    {
      int length = 0;
      List<String> idList = new ArrayList<String>(list.size());
      for (short id : list.keySet())
      {
        String temp = String.valueOf(id);
        idList.add(temp);
        length += temp.length() + 1;
      }
      List<String> cnList = new ArrayList<String>(list.size());
      for (ChangeNumber cn : list.values())
      {
        String temp = cn.toString();
        cnList.add(temp);
        length += temp.length() + 1;
      }
      byte[] result = new byte[length];

      int pos = 0;
      for (int i=0; i< list.size(); i++)
      {
        String str = idList.get(i);
        pos = addByteArray(str.getBytes("UTF-8"), result, pos);
        str = cnList.get(i);
        pos = addByteArray(str.getBytes("UTF-8"), result, pos);
      }
      return result;
    }
  }
}
