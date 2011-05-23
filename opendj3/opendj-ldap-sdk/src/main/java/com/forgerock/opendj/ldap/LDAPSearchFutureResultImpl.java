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
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 */

package com.forgerock.opendj.ldap;



import org.forgerock.opendj.ldap.AsynchronousConnection;
import org.forgerock.opendj.ldap.IntermediateResponseHandler;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Responses;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;



/**
 * Search result future implementation.
 */
final class LDAPSearchFutureResultImpl extends
    AbstractLDAPFutureResultImpl<Result> implements SearchResultHandler
{

  private SearchResultHandler searchResultHandler;

  private final SearchRequest request;



  LDAPSearchFutureResultImpl(final int requestID, final SearchRequest request,
      final SearchResultHandler resultHandler,
      final IntermediateResponseHandler intermediateResponseHandler,
      final AsynchronousConnection connection)
  {
    super(requestID, resultHandler, intermediateResponseHandler, connection);
    this.request = request;
    this.searchResultHandler = resultHandler;
  }



  public boolean handleEntry(final SearchResultEntry entry)
  {
    // FIXME: there's a potential race condition here - the future could
    // get cancelled between the isDone() call and the handler
    // invocation. We'd need to add support for intermediate handlers in
    // the synchronizer.
    if (!isDone())
    {
      updateTimestamp();
      if (searchResultHandler != null)
      {
        if (!searchResultHandler.handleEntry(entry))
        {
          searchResultHandler = null;
        }
      }
    }
    return true;
  }



  public boolean handleReference(final SearchResultReference reference)
  {
    // FIXME: there's a potential race condition here - the future could
    // get cancelled between the isDone() call and the handler
    // invocation. We'd need to add support for intermediate handlers in
    // the synchronizer.
    if (!isDone())
    {
      updateTimestamp();
      if (searchResultHandler != null)
      {
        if (!searchResultHandler.handleReference(reference))
        {
          searchResultHandler = null;
        }
      }
    }
    return true;
  }



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("LDAPSearchFutureResultImpl(");
    sb.append("request = ");
    sb.append(request);
    super.toString(sb);
    sb.append(")");
    return sb.toString();
  }



  SearchRequest getRequest()
  {
    return request;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  Result newErrorResult(final ResultCode resultCode,
      final String diagnosticMessage, final Throwable cause)
  {
    return Responses.newResult(resultCode).setDiagnosticMessage(
        diagnosticMessage).setCause(cause);
  }
}
