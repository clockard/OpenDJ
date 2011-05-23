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
package com.forgerock.opendj.ldap.controls;



import static org.opends.sdk.CoreMessages.ERR_ACCTUSABLEREQ_CONTROL_BAD_OID;
import static org.opends.sdk.CoreMessages.ERR_ACCTUSABLEREQ_CONTROL_HAS_VALUE;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.sdk.ByteString;
import org.opends.sdk.DecodeException;
import org.opends.sdk.DecodeOptions;
import org.opends.sdk.controls.Control;
import org.opends.sdk.controls.ControlDecoder;

import com.forgerock.opendj.util.Validator;



/**
 * The Sun-defined account usability request control. The OID for this control
 * is 1.3.6.1.4.1.42.2.27.9.5.8, and it does not have a value.
 *
 * @see AccountUsabilityResponseControl
 */
public final class AccountUsabilityRequestControl implements Control
{
  /**
   * The OID for the account usability request control.
   */
  public static final String OID = "1.3.6.1.4.1.42.2.27.9.5.8";

  private final boolean isCritical;

  private static final AccountUsabilityRequestControl CRITICAL_INSTANCE =
    new AccountUsabilityRequestControl(true);
  private static final AccountUsabilityRequestControl NONCRITICAL_INSTANCE =
    new AccountUsabilityRequestControl(false);

  /**
   * A decoder which can be used for decoding the account usability request
   * control.
   */
  public static final ControlDecoder<AccountUsabilityRequestControl> DECODER =
    new ControlDecoder<AccountUsabilityRequestControl>()
  {

    public AccountUsabilityRequestControl decodeControl(final Control control,
        final DecodeOptions options) throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof AccountUsabilityRequestControl)
      {
        return (AccountUsabilityRequestControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_ACCTUSABLEREQ_CONTROL_BAD_OID
            .get(control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (control.hasValue())
      {
        final LocalizableMessage message = ERR_ACCTUSABLEREQ_CONTROL_HAS_VALUE
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
   * Creates a new account usability request control having the provided
   * criticality.
   *
   * @param isCritical
   *          {@code true} if it is unacceptable to perform the operation
   *          without applying the semantics of this control, or {@code false}
   *          if it can be ignored.
   * @return The new control.
   */
  public static AccountUsabilityRequestControl newControl(
      final boolean isCritical)
  {
    return isCritical ? CRITICAL_INSTANCE : NONCRITICAL_INSTANCE;
  }



  // Prevent direct instantiation.
  private AccountUsabilityRequestControl(final boolean isCritical)
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
    builder.append("AccountUsableRequestControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(")");
    return builder.toString();
  }

}
