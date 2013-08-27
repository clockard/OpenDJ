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
package org.opends.server.replication.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;

import org.opends.server.protocols.asn1.ASN1Writer;
import org.opends.server.replication.protocol.ProtocolVersion;
import org.opends.server.types.ByteString;

/**
 * This class is used to associate serverIds with ChangeNumbers.
 * <p>
 * For example, it is exchanged with the replication servers at connection
 * establishment time to communicate
 * "which ChangeNumbers last seen by a serverId"
 */
public class ServerState implements Iterable<Integer>
{

  /** Associates a serverId with a ChangeNumber. */
  private final Map<Integer, ChangeNumber> serverIdToChangeNumber =
      new HashMap<Integer, ChangeNumber>();
  /**
   * Whether the state has been saved to persistent storage. It starts at true,
   * and moves to false when an update is made to the current object.
   */
  private volatile boolean saved = true;

  /**
   * Creates a new empty ServerState.
   */
  public ServerState()
  {
    super();
  }

  /**
   * Empty the ServerState.
   * After this call the Server State will be in the same state
   * as if it was just created.
   */
  public void clear()
  {
    synchronized (serverIdToChangeNumber)
    {
      serverIdToChangeNumber.clear();
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
      while (endpos > pos)
      {
        // FIXME JNR: why store the serverId separately from the changeNumber
        // since the changeNumber already contains the serverId?

        // read the ServerId
        int length = getNextLength(in, pos);
        String serverIdString = new String(in, pos, length, "UTF-8");
        int serverId = Integer.valueOf(serverIdString);
        pos += length +1;

        // read the ChangeNumber
        length = getNextLength(in, pos);
        String cnString = new String(in, pos, length, "UTF-8");
        ChangeNumber cn = new ChangeNumber(cnString);
        pos += length +1;

        // Add the serverId
        serverIdToChangeNumber.put(serverId, cn);
      }
    } catch (UnsupportedEncodingException e)
    {
      throw new DataFormatException("UTF-8 is not supported by this jvm.");
    }
  }

  /**
   * Get the length of the next String encoded in the in byte array.
   * This method is used to cut the different parts (server ids, change number)
   * of a server state.
   *
   * @param in the byte array where to calculate the string.
   * @param pos the position where to start from in the byte array.
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
        throw new DataFormatException("byte[] is not a valid server state");
      length++;
    }
    return length;
  }

  /**
   * Update the Server State with a ChangeNumber.
   *
   * @param changeNumber    The committed ChangeNumber.
   *
   * @return a boolean indicating if the update was meaningful.
   */
  public boolean update(ChangeNumber changeNumber)
  {
    if (changeNumber == null)
      return false;

    saved = false;

    synchronized (serverIdToChangeNumber)
    {
      int serverId = changeNumber.getServerId();
      ChangeNumber oldCN = serverIdToChangeNumber.get(serverId);
      if (oldCN == null || changeNumber.newer(oldCN))
      {
        serverIdToChangeNumber.put(serverId, changeNumber);
        return true;
      }
      return false;
    }
  }

  /**
   * Update the Server State with a Server State. Every change number of this
   * object is updated with the change number of the passed server state if
   * it is newer.
   *
   * @param serverState the server state to use for the update.
   *
   * @return a boolean indicating if the update was meaningful.
   */
  public boolean update(ServerState serverState)
  {
    if (serverState == null)
      return false;

    boolean updated = false;
    for (ChangeNumber cn : serverState.serverIdToChangeNumber.values())
    {
      if (update(cn))
      {
        updated = true;
      }
    }
    return updated;
  }

