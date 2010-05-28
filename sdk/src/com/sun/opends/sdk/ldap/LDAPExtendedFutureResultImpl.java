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
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package com.sun.opends.sdk.ldap;



import org.opends.sdk.*;
import org.opends.sdk.requests.ExtendedRequest;
import org.opends.sdk.responses.ExtendedResult;



/**
 * Extended result future implementation.
 *
 * @param <R>
 *          The type of result returned by this future.
 */
final class LDAPExtendedFutureResultImpl<R extends ExtendedResult> extends
    AbstractLDAPFutureResultImpl<R> implements FutureResult<R>
{
  private final ExtendedRequest<R> request;



  LDAPExtendedFutureResultImpl(final int messageID,
      final ExtendedRequest<R> request,
      final ResultHandler<? super R> resultHandler,
      final IntermediateResponseHandler intermediateResponseHandler,
      final AsynchronousConnection connection)
  {
    super(messageID, resultHandler, intermediateResponseHandler, connection);
    this.request = request;
  }



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("LDAPExtendedFutureResultImpl(");
    sb.append("request = ");
    sb.append(request);
    super.toString(sb);
    sb.append(")");
    return sb.toString();
  }



  R decodeResult(final ExtendedResult result, final DecodeOptions options)
      throws DecodeException
  {
    return request.getResultDecoder().decodeExtendedResult(result, options);
  }



  ExtendedRequest<R> getRequest()
  {
    return request;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  R newErrorResult(final ResultCode resultCode, final String diagnosticMessage,
      final Throwable cause)
  {
    return request.getResultDecoder().adaptExtendedErrorResult(resultCode, "",
        diagnosticMessage);
  }
}
