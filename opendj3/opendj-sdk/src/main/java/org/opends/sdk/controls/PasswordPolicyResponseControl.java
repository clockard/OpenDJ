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



import static com.sun.opends.sdk.messages.Messages.*;
import static com.sun.opends.sdk.util.StaticUtils.byteToHex;
import static com.sun.opends.sdk.util.StaticUtils.getExceptionMessage;

import java.io.IOException;

import org.opends.sdk.*;
import org.opends.sdk.asn1.ASN1;
import org.opends.sdk.asn1.ASN1Reader;
import org.opends.sdk.asn1.ASN1Writer;

import com.sun.opends.sdk.util.StaticUtils;
import com.sun.opends.sdk.util.Validator;



/**
 * The password policy response control as defined in
 * draft-behera-ldap-password-policy.
 * <p>
 * If the client has sent a passwordPolicyRequest control, the server (when
 * solicited by the inclusion of the request control) sends this control with
 * the following operation responses: bindResponse, modifyResponse, addResponse,
 * compareResponse and possibly extendedResponse, to inform of various
 * conditions, and MAY be sent with other operations (in the case of the
 * changeAfterReset error).
 *
 * @see PasswordPolicyRequestControl
 * @see PasswordPolicyWarningType
 * @see PasswordPolicyErrorType
 * @see <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy">
 *         draft-behera-ldap-password-policy - Password Policy for LDAP
 *         Directories </a>
 */
public final class PasswordPolicyResponseControl implements Control
{
  /**
   * The OID for the password policy control from
   * draft-behera-ldap-password-policy.
   */
  public static final String OID = PasswordPolicyRequestControl.OID;

  private final int warningValue;

  private final PasswordPolicyErrorType errorType;

  private final PasswordPolicyWarningType warningType;

  /**
   * A decoder which can be used for decoding the password policy response
   * control.
   */
  public static final ControlDecoder<PasswordPolicyResponseControl> DECODER =
    new ControlDecoder<PasswordPolicyResponseControl>()
  {

    public PasswordPolicyResponseControl decodeControl(final Control control,
        final DecodeOptions options) throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof PasswordPolicyResponseControl)
      {
        return (PasswordPolicyResponseControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_PWPOLICYRES_CONTROL_BAD_OID.get(
            control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (!control.hasValue())
      {
        // The response control must always have a value.
        final LocalizableMessage message = ERR_PWPOLICYRES_NO_CONTROL_VALUE
            .get();
        throw DecodeException.error(message);
      }

      final ASN1Reader reader = ASN1.getReader(control.getValue());
      try
      {
        PasswordPolicyWarningType warningType = null;
        PasswordPolicyErrorType errorType = null;
        int warningValue = -1;

        reader.readStartSequence();

        if (reader.hasNextElement()
            && (reader.peekType() == TYPE_WARNING_ELEMENT))
        {
          // Its a CHOICE element. Read as sequence to retrieve
          // nested element.
          reader.readStartSequence();
          final int warningChoiceValue = (0x7F & reader.peekType());
          if (warningChoiceValue < 0
              || warningChoiceValue >= PasswordPolicyWarningType.values().length)
          {
            final LocalizableMessage message = ERR_PWPOLICYRES_INVALID_WARNING_TYPE
                .get(byteToHex(reader.peekType()));
            throw DecodeException.error(message);
          }
          else
          {
            warningType = PasswordPolicyWarningType.values()[warningChoiceValue];
          }
          warningValue = (int) reader.readInteger();
          reader.readEndSequence();
        }

        if (reader.hasNextElement()
            && (reader.peekType() == TYPE_ERROR_ELEMENT))
        {
          final int errorValue = reader.readEnumerated();
          if (errorValue < 0
              || errorValue >= PasswordPolicyErrorType.values().length)
          {
            final LocalizableMessage message = ERR_PWPOLICYRES_INVALID_ERROR_TYPE
                .get(errorValue);
            throw DecodeException.error(message);
          }
          else
          {
            errorType = PasswordPolicyErrorType.values()[errorValue];
          }
        }

        reader.readEndSequence();

        return new PasswordPolicyResponseControl(control.isCritical(),
            warningType, warningValue, errorType);
      }
      catch (final IOException e)
      {
        StaticUtils.DEBUG_LOG.throwing("PasswordPolicyControl.ResponseDecoder",
            "decode", e);

        final LocalizableMessage message = ERR_PWPOLICYRES_DECODE_ERROR
            .get(getExceptionMessage(e));
        throw DecodeException.error(message);
      }
    }



    public String getOID()
    {
      return OID;
    }
  };



  /**
   * Creates a new password policy response control with the provided error.
   *
   * @param errorType
   *          The password policy error type.
   * @return The new control.
   * @throws NullPointerException
   *           If {@code errorType} was {@code null}.
   */
  public static PasswordPolicyResponseControl newControl(
      final PasswordPolicyErrorType errorType) throws NullPointerException
  {
    Validator.ensureNotNull(errorType);

    return new PasswordPolicyResponseControl(false, null, -1, errorType);
  }



  /**
   * Creates a new password policy response control with the provided warning.
   *
   * @param warningType
   *          The password policy warning type.
   * @param warningValue
   *          The password policy warning value.
   * @return The new control.
   * @throws IllegalArgumentException
   *           If {@code warningValue} was negative.
   * @throws NullPointerException
   *           If {@code warningType} was {@code null}.
   */
  public static PasswordPolicyResponseControl newControl(
      final PasswordPolicyWarningType warningType, final int warningValue)
      throws IllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(warningType);
    Validator.ensureTrue(warningValue >= 0, "warningValue is negative");

    return new PasswordPolicyResponseControl(false, warningType, warningValue,
        null);
  }



