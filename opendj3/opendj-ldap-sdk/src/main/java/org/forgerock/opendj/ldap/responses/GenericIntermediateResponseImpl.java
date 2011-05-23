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

package org.forgerock.opendj.ldap.responses;



import org.forgerock.opendj.ldap.ByteString;

import com.forgerock.opendj.util.StaticUtils;



/**
 * Generic intermediate response implementation.
 */
final class GenericIntermediateResponseImpl extends
    AbstractIntermediateResponse<GenericIntermediateResponse> implements
    GenericIntermediateResponse
{

  private String responseName = null;

  private ByteString responseValue = null;



  /**
   * Creates a new generic intermediate response using the provided response
   * name and value.
   *
   * @param responseName
   *          The dotted-decimal representation of the unique OID corresponding
   *          to this intermediate response, which may be {@code null}
   *          indicating that none was provided.
   * @param responseValue
   *          The response value associated with this generic intermediate
   *          response, which may be {@code null} indicating that none was
   *          provided.
   */
  GenericIntermediateResponseImpl(final String responseName,
      final ByteString responseValue)
  {
    this.responseName = responseName;
    this.responseValue = responseValue;
  }



  /**
   * Creates a new generic intermediate response that is an exact copy of the
   * provided result.
   *
   * @param genericIntermediateResponse
   *          The generic intermediate response to be copied.
   * @throws NullPointerException
   *           If {@code genericExtendedResult} was {@code null} .
   */
  GenericIntermediateResponseImpl(
      final GenericIntermediateResponse genericIntermediateResponse)
      throws NullPointerException
  {
    super(genericIntermediateResponse);
    this.responseName = genericIntermediateResponse.getOID();
    this.responseValue = genericIntermediateResponse.getValue();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getOID()
  {
    return responseName;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ByteString getValue()
  {
    return responseValue;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasValue()
  {
    return responseValue != null;
  }



  /**
   * {@inheritDoc}
   */
  public GenericIntermediateResponse setOID(final String oid)
      throws UnsupportedOperationException
  {
    this.responseName = oid;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public GenericIntermediateResponse setValue(final ByteString bytes)
      throws UnsupportedOperationException
  {
    this.responseValue = bytes;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("GenericIntermediateResponse(responseName=");
    builder.append(getOID() == null ? "" : getOID());
    if (hasValue())
    {
      builder.append(", requestValue=");
      StaticUtils.toHexPlusAscii(getValue(), builder, 4);
    }
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }
}
