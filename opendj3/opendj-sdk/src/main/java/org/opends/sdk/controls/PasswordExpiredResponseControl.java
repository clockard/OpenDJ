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



import static org.opends.sdk.CoreMessages.ERR_PWEXPIRED_CONTROL_BAD_OID;
import static org.opends.sdk.CoreMessages.ERR_PWEXPIRED_CONTROL_INVALID_VALUE;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.sdk.ByteString;
import org.opends.sdk.DecodeException;
import org.opends.sdk.DecodeOptions;

import com.sun.opends.sdk.util.Validator;



/**
 * The Netscape password expired response control as defined in
 * draft-vchu-ldap-pwd-policy. This control indicates to a client that their
 * password has expired and must be changed. This control always has a value
 * which is the string {@code "0"}.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-vchu-ldap-pwd-policy">draft-vchu-ldap-pwd-policy
 *      - Password Policy for LDAP Directories </a>
 */
public final class PasswordExpiredResponseControl implements Control
{
  /**
   * The OID for the Netscape password expired response control.
   */
  public static final String OID = "2.16.840.1.113730.3.4.4";

  private final boolean isCritical;

  private static final PasswordExpiredResponseControl CRITICAL_INSTANCE =
    new PasswordExpiredResponseControl(true);
  private static final PasswordExpiredResponseControl NONCRITICAL_INSTANCE =
    new PasswordExpiredResponseControl(false);

  /**
   * A decoder which can be used for decoding the password expired response
   * control.
   */
  public static final ControlDecoder<PasswordExpiredResponseControl> DECODER =
    new ControlDecoder<PasswordExpiredResponseControl>()
  {

    public PasswordExpiredResponseControl decodeControl(final Control control,
        final DecodeOptions options) throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof PasswordExpiredResponseControl)
      {
        return (PasswordExpiredResponseControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_PWEXPIRED_CONTROL_BAD_OID.get(
            control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (control.hasValue())
      {
        try
        {
          Integer.parseInt(control.getValue().toString());
        }
        catch (final Exception e)
        {
          final LocalizableMessage message = ERR_PWEXPIRED_CONTROL_INVALID_VALUE
              .get();
          throw DecodeException.error(message);
        }
      }

      return control.isCritical() ? CRITICAL_INSTANCE : NONCRITICAL_INSTANCE;
    }



    public String getOID()
    {
      return OID;
    }
  };

  private final static ByteString CONTROL_VALUE = ByteString.valueOf("0");



  /**
   * Creates a new Netscape password expired response control.
   *
   * @return The new control.
   */
  public static PasswordExpiredResponseControl newControl()
  {
    return NONCRITICAL_INSTANCE;
  }



  private PasswordExpiredResponseControl(final boolean isCritical)
  {
    this.isCritical = isCritical;
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
    return CONTROL_VALUE;
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
    builder.append("PasswordExpiredResponseControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(")");
    return builder.toString();
  }
}
