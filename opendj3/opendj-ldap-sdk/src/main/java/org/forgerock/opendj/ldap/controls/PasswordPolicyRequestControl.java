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
package org.forgerock.opendj.ldap.controls;



import static org.forgerock.opendj.ldap.CoreMessages.ERR_PWPOLICYREQ_CONTROL_BAD_OID;
import static org.forgerock.opendj.ldap.CoreMessages.ERR_PWPOLICYREQ_CONTROL_HAS_VALUE;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;

import com.forgerock.opendj.util.Validator;



/**
 * The password policy request control as defined in
 * draft-behera-ldap-password-policy.
 * <p>
 * This control may be sent with any request in order to convey to the server
 * that this client is aware of, and can process the password policy response
 * control. When a server receives this control, it will return the password
 * policy response control when appropriate and with the proper data.
 *
 * @see PasswordPolicyResponseControl
 * @see <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy">
 *         draft-behera-ldap-password-policy - Password Policy for LDAP Directories </a>
 */
public final class PasswordPolicyRequestControl implements Control
{
  /**
   * The OID for the password policy control from
   * draft-behera-ldap-password-policy.
   */
  public static final String OID = "1.3.6.1.4.1.42.2.27.8.5.1";

  private final boolean isCritical;

  private static final PasswordPolicyRequestControl CRITICAL_INSTANCE =
    new PasswordPolicyRequestControl(true);
  private static final PasswordPolicyRequestControl NONCRITICAL_INSTANCE =
    new PasswordPolicyRequestControl(false);

  /**
   * A decoder which can be used for decoding the password policy request
   * control.
   */
  public static final ControlDecoder<PasswordPolicyRequestControl> DECODER =
    new ControlDecoder<PasswordPolicyRequestControl>()
  {

    public PasswordPolicyRequestControl decodeControl(final Control control,
        final DecodeOptions options) throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof PasswordPolicyRequestControl)
      {
        return (PasswordPolicyRequestControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_PWPOLICYREQ_CONTROL_BAD_OID.get(
            control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (control.hasValue())
      {
        final LocalizableMessage message = ERR_PWPOLICYREQ_CONTROL_HAS_VALUE
            .get();
        throw DecodeException.error(message);
      }

      return control.isCritical() ? CRITICAL_INSTANCE : NONCRITICAL_INSTANCE;
    }



    public String getOID()
    {
      return OID;
    }
  };



  /**
   * Creates a new password policy request control having the provided
   * criticality.
   *
   * @param isCritical
   *          {@code true} if it is unacceptable to perform the operation
   *          without applying the semantics of this control, or {@code false}
   *          if it can be ignored.
   * @return The new control.
   */
  public static PasswordPolicyRequestControl newControl(final boolean isCritical)
  {
    return isCritical ? CRITICAL_INSTANCE : NONCRITICAL_INSTANCE;
  }



  private PasswordPolicyRequestControl(final boolean isCritical)
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
    return null;
  }



  /**
   * {@inheritDoc}
   */
  public boolean hasValue()
  {
    return false;
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
    builder.append("PasswordPolicyRequestControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(")");
    return builder.toString();
  }

}