  /**
   * Replace the Server State with another ServerState.
   *
   * @param serverState The ServerState.
   *
   * @return a boolean indicating if the update was meaningful.
   */
  public boolean reload(ServerState serverState) {
    if (serverState == null) {
      return false;
    }

    synchronized (serverIdToChangeNumber)
    {
      clear();
      return update(serverState);
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
    Set<String> set = new HashSet<String>();

    synchronized (serverIdToChangeNumber)
    {
      for (ChangeNumber change : serverIdToChangeNumber.values())
      {
        Date date = new Date(change.getTime());
        set.add(change + " " + date + " " + change.getTime());
      }
    }

    return set;
  }

  /**
   * Return an ArrayList of ANS1OctetString encoding the ChangeNumbers
   * contained in the ServerState.
   * @return an ArrayList of ANS1OctetString encoding the ChangeNumbers
   * contained in the ServerState.
   */
  public ArrayList<ByteString> toASN1ArrayList()
  {
    ArrayList<ByteString> values = new ArrayList<ByteString>(0);

    synchronized (serverIdToChangeNumber)
    {
      for (ChangeNumber changeNumber : serverIdToChangeNumber.values())
      {
        values.add(ByteString.valueOf(changeNumber.toString()));
      }
    }
    return values;
  }



  /**
   * Encodes this server state to the provided ASN1 writer.
   *
   * @param writer
   *          The ASN1 writer.
   * @param protocolVersion
   *          The replication protocol version.
   * @throws IOException
   *           If an error occurred during encoding.
   */
  public void writeTo(ASN1Writer writer, short protocolVersion)
      throws IOException
  {
    synchronized (serverIdToChangeNumber)
    {
      if (protocolVersion >= ProtocolVersion.REPLICATION_PROTOCOL_V7)
      {
        for (ChangeNumber cn : serverIdToChangeNumber.values())
        {
          writer.writeOctetString(cn.toByteString());
        }
      }
      else
      {
        for (ChangeNumber cn : serverIdToChangeNumber.values())
        {
          writer.writeOctetString(cn.toString());
        }
      }
    }
  }

  /**
   * Return the text representation of ServerState.
   * @return the text representation of ServerState
   */
  @Override
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();

    synchronized (serverIdToChangeNumber)
    {
      for (ChangeNumber change : serverIdToChangeNumber.values())
      {
        buffer.append(change).append(" ");
      }
      if (!serverIdToChangeNumber.isEmpty())
        buffer.deleteCharAt(buffer.length() - 1);
    }

    return buffer.toString();
  }

  /**
   * Returns the {@code ChangeNumber} contained in this server state which
   * corresponds to the provided server ID.
   *
   * @param serverId
   *          The server ID.
   * @return The {@code ChangeNumber} contained in this server state which
   *         corresponds to the provided server ID.
   */
  public ChangeNumber getChangeNumber(int serverId)
  {
    return serverIdToChangeNumber.get(serverId);
  }

