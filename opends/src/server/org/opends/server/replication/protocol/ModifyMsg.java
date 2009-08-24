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
 *      Copyright 2006-2009 Sun Microsystems, Inc.
 */
package org.opends.server.replication.protocol;

import static org.opends.server.replication.protocol.OperationContext.*;

import org.opends.server.core.ModifyOperationBasis;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.asn1.ASN1Exception;
import org.opends.server.protocols.asn1.ASN1Reader;
import org.opends.server.protocols.asn1.ASN1;
import org.opends.server.replication.common.ChangeNumber;
import org.opends.server.types.*;
import org.opends.server.types.AbstractOperation;
import org.opends.server.types.DN;
import org.opends.server.types.LDAPException;
import org.opends.server.types.Modification;
import org.opends.server.types.RawModification;
import org.opends.server.types.operation.PostOperationModifyOperation;


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Message used to send Modify information.
 */
public class ModifyMsg extends ModifyCommonMsg
{
  /**
   * Creates a new Modify message from a ModifyOperation.
   *
   * @param op The operation to use for building the message
   */
  public ModifyMsg(PostOperationModifyOperation op)
  {
    super((OperationContext) op.getAttachment(OperationContext.SYNCHROCONTEXT),
          op.getRawEntryDN().toString());
    encodedMods = modsToByte(op.getModifications());
  }

  /**
   * Creates a new Modify message using the provided information.
   *
   * @param changeNumber The ChangeNumber for the operation.
   * @param dn           The baseDN of the operation.
   * @param mods         The mod of the operation.
   * @param entryuuid    The unique id of the entry on which the modification
   *                     needs to apply.
   */
  public ModifyMsg(ChangeNumber changeNumber, DN dn, List<Modification> mods,
                   String entryuuid)
  {
    super(new ModifyContext(changeNumber, entryuuid),
          dn.toNormalizedString());
    this.encodedMods = modsToByte(mods);
  }

  /**
   * Creates a new Modify message from a byte[].
   *
   * @param in The byte[] from which the operation must be read.
   * @throws DataFormatException If the input byte[] is not a valid ModifyMsg
   * @throws UnsupportedEncodingException If UTF8 is not supported by the JVM.
   */
  public ModifyMsg(byte[] in) throws DataFormatException,
                                     UnsupportedEncodingException
  {
    bytes = in;

    // Decode header
    byte[] allowedPduTypes = new byte[2];
    allowedPduTypes[0] = MSG_TYPE_MODIFY;
    allowedPduTypes[1] = MSG_TYPE_MODIFY_V1;
    int pos = decodeHeader(allowedPduTypes, in);

    /* Read the mods : all the remaining bytes but the terminating 0 */
    int length = in.length - pos - 1;
    encodedMods = new byte[length];
    try
    {
      System.arraycopy(in, pos, encodedMods, 0, length);
    } catch (IndexOutOfBoundsException e)
    {
      throw new DataFormatException(e.getMessage());
    } catch (ArrayStoreException e)
    {
      throw new DataFormatException(e.getMessage());
    } catch (NullPointerException e)
    {
      throw new DataFormatException(e.getMessage());
    }
  }

  /**
   * Creates a new Modify message from a V1 byte[].
   *
   * @param in The byte[] from which the operation must be read.
   * @throws DataFormatException If the input byte[] is not a valid ModifyMsg
   * @throws UnsupportedEncodingException If UTF8 is not supported by the JVM.
   *
   * @return The created ModifyMsg.
   */
  public static ModifyMsg createV1(byte[] in) throws DataFormatException,
                                     UnsupportedEncodingException
  {
    ModifyMsg msg = new ModifyMsg(in);
    msg.bytes = null;

    return msg;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getBytes() throws UnsupportedEncodingException
  {
    if (bytes == null)
    {
      /* encode the header in a byte[] large enough to also contain the mods */
      byte[] mybytes = encodeHeader(MSG_TYPE_MODIFY, encodedMods.length + 1);

      /* add the mods */
      int pos = mybytes.length - (encodedMods.length + 1);
      addByteArray(encodedMods, mybytes, pos);

      return mybytes;
    }
    else
    {
      return bytes;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AbstractOperation createOperation(InternalClientConnection connection,
                   String newDn)
                   throws LDAPException, ASN1Exception, DataFormatException
  {
    if (newDn == null)
      newDn = getDn();

    ArrayList<RawModification> ldapmods = new ArrayList<RawModification>();

    ASN1Reader asn1Reader = ASN1.getReader(encodedMods);
    while(asn1Reader.hasNextElement())
    {
      ldapmods.add(LDAPModification.decode(asn1Reader));
    }

    ModifyOperationBasis mod = new ModifyOperationBasis(connection,
                               InternalClientConnection.nextOperationID(),
                               InternalClientConnection.nextMessageID(), null,
        ByteString.valueOf(newDn), ldapmods);
    ModifyContext ctx = new ModifyContext(getChangeNumber(), getUniqueId());
    mod.setAttachment(SYNCHROCONTEXT, ctx);
    return mod;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    if (protocolVersion == ProtocolVersion.REPLICATION_PROTOCOL_V1)
    {
      return "ModifyMsg content: " +
        " protocolVersion: " + protocolVersion +
        " dn: " + dn +
        " changeNumber: " + changeNumber +
        " uniqueId: " + uniqueId +
        " assuredFlag: " + assuredFlag;
    }
    if (protocolVersion >= ProtocolVersion.REPLICATION_PROTOCOL_V2)
    {
      return "ModifyMsg content: " +
        " protocolVersion: " + protocolVersion +
        " dn: " + dn +
        " changeNumber: " + changeNumber +
        " uniqueId: " + uniqueId +
        " assuredFlag: " + assuredFlag +
        " assuredMode: " + assuredMode +
        " safeDataLevel: " + safeDataLevel;
    }
    return "!!! Unknown version: " + protocolVersion + "!!!";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size()
  {
    // The ModifyMsg can be very large when added or deleted attribute
    // values are very large. We therefore need to count the
    // whole encoded msg.
    return encodedMods.length + 100; // 100 let's assume header size is 100
  }

  /**
   * {@inheritDoc}
   */
  public byte[] getBytes_V1() throws UnsupportedEncodingException
  {
    /* encode the header in a byte[] large enough to also contain the mods */
    byte[] encodedMsg = encodeHeader_V1(MSG_TYPE_MODIFY_V1, encodedMods.length +
      1);

    /* add the mods */
    int pos = encodedMsg.length - (encodedMods.length + 1);
    addByteArray(encodedMods, encodedMsg, pos);

    return encodedMsg;
  }
}
