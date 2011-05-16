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
 *      Copyright 2010 Sun Microsystems, Inc.
 */
package org.opends.sdk.controls;



import static com.sun.opends.sdk.util.StaticUtils.getExceptionMessage;
import static org.opends.sdk.CoreMessages.*;
import static org.opends.sdk.asn1.ASN1Constants.UNIVERSAL_INTEGER_TYPE;
import static org.opends.sdk.asn1.ASN1Constants.UNIVERSAL_OCTET_STRING_TYPE;

import java.io.IOException;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.opends.sdk.*;
import org.opends.sdk.asn1.ASN1;
import org.opends.sdk.asn1.ASN1Reader;
import org.opends.sdk.asn1.ASN1Writer;
import org.opends.sdk.schema.Schema;

import com.sun.opends.sdk.util.StaticUtils;
import com.sun.opends.sdk.util.Validator;



/**
 * The entry change notification response control as defined in
 * draft-ietf-ldapext-psearch. This control provides additional information
 * about the change the caused a particular entry to be returned as the result
 * of a persistent search.
 *
 * @see PersistentSearchRequestControl
 * @see PersistentSearchChangeType
 * @see <a
 *      href="http://tools.ietf.org/html/draft-ietf-ldapext-psearch">draft-ietf-ldapext-psearch
 *      - Persistent Search: A Simple LDAP Change Notification Mechanism </a>
 */
public final class EntryChangeNotificationResponseControl implements Control
{
  /**
   * The OID for the entry change notification response control.
   */
  public static final String OID = "2.16.840.1.113730.3.4.7";

