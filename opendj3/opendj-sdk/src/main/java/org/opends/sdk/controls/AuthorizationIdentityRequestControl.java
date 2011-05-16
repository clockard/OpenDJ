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



import static org.opends.sdk.CoreMessages.ERR_AUTHZIDREQ_CONTROL_BAD_OID;
import static org.opends.sdk.CoreMessages.ERR_AUTHZIDREQ_CONTROL_HAS_VALUE;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.sdk.ByteString;
import org.opends.sdk.DecodeException;
import org.opends.sdk.DecodeOptions;

import com.sun.opends.sdk.util.Validator;



/**
 * The authorization request control as defined in RFC 3829. The authorization
 * identity control extends the Lightweight Directory Access Protocol (LDAP)
 * bind operation with a mechanism for requesting and returning the
 * authorization identity it establishes.
 *
 * @see AuthorizationIdentityResponseControl
 * @see org.opends.sdk.requests.WhoAmIExtendedRequest
 * @see <a href="http://tools.ietf.org/html/rfc3829">RFC 3829 - Lightweight
 *      Directory Access Protocol (LDAP) Authorization Identity Request and
 *      Response Controls </a>
 * @see <a href="http://tools.ietf.org/html/rfc4532">RFC 4532 - Lightweight
 *      Directory Access Protocol (LDAP) "Who am I?" Operation </a>
 */
public final class AuthorizationIdentityRequestControl implements Control
{
  /**
   * The OID for the authorization identity request control.
   */
  public static final String OID = "2.16.840.1.113730.3.4.16";

  private final boolean isCritical;

  private static final AuthorizationIdentityRequestControl CRITICAL_INSTANCE =
    new AuthorizationIdentityRequestControl(true);

  private static final AuthorizationIdentityRequestControl
    NONCRITICAL_INSTANCE= new AuthorizationIdentityRequestControl(false);

  /**
   * A decoder which can be used for decoding the authorization identity request
   * control.
   */
  public static final ControlDecoder<AuthorizationIdentityRequestControl>
    DECODER = new ControlDecoder<AuthorizationIdentityRequestControl>()
  {

    public AuthorizationIdentityRequestControl decodeControl(
        final Control control, final DecodeOptions options)
        throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof AuthorizationIdentityRequestControl)
      {
        return (AuthorizationIdentityRequestControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_AUTHZIDREQ_CONTROL_BAD_OID.get(
            control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (control.hasValue())
      {
        final LocalizableMessage message = ERR_AUTHZIDREQ_CONTROL_HAS_VALUE
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
   * Creates a new authorization identity request control having the provided
   * criticality.
   *
   * @param isCritical
   *          {@code true} if it is unacceptable to perform the operation
   *          without applying the semantics of this control, or {@code false}
   *          if it can be ignored.
   * @return The new control.
   */
  public static AuthorizationIdentityRequestControl newControl(
      final boolean isCritical)
  {
    return isCritical ? CRITICAL_INSTANCE : NONCRITICAL_INSTANCE;
  }



  // Prevent direct instantiation.
  private AuthorizationIdentityRequestControl(final boolean isCritical)
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
    builder.append("AuthorizationIdentityRequestControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(")");
    return builder.toString();
  }

}