  /**
   * Returns the largest (most recent) {@code ChangeNumber} in this server
   * state.
   *
   * @return The largest (most recent) {@code ChangeNumber} in this server
   *         state.
   */
  public ChangeNumber getMaxChangeNumber()
  {
    ChangeNumber maxCN = null;

    synchronized (serverIdToChangeNumber)
    {
      for (ChangeNumber tmpMax : serverIdToChangeNumber.values())
      {
        if (maxCN == null || tmpMax.newer(maxCN))
          maxCN = tmpMax;
      }
    }
    return maxCN;
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
    synchronized (serverIdToChangeNumber)
    {
      final int size = serverIdToChangeNumber.size();
      List<String> idList = new ArrayList<String>(size);
      List<String> cnList = new ArrayList<String>(size);
      // calculate the total length needed to allocate byte array
      int length = 0;
      for (Entry<Integer, ChangeNumber> entry : serverIdToChangeNumber
          .entrySet())
      {
        // serverId is useless, see comment in ServerState ctor
        String serverIdStr = String.valueOf(entry.getKey());
        idList.add(serverIdStr);
        length += serverIdStr.length() + 1;

        String changeNumberStr = entry.getValue().toString();
        cnList.add(changeNumberStr);
        length += changeNumberStr.length() + 1;
      }
      byte[] result = new byte[length];

      // write the server state into the byte array
      int pos = 0;
      for (int i = 0; i < size; i++)
      {
        String str = idList.get(i);
        pos = addByteArray(str.getBytes("UTF-8"), result, pos);
        str = cnList.get(i);
        pos = addByteArray(str.getBytes("UTF-8"), result, pos);
      }
      return result;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Integer> iterator()
  {
    return serverIdToChangeNumber.keySet().iterator();
  }

  /**
   * Check that all the ChangeNumbers in the covered serverState are also in
   * this serverState.
   *
   * @param covered The ServerState that needs to be checked.
   * @return A boolean indicating if this ServerState covers the ServerState
   *         given in parameter.
   */
  public boolean cover(ServerState covered)
  {
    for (ChangeNumber coveredChange : covered.serverIdToChangeNumber.values())
    {
      if (!cover(coveredChange))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks that the ChangeNumber given as a parameter is in this ServerState.
   *
   * @param   covered The ChangeNumber that should be checked.
   * @return  A boolean indicating if this ServerState contains the ChangeNumber
   *          given in parameter.
   */
  public boolean cover(ChangeNumber covered)
  {
    ChangeNumber change =
        this.serverIdToChangeNumber.get(covered.getServerId());
    return change != null && !change.older(covered);
  }

  /**
   * Tests if the state is empty.
   *
   * @return True if the state is empty.
   */
  public boolean isEmpty()
  {
    return serverIdToChangeNumber.isEmpty();
  }

  /**
   * Make a duplicate of this state.
   * @return The duplicate of this state.
   */
  public ServerState duplicate()
  {
    ServerState newState = new ServerState();
    synchronized (serverIdToChangeNumber)
    {
      newState.serverIdToChangeNumber.putAll(serverIdToChangeNumber);
    }
    return newState;
  }

  /**
   * Computes the number of changes a first server state has in advance
   * compared to a second server state.
   * @param ss1 The server state supposed to be newer than the second one
   * @param ss2 The server state supposed to be older than the first one
   * @return The difference of changes (sum of the differences for each server
   * id changes). 0 If no gap between 2 states.
   * @throws IllegalArgumentException If one of the passed state is null
   */
  public static int diffChanges(ServerState ss1, ServerState ss2)
    throws  IllegalArgumentException
  {
    if (ss1 == null || ss2 == null)
    {
      throw new IllegalArgumentException("Null server state(s)");
    }

    int diff = 0;
    for (Integer serverId : ss1.serverIdToChangeNumber.keySet())
    {
      ChangeNumber cn1 = ss1.serverIdToChangeNumber.get(serverId);
      if (cn1 != null)
      {
        ChangeNumber cn2 = ss2.serverIdToChangeNumber.get(serverId);
         if (cn2 != null)
         {
           diff += ChangeNumber.diffSeqNum(cn1, cn2);
         } else {
           // ss2 does not have a change for this server id but ss1, so the
           // server holding ss1 has every changes represented in cn1 in advance
           // compared to server holding ss2, add this amount
           diff += cn1.getSeqnum();
         }
      }
    }

    return diff;
  }

  /**
   * Set the saved status of this ServerState.
   *
   * @param b A boolean indicating if the State has been safely stored.
   */
  public void setSaved(boolean b)
  {
    saved = b;
  }

  /**
   * Get the saved status of this ServerState.
   *
   * @return The saved status of this ServerState.
   */
  public boolean isSaved()
  {
    return saved;
  }

  /**
   * Build a copy of the ServerState with only ChangeNumbers older than
   * a specific ChangeNumber. This is used when building the initial
   * Cookie in the External Changelog, to cope with purged changes.
   * @param cn The ChangeNumber to compare the ServerState with
   * @return a copy of the ServerState which only contains the ChangeNumbers
   *         older than cn.
   */
  public ServerState duplicateOnlyOlderThan(ChangeNumber cn)
  {
    ServerState newState = new ServerState();
    synchronized (serverIdToChangeNumber)
    {
      for (ChangeNumber change : serverIdToChangeNumber.values())
      {
        if (change.older(cn))
        {
          newState.serverIdToChangeNumber.put(change.getServerId(), change);
        }
      }
    }
    return newState;
  }

}