  /**
   * A decoder which can be used for decoding the entry change notification
   * response control.
   */
  public static final ControlDecoder<EntryChangeNotificationResponseControl>
    DECODER = new ControlDecoder<EntryChangeNotificationResponseControl>()
  {

    public EntryChangeNotificationResponseControl decodeControl(
        final Control control, final DecodeOptions options)
        throws DecodeException
    {
      Validator.ensureNotNull(control, options);

      if (control instanceof EntryChangeNotificationResponseControl)
      {
        return (EntryChangeNotificationResponseControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_ECN_CONTROL_BAD_OID.get(control
            .getOID(), OID);
        throw DecodeException.error(message);
      }

      if (!control.hasValue())
      {
        // The response control must always have a value.
        final LocalizableMessage message = ERR_ECN_NO_CONTROL_VALUE.get();
        throw DecodeException.error(message);
      }

      String previousDNString = null;
      long changeNumber = -1;
      PersistentSearchChangeType changeType;
      final ASN1Reader reader = ASN1.getReader(control.getValue());
      try
      {
        reader.readStartSequence();

        final int changeTypeInt = reader.readEnumerated();
        switch (changeTypeInt)
        {
        case 1:
          changeType = PersistentSearchChangeType.ADD;
          break;
        case 2:
          changeType = PersistentSearchChangeType.DELETE;
          break;
        case 4:
          changeType = PersistentSearchChangeType.MODIFY;
          break;
        case 8:
          changeType = PersistentSearchChangeType.MODIFY_DN;
          break;
        default:
          final LocalizableMessage message = ERR_ECN_BAD_CHANGE_TYPE
              .get(changeTypeInt);
          throw DecodeException.error(message);
        }

        if (reader.hasNextElement()
            && (reader.peekType() == UNIVERSAL_OCTET_STRING_TYPE))
        {
          if (changeType != PersistentSearchChangeType.MODIFY_DN)
          {
            final LocalizableMessage message = ERR_ECN_ILLEGAL_PREVIOUS_DN
                .get(String.valueOf(changeType));
            throw DecodeException.error(message);
          }

          previousDNString = reader.readOctetStringAsString();
        }
        if (reader.hasNextElement()
            && (reader.peekType() == UNIVERSAL_INTEGER_TYPE))
        {
          changeNumber = reader.readInteger();
        }
      }
      catch (final IOException e)
      {
        StaticUtils.DEBUG_LOG.throwing(
            "EntryChangeNotificationControl.Decoder", "decode", e);

        final LocalizableMessage message = ERR_ECN_CANNOT_DECODE_VALUE
            .get(getExceptionMessage(e));
        throw DecodeException.error(message, e);
      }

      final Schema schema = options.getSchemaResolver().resolveSchema(
          previousDNString);
      DN previousDN = null;
      if (previousDNString != null)
      {
        try
        {
          previousDN = DN.valueOf(previousDNString, schema);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          final LocalizableMessage message = ERR_ECN_INVALID_PREVIOUS_DN
              .get(getExceptionMessage(e));
          throw DecodeException.error(message, e);
        }
      }

      return new EntryChangeNotificationResponseControl(control.isCritical(),
          changeType, previousDN, changeNumber);
    }



    public String getOID()
    {
      return OID;
    }
  };



  /**
   * Creates a new entry change notification response control with the provided
   * change type and optional previous distinguished name and change number.
   *
   * @param type
   *          The change type for this change notification control.
   * @param previousName
   *          The distinguished name that the entry had prior to a modify DN
   *          operation, or <CODE>null</CODE> if the operation was not a modify
   *          DN.
   * @param changeNumber
   *          The change number for the associated change, or a negative value
   *          if no change number is available.
   * @return The new control.
   * @throws NullPointerException
   *           If {@code type} was {@code null}.
   */
  public static EntryChangeNotificationResponseControl newControl(
      final PersistentSearchChangeType type, final DN previousName,
      final long changeNumber) throws NullPointerException
  {
    return new EntryChangeNotificationResponseControl(false, type,
        previousName, changeNumber);
  }



  /**
   * Creates a new entry change notification response control with the provided
   * change type and optional previous distinguished name and change number. The
   * previous distinguished name, if provided, will be decoded using the default
   * schema.
   *
   * @param type
   *          The change type for this change notification control.
   * @param previousName
   *          The distinguished name that the entry had prior to a modify DN
   *          operation, or <CODE>null</CODE> if the operation was not a modify
   *          DN.
   * @param changeNumber
   *          The change number for the associated change, or a negative value
   *          if no change number is available.
   * @return The new control.
   * @throws LocalizedIllegalArgumentException
   *           If {@code previousName} is not a valid LDAP string representation
   *           of a DN.
   * @throws NullPointerException
   *           If {@code type} was {@code null}.
   */
  public static EntryChangeNotificationResponseControl newControl(
      final PersistentSearchChangeType type, final String previousName,
      final long changeNumber) throws LocalizedIllegalArgumentException,
      NullPointerException
  {
    return new EntryChangeNotificationResponseControl(false, type, DN
        .valueOf(previousName), changeNumber);
  }



  // The previous DN for this change notification control.
  private final DN previousName;

  // The change number for this change notification control.
  private final long changeNumber;

  // The change type for this change notification control.
  private final PersistentSearchChangeType changeType;

  private final boolean isCritical;



  private EntryChangeNotificationResponseControl(final boolean isCritical,
      final PersistentSearchChangeType changeType, final DN previousName,
      final long changeNumber)
  {
    Validator.ensureNotNull(changeType);
    this.isCritical = isCritical;
    this.changeType = changeType;
    this.previousName = previousName;
    this.changeNumber = changeNumber;
  }



  /**
   * Returns the change number for this entry change notification control.
   *
   * @return The change number for this entry change notification control, or a
   *         negative value if no change number is available.
   */
  public long getChangeNumber()
  {
    return changeNumber;
  }



  /**
   * Returns the change type for this entry change notification control.
   *
   * @return The change type for this entry change notification control.
   */
  public PersistentSearchChangeType getChangeType()
  {
    return changeType;
  }



  /**
   * {@inheritDoc}
   */
  public String getOID()
  {
    return OID;
  }



  /**
   * Returns the distinguished name that the entry had prior to a modify DN
   * operation, or <CODE>null</CODE> if the operation was not a modify DN.
   *
   * @return The distinguished name that the entry had prior to a modify DN
   *         operation.
   */
  public DN getPreviousName()
  {
    return previousName;
  }



  /**
   * {@inheritDoc}
   */
  public ByteString getValue()
  {
    final ByteStringBuilder buffer = new ByteStringBuilder();
    final ASN1Writer writer = ASN1.getWriter(buffer);
    try
    {
      writer.writeStartSequence();
      writer.writeInteger(changeType.intValue());

      if (previousName != null)
      {
        writer.writeOctetString(previousName.toString());
      }

      if (changeNumber > 0)
      {
        writer.writeInteger(changeNumber);
      }
      writer.writeEndSequence();
      return buffer.toByteString();
    }
    catch (final IOException ioe)
    {
      // This should never happen unless there is a bug somewhere.
      throw new RuntimeException(ioe);
    }
  }



  /**
   * {@inheritDoc}
   */
  public boolean hasValue()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  public boolean isCritical()
  {
    return isCritical;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("EntryChangeNotificationResponseControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(", changeType=");
    builder.append(changeType.toString());
    builder.append(", previousDN=\"");
    builder.append(previousName);
    builder.append("\"");
    builder.append(", changeNumber=");
    builder.append(changeNumber);
    builder.append(")");
    return builder.toString();
  }

}
