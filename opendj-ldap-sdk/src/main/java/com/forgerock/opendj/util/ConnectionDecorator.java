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
 *      Copyright 2010 Sun Microsystems, Inc.
 *      Portions copyright 2011-2012 ForgeRock AS
 */

package com.forgerock.opendj.util;



import java.util.Collection;

import org.forgerock.opendj.ldap.*;
import org.forgerock.opendj.ldap.requests.*;
import org.forgerock.opendj.ldap.responses.*;
import org.forgerock.opendj.ldif.ConnectionEntryReader;



/**
 * A base class from which connection decorators may be easily implemented. The
 * default implementation of each method is to delegate to the decorated
 * connection.
 */
public abstract class ConnectionDecorator implements Connection
{
  /**
   * The decorated connection.
   */
  protected final Connection connection;



  /**
   * Creates a new connection decorator.
   *
   * @param connection
   *          The connection to be decorated.
   */
  protected ConnectionDecorator(final Connection connection)
  {
    Validator.ensureNotNull(connection);
    this.connection = connection;
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<Void> abandonAsync(final AbandonRequest request)
  {
    return connection.abandonAsync(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result add(final AddRequest request) throws ErrorResultException,
      InterruptedException
  {
    return connection.add(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result add(final Entry entry) throws ErrorResultException,
      InterruptedException
  {
    return connection.add(entry);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result add(final String... ldifLines) throws ErrorResultException,
      InterruptedException
  {
    return connection.add(ldifLines);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<Result> addAsync(final AddRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super Result> resultHandler)
  {
    return connection.addAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public void addConnectionEventListener(final ConnectionEventListener listener)
  {
    connection.addConnectionEventListener(listener);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public BindResult bind(final BindRequest request)
      throws ErrorResultException, InterruptedException
  {
    return connection.bind(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public BindResult bind(final String name, final char[] password)
      throws ErrorResultException, InterruptedException
  {
    return connection.bind(name, password);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<BindResult> bindAsync(final BindRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super BindResult> resultHandler)
  {
    return connection.bindAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public void close()
  {
    connection.close();
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public void close(final UnbindRequest request, final String reason)
  {
    connection.close(request, reason);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public CompareResult compare(final CompareRequest request)
      throws ErrorResultException, InterruptedException
  {
    return connection.compare(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public CompareResult compare(final String name,
      final String attributeDescription, final String assertionValue)
      throws ErrorResultException, InterruptedException
  {
    return connection.compare(name, attributeDescription, assertionValue);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<CompareResult> compareAsync(final CompareRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super CompareResult> resultHandler)
  {
    return connection.compareAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result delete(final DeleteRequest request)
      throws ErrorResultException, InterruptedException
  {
    return connection.delete(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result delete(final String name) throws ErrorResultException,
      InterruptedException
  {
    return connection.delete(name);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<Result> deleteAsync(final DeleteRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super Result> resultHandler)
  {
    return connection.deleteAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public <R extends ExtendedResult> R extendedRequest(
      final ExtendedRequest<R> request) throws ErrorResultException,
      InterruptedException
  {
    return connection.extendedRequest(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public <R extends ExtendedResult> R extendedRequest(
      final ExtendedRequest<R> request,
      final IntermediateResponseHandler handler) throws ErrorResultException,
      InterruptedException
  {
    return connection.extendedRequest(request, handler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public GenericExtendedResult extendedRequest(final String requestName,
      final ByteString requestValue) throws ErrorResultException,
      InterruptedException
  {
    return connection.extendedRequest(requestName, requestValue);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public <R extends ExtendedResult> FutureResult<R> extendedRequestAsync(
      final ExtendedRequest<R> request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super R> resultHandler)
  {
    return connection.extendedRequestAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public boolean isClosed()
  {
    return connection.isClosed();
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public boolean isValid()
  {
    return connection.isValid();
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result modify(final ModifyRequest request)
      throws ErrorResultException, InterruptedException
  {
    return connection.modify(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result modify(final String... ldifLines) throws ErrorResultException,
      InterruptedException
  {
    return connection.modify(ldifLines);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<Result> modifyAsync(final ModifyRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super Result> resultHandler)
  {
    return connection.modifyAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result modifyDN(final ModifyDNRequest request)
      throws ErrorResultException, InterruptedException
  {
    return connection.modifyDN(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result modifyDN(final String name, final String newRDN)
      throws ErrorResultException, InterruptedException
  {
    return connection.modifyDN(name, newRDN);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<Result> modifyDNAsync(final ModifyDNRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final ResultHandler<? super Result> resultHandler)
  {
    return connection.modifyDNAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public SearchResultEntry readEntry(final DN name,
      final String... attributeDescriptions) throws ErrorResultException,
      InterruptedException
  {
    return connection.readEntry(name, attributeDescriptions);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public SearchResultEntry readEntry(final String name,
      final String... attributeDescriptions) throws ErrorResultException,
      InterruptedException
  {
    return connection.readEntry(name, attributeDescriptions);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<SearchResultEntry> readEntryAsync(final DN name,
      final Collection<String> attributeDescriptions,
      final ResultHandler<? super SearchResultEntry> handler)
  {
    return connection.readEntryAsync(name, attributeDescriptions, handler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public void removeConnectionEventListener(
      final ConnectionEventListener listener)
  {
    connection.removeConnectionEventListener(listener);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public ConnectionEntryReader search(final SearchRequest request)
  {
    return connection.search(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result search(final SearchRequest request,
      final Collection<? super SearchResultEntry> entries)
      throws ErrorResultException, InterruptedException
  {
    return connection.search(request, entries);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result search(final SearchRequest request,
      final Collection<? super SearchResultEntry> entries,
      final Collection<? super SearchResultReference> references)
      throws ErrorResultException, InterruptedException
  {
    return connection.search(request, entries, references);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public Result search(final SearchRequest request,
      final SearchResultHandler handler) throws ErrorResultException,
      InterruptedException
  {
    return connection.search(request, handler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public ConnectionEntryReader search(final String baseObject,
      final SearchScope scope, final String filter,
      final String... attributeDescriptions)
  {
    return connection.search(baseObject, scope, filter, attributeDescriptions);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<Result> searchAsync(final SearchRequest request,
      final IntermediateResponseHandler intermediateResponseHandler,
      final SearchResultHandler resultHandler)
  {
    return connection.searchAsync(request, intermediateResponseHandler,
        resultHandler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public SearchResultEntry searchSingleEntry(final SearchRequest request)
      throws ErrorResultException, InterruptedException
  {
    return connection.searchSingleEntry(request);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public SearchResultEntry searchSingleEntry(final String baseObject,
      final SearchScope scope, final String filter,
      final String... attributeDescriptions) throws ErrorResultException,
      InterruptedException
  {
    return connection.searchSingleEntry(baseObject, scope, filter,
        attributeDescriptions);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public FutureResult<SearchResultEntry> searchSingleEntryAsync(
      final SearchRequest request,
      final ResultHandler<? super SearchResultEntry> handler)
  {
    return connection.searchSingleEntryAsync(request, handler);
  }



  /**
   * {@inheritDoc}
   * <p>
   * The default implementation is to delegate.
   */
  @Override
  public String toString()
  {
    return connection.toString();
  }

}