  /**
   * Creates a new password policy response control with the provided warning
   * and error.
   *
   * @param warningType
   *          The password policy warning type.
   * @param warningValue
   *          The password policy warning value.
   * @param errorType
   *          The password policy error type.
   * @return The new control.
   * @throws IllegalArgumentException
   *           If {@code warningValue} was negative.
   * @throws NullPointerException
   *           If {@code warningType} or {@code errorType} was {@code null}.
   */
  public static PasswordPolicyResponseControl newControl(
      final PasswordPolicyWarningType warningType, final int warningValue,
      final PasswordPolicyErrorType errorType) throws IllegalArgumentException,
      NullPointerException
  {
    Validator.ensureNotNull(warningType, errorType);
    Validator.ensureTrue(warningValue >= 0, "warningValue is negative");

    return new PasswordPolicyResponseControl(false, warningType, warningValue,
        errorType);
  }



  private final boolean isCritical;

  /**
   * The BER type value for the warning element of the control value.
   */
  private static final byte TYPE_WARNING_ELEMENT = (byte) 0xA0;

  /**
   * The BER type value for the error element of the control value.
   */
  private static final byte TYPE_ERROR_ELEMENT = (byte) 0x81;



  private PasswordPolicyResponseControl(final boolean isCritical,
      final PasswordPolicyWarningType warningType, final int warningValue,
      final PasswordPolicyErrorType errorType)
  {
    this.isCritical = isCritical;
    this.warningType = warningType;
    this.warningValue = warningValue;
    this.errorType = errorType;
  }



  /**
   * Returns the password policy error type, if available.
   *
   * @return The password policy error type, or {@code null} if this control
   *         does not contain a error.
   */
  public PasswordPolicyErrorType getErrorType()
  {
    return errorType;
  }



  /**
   * {@inheritDoc}
   */
  public String getOID()
  {
    return OID;
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
      if (warningType != null)
      {
        // Just write the CHOICE element as a single element SEQUENCE.
        writer.writeStartSequence(TYPE_WARNING_ELEMENT);
        writer.writeInteger((byte) (0x80 | warningType.intValue()),
            warningValue);
        writer.writeEndSequence();
      }

      if (errorType != null)
      {
        writer.writeInteger(TYPE_ERROR_ELEMENT, errorType.intValue());
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
   * Returns the password policy warning type, if available.
   *
   * @return The password policy warning type, or {@code null} if this control
   *         does not contain a warning.
   */
  public PasswordPolicyWarningType getWarningType()
  {
    return warningType;
  }



  /**
   * Returns the password policy warning value, if available. The value is
   * undefined if this control does not contain a warning.
   *
   * @return The password policy warning value, or {@code -1} if this control
   *         does not contain a warning.
   */
  public int getWarningValue()
  {
    return warningValue;
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
    builder.append("PasswordPolicyResponseControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    if (warningType != null)
    {
      builder.append(", warningType=");
      builder.append(warningType);
      builder.append(", warningValue=");
      builder.append(warningValue);
    }
    if (errorType != null)
    {
      builder.append(", errorType=");
      builder.append(errorType);
    }
    builder.append(")");
    return builder.toString();
  }
}
