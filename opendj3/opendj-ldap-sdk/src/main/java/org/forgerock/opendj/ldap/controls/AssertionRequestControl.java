/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.ldap.controls;



import static com.forgerock.opendj.util.StaticUtils.getExceptionMessage;
import static org.forgerock.opendj.ldap.CoreMessages.ERR_LDAPASSERT_CONTROL_BAD_OID;
import static org.forgerock.opendj.ldap.CoreMessages.ERR_LDAPASSERT_INVALID_CONTROL_VALUE;
import static org.forgerock.opendj.ldap.CoreMessages.ERR_LDAPASSERT_NO_CONTROL_VALUE;

import java.io.IOException;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.asn1.ASN1;
import org.forgerock.opendj.asn1.ASN1Reader;
import org.forgerock.opendj.asn1.ASN1Writer;
import org.forgerock.opendj.ldap.*;

import com.forgerock.opendj.ldap.LDAPUtils;
import com.forgerock.opendj.util.Validator;



/**
 * The assertion request control as defined in RFC 4528. The Assertion control
 * allows a client to specify that a directory operation should only be
 * processed if an assertion applied to the target entry of the operation is
 * true. It can be used to construct "test and set", "test and clear", and other
 * conditional operations.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4528">RFC 4528 - Lightweight
 *      Directory Access Protocol (LDAP) Assertion Control </a>
 */
public final class AssertionRequestControl implements Control
{
  /**
   * The IANA-assigned OID for the LDAP assertion request control.
   */
  public static final String OID = "1.3.6.1.1.12";

  /**
   * A decoder which can be used for decoding the LDAP assertion request
   * control.
   */
  public static final ControlDecoder<AssertionRequestControl> DECODER =
    new ControlDecoder<AssertionRequestControl>()
  {

    public AssertionRequestControl decodeControl(final Control control,
        final DecodeOptions options) throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof AssertionRequestControl)
      {
        return (AssertionRequestControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_LDAPASSERT_CONTROL_BAD_OID.get(
            control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (!control.hasValue())
      {
        // The response control must always have a value.
        final LocalizableMessage message = ERR_LDAPASSERT_NO_CONTROL_VALUE
            .get();
        throw DecodeException.error(message);
      }

      final ASN1Reader reader = ASN1.getReader(control.getValue());
      Filter filter;
      try
      {
        filter = LDAPUtils.decodeFilter(reader);
      }
      catch (final IOException e)
      {
        throw DecodeException.error(ERR_LDAPASSERT_INVALID_CONTROL_VALUE
            .get(getExceptionMessage(e)), e);
      }

      return new AssertionRequestControl(control.isCritical(), filter);
    }



    public String getOID()
    {
      return OID;
    }
  };



  /**
   * Creates a new assertion using the provided criticality and assertion
   * filter.
   *
   * @param isCritical
   *          {@code true} if it is unacceptable to perform the operation
   *          without applying the semantics of this control, or {@code false}
   *          if it can be ignored.
   * @param filter
   *          The assertion filter.
   * @return The new control.
   * @throws NullPointerException
   *           If {@code filter} was {@code null}.
   */
  public static AssertionRequestControl newControl(final boolean isCritical,
      final Filter filter) throws NullPointerException
  {
    return new AssertionRequestControl(isCritical, filter);
  }



  // The assertion filter.
  private final Filter filter;

  private final boolean isCritical;



  // Prevent direct instantiation.
  private AssertionRequestControl(final boolean isCritical, final Filter filter)
  {
    Validator.ensureNotNull(filter);
    this.isCritical = isCritical;
    this.filter = filter;
  }



  /**
   * Returns the assertion filter.
   *
   * @return The assertion filter.
   */
  public Filter getFilter()
  {
    return filter;
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
      LDAPUtils.encodeFilter(writer, filter);
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
    builder.append("AssertionRequestControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(", filter=\"");
    builder.append(filter.toString());
    builder.append("\")");
    return builder.toString();
  }
}
