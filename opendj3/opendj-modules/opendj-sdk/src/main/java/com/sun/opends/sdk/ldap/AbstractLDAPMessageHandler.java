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



import java.io.IOException;

import org.opends.sdk.ByteString;
import org.opends.sdk.requests.*;
import org.opends.sdk.responses.*;



/**
 * Abstract LDAP message handler.
 *
 * @param <P>
 *          A user provided handler parameter.
 */
abstract class AbstractLDAPMessageHandler<P> implements LDAPMessageHandler<P>
{
  public void abandonRequest(final P param, final int messageID,
      final AbandonRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void addRequest(final P param, final int messageID,
      final AddRequest request) throws UnexpectedRequestException, IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void addResult(final P param, final int messageID, final Result result)
      throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void bindRequest(final P param, final int messageID,
      final int version, final GenericBindRequest request)
      throws UnexpectedRequestException, IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void bindResult(final P param, final int messageID,
      final BindResult result) throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void compareRequest(final P param, final int messageID,
      final CompareRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void compareResult(final P param, final int messageID,
      final CompareResult result) throws UnexpectedResponseException,
      IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void deleteRequest(final P param, final int messageID,
      final DeleteRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void deleteResult(final P param, final int messageID,
      final Result result) throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public <R extends ExtendedResult> void extendedRequest(final P param,
      final int messageID, final ExtendedRequest<R> request)
      throws UnexpectedRequestException, IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void extendedResult(final P param, final int messageID,
      final ExtendedResult result) throws UnexpectedResponseException,
      IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void intermediateResponse(final P param, final int messageID,
      final IntermediateResponse response) throws UnexpectedResponseException,
      IOException
  {
    throw new UnexpectedResponseException(messageID, response);
  }



  public void modifyDNRequest(final P param, final int messageID,
      final ModifyDNRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void modifyDNResult(final P param, final int messageID,
      final Result result) throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void modifyRequest(final P param, final int messageID,
      final ModifyRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void modifyResult(final P param, final int messageID,
      final Result result) throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void searchRequest(final P param, final int messageID,
      final SearchRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void searchResult(final P param, final int messageID,
      final Result result) throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, result);
  }



  public void searchResultEntry(final P param, final int messageID,
      final SearchResultEntry entry) throws UnexpectedResponseException,
      IOException
  {
    throw new UnexpectedResponseException(messageID, entry);
  }



  public void searchResultReference(final P param, final int messageID,
      final SearchResultReference reference)
      throws UnexpectedResponseException, IOException
  {
    throw new UnexpectedResponseException(messageID, reference);
  }



  public void unbindRequest(final P param, final int messageID,
      final UnbindRequest request) throws UnexpectedRequestException,
      IOException
  {
    throw new UnexpectedRequestException(messageID, request);
  }



  public void unrecognizedMessage(final P param, final int messageID,
      final byte messageTag, final ByteString messageBytes)
      throws UnsupportedMessageException, IOException
  {
    throw new UnsupportedMessageException(messageID, messageTag, messageBytes);
  }
}
