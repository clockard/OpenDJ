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

package com.forgerock.opendj.ldap.extensions;



import java.io.IOException;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.asn1.ASN1;
import org.forgerock.opendj.asn1.ASN1Reader;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.requests.AbstractExtendedRequest;
import org.forgerock.opendj.ldap.requests.ExtendedRequest;
import org.forgerock.opendj.ldap.requests.ExtendedRequestDecoder;
import org.forgerock.opendj.ldap.responses.AbstractExtendedResultDecoder;
import org.forgerock.opendj.ldap.responses.ExtendedResult;
import org.forgerock.opendj.ldap.responses.ExtendedResultDecoder;



/**
 * Get connection ID extended request. This operation can be used to retrieve
 * the client connection ID.
 *
 * @see GetConnectionIDExtendedResult
 */
public final class GetConnectionIDExtendedRequest
    extends
    AbstractExtendedRequest<GetConnectionIDExtendedRequest, GetConnectionIDExtendedResult>
{
  private static final class RequestDecoder
      implements
      ExtendedRequestDecoder<GetConnectionIDExtendedRequest, GetConnectionIDExtendedResult>
  {

    public GetConnectionIDExtendedRequest decodeExtendedRequest(
        final ExtendedRequest<?> request, final DecodeOptions options)
        throws DecodeException
    {
      // TODO: Check the OID and that the value is not present.
      final GetConnectionIDExtendedRequest newRequest = new GetConnectionIDExtendedRequest();
      for (final Control control : request.getControls())
      {
        newRequest.addControl(control);
      }
      return newRequest;

    }
  }



  private static final class ResultDecoder extends
      AbstractExtendedResultDecoder<GetConnectionIDExtendedResult>
  {
    /**
     * {@inheritDoc}
     */
    public GetConnectionIDExtendedResult newExtendedErrorResult(
        final ResultCode resultCode, final String matchedDN,
        final String diagnosticMessage)
    {
      if (!resultCode.isExceptional())
      {
        // A successful response must contain a response name and
        // value.
        throw new IllegalArgumentException(
            "No response name and value for result code "
                + resultCode.intValue());
      }
      return GetConnectionIDExtendedResult.newResult(resultCode, -1)
          .setMatchedDN(matchedDN).setDiagnosticMessage(diagnosticMessage);
    }



    public GetConnectionIDExtendedResult decodeExtendedResult(
        final ExtendedResult result, final DecodeOptions options)
        throws DecodeException
    {
      if (result instanceof GetConnectionIDExtendedResult)
      {
        return (GetConnectionIDExtendedResult) result;
      }
      else
      {
        final ResultCode resultCode = result.getResultCode();
        final ByteString responseValue = result.getValue();

        if (!resultCode.isExceptional()
            && ((responseValue == null) || (responseValue.length() <= 0)))
        {
          throw DecodeException.error(LocalizableMessage
              .raw("Empty response value"));
        }

        try
        {
          final ASN1Reader reader = ASN1.getReader(responseValue);
          final int connectionID = (int) reader.readInteger();
          final GetConnectionIDExtendedResult newResult = GetConnectionIDExtendedResult
              .newResult(resultCode, connectionID).setMatchedDN(
                  result.getMatchedDN()).setDiagnosticMessage(
                  result.getDiagnosticMessage());

          for (final Control control : result.getControls())
          {
            newResult.addControl(control);
          }

          return newResult;
        }
        catch (final IOException e)
        {
          throw DecodeException.error(LocalizableMessage
              .raw("Error decoding response value"), e);
        }
      }
    }
  }



  /**
   * The OID for the extended operation that can be used to get the client
   * connection ID. It will be both the request and response OID.
   */
  public static final String OID = "1.3.6.1.4.1.26027.1.6.2";

  // Singleton.
  private static final GetConnectionIDExtendedRequest INSTANCE =
    new GetConnectionIDExtendedRequest();

  /**
   * A decoder which can be used to decode get connection ID extended operation
   * requests.
   */
  public static final RequestDecoder REQUEST_DECODER = new RequestDecoder();

  // No need to expose this.
  private static final ResultDecoder RESULT_DECODER = new ResultDecoder();



  /**
   * Creates a new get connection ID extended request.
   *
   * @return The new get connection ID extended request.
   */
  public static GetConnectionIDExtendedRequest newRequest()
  {
    return INSTANCE;
  }



  // Prevent instantiation.
  private GetConnectionIDExtendedRequest()
  {
    // Nothing to do.
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getOID()
  {
    return OID;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ExtendedResultDecoder<GetConnectionIDExtendedResult> getResultDecoder()
  {
    return RESULT_DECODER;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ByteString getValue()
  {
    return null;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasValue()
  {
    return false;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("GetConnectionIDExtendedRequest(requestName=");
    builder.append(getOID());
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }
}
