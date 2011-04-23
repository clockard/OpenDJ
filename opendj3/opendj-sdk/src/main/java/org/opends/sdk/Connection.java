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

package org.opends.sdk;



import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import org.opends.sdk.ldif.ConnectionEntryReader;
import org.opends.sdk.requests.*;
import org.opends.sdk.responses.*;
import org.opends.sdk.schema.Schema;



/**
 * A synchronous connection with a Directory Server over which read and update
 * operations may be performed. See RFC 4511 for the LDAPv3 protocol
 * specification and more information about the types of operations defined in
 * LDAP.
 * <p>
 * <h3>Operation processing</h3>
 * <p>
 * All operations are performed synchronously and return an appropriate
 * {@link Result} representing the final status of the operation. Operation
 * failures, for whatever reason, are signalled using an
 * {@link ErrorResultException}.
 * <p>
 * <h3>Closing connections</h3>
 * <p>
 * Applications must ensure that a connection is closed by calling
 * {@link #close()} even if a fatal error occurs on the connection. Once a
 * connection has been closed by the client application, any attempts to
 * continue to use the connection will result in an
 * {@link IllegalStateException} being thrown. Note that, if a fatal error is
 * encountered on the connection, then the application can continue to use the
 * connection. In this case all requests subsequent to the failure will fail
 * with an appropriate {@link ErrorResultException} when their result is
 * retrieved.
 * <p>
 * <h3>Event notification</h3>
 * <p>
 * Applications can choose to be notified when a connection is closed by the
 * application, receives an unsolicited notification, or experiences a fatal
 * error by registering a {@link ConnectionEventListener} with the connection
 * using the {@link #addConnectionEventListener} method.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4511">RFC 4511 - Lightweight
 *      Directory Access Protocol (LDAP): The Protocol </a>
 */
public interface Connection extends Closeable
{

  /**
   * Adds an entry to the Directory Server using the provided add request.
   *
   * @param request
   *          The add request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support add operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  Result add(AddRequest request) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Adds the provided entry to the Directory Server.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * AddRequest request = new AddRequest(entry);
   * connection.add(request);
   * </pre>
   *
   * @param entry
   *          The entry to be added.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support add operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code entry} was {@code null} .
   */
  Result add(Entry entry) throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Adds an entry to the Directory Server using the provided lines of LDIF.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * AddRequest request = new AddRequest(ldifLines);
   * connection.add(request);
   * </pre>
   *
   * @param ldifLines
   *          Lines of LDIF containing the an LDIF add change record or an LDIF
   *          entry record.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support add operations.
   * @throws LocalizedIllegalArgumentException
   *           If {@code ldifLines} was empty, or contained invalid LDIF, or
   *           could not be decoded using the default schema.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code ldifLines} was {@code null} .
   */
  Result add(String... ldifLines) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      LocalizedIllegalArgumentException, IllegalStateException,
      NullPointerException;



  /**
   * Registers the provided connection event listener so that it will be
   * notified when this connection is closed by the application, receives an
   * unsolicited notification, or experiences a fatal error.
   *
   * @param listener
   *          The listener which wants to be notified when events occur on this
   *          connection.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If the {@code listener} was {@code null}.
   */
  void addConnectionEventListener(ConnectionEventListener listener)
      throws IllegalStateException, NullPointerException;



  /**
   * Authenticates to the Directory Server using the provided bind request.
   *
   * @param request
   *          The bind request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support bind operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  BindResult bind(BindRequest request) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Authenticates to the Directory Server using simple authentication and the
   * provided user name and password.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * BindRequest request = new SimpleBindRequest(name, password);
   * connection.bind(request);
   * </pre>
   *
   * @param name
   *          The distinguished name of the Directory object that the client
   *          wishes to bind as, which may be empty.
   * @param password
   *          The password of the Directory object that the client wishes to
   *          bind as, which may be empty.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} could not be decoded using the default schema.
   * @throws UnsupportedOperationException
   *           If this connection does not support bind operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code name} or {@code password} was {@code null}.
   */
  BindResult bind(String name, char[] password) throws ErrorResultException,
      InterruptedException, LocalizedIllegalArgumentException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Releases any resources associated with this connection. For physical
   * connections to a Directory Server this will mean that an unbind request is
   * sent and the underlying socket is closed.
   * <p>
   * Other connection implementations may behave differently, and may choose not
   * to send an unbind request if its use is inappropriate (for example a pooled
   * connection will be released and returned to its connection pool without
   * ever issuing an unbind request).
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * UnbindRequest request = new UnbindRequest();
   * connection.close(request);
   * </pre>
   *
   * Calling {@code close} on a connection that is already closed has no effect.
   */
  void close();



  /**
   * Releases any resources associated with this connection. For physical
   * connections to a Directory Server this will mean that the provided unbind
   * request is sent and the underlying socket is closed.
   * <p>
   * Other connection implementations may behave differently, and may choose to
   * ignore the provided unbind request if its use is inappropriate (for example
   * a pooled connection will be released and returned to its connection pool
   * without ever issuing an unbind request).
   * <p>
   * Calling {@code close} on a connection that is already closed has no effect.
   *
   * @param request
   *          The unbind request to use in the case where a physical connection
   *          is closed.
   * @param reason
   *          A reason describing why the connection was closed.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  void close(UnbindRequest request, String reason) throws NullPointerException;



  /**
   * Compares an entry in the Directory Server using the provided compare
   * request.
   *
   * @param request
   *          The compare request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support compare operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  CompareResult compare(CompareRequest request) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Compares the named entry in the Directory Server against the provided
   * attribute value assertion.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * CompareRequest request = new CompareRequest(name, attributeDescription,
   *     assertionValue);
   * connection.compare(request);
   * </pre>
   *
   * @param name
   *          The distinguished name of the entry to be compared.
   * @param attributeDescription
   *          The name of the attribute to be compared.
   * @param assertionValue
   *          The assertion value to be compared.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} or {@code AttributeDescription} could not be
   *           decoded using the default schema.
   * @throws UnsupportedOperationException
   *           If this connection does not support compare operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code name}, {@code attributeDescription}, or {@code
   *           assertionValue} was {@code null}.
   */
  CompareResult compare(String name, String attributeDescription,
      String assertionValue) throws ErrorResultException, InterruptedException,
      LocalizedIllegalArgumentException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Deletes an entry from the Directory Server using the provided delete
   * request.
   *
   * @param request
   *          The delete request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support delete operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  Result delete(DeleteRequest request) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Deletes the named entry from the Directory Server.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * DeleteRequest request = new DeleteRequest(name);
   * connection.delete(request);
   * </pre>
   *
   * @param name
   *          The distinguished name of the entry to be deleted.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} could not be decoded using the default schema.
   * @throws UnsupportedOperationException
   *           If this connection does not support delete operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code name} was {@code null}.
   */
  Result delete(String name) throws ErrorResultException, InterruptedException,
      LocalizedIllegalArgumentException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Requests that the Directory Server performs the provided extended request.
   *
   * @param <R>
   *          The type of result returned by the extended request.
   * @param request
   *          The extended request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support extended operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  <R extends ExtendedResult> R extendedRequest(ExtendedRequest<R> request)
      throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Requests that the Directory Server performs the provided extended request,
   * optionally listening for any intermediate responses.
   *
   * @param <R>
   *          The type of result returned by the extended request.
   * @param request
   *          The extended request.
   * @param handler
   *          An intermediate response handler which can be used to process any
   *          intermediate responses as they are received, may be {@code null}.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support extended operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  <R extends ExtendedResult> R extendedRequest(ExtendedRequest<R> request,
      IntermediateResponseHandler handler) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Requests that the Directory Server performs the provided extended request.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * GenericExtendedRequest request = new GenericExtendedRequest(requestName,
   *     requestValue);
   * connection.extendedRequest(request);
   * </pre>
   *
   * @param requestName
   *          The dotted-decimal representation of the unique OID corresponding
   *          to the extended request.
   * @param requestValue
   *          The content of the extended request in a form defined by the
   *          extended operation, or {@code null} if there is no content.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support extended operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code requestName} was {@code null}.
   */
  GenericExtendedResult extendedRequest(String requestName,
      ByteString requestValue) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Returns an asynchronous connection sharing the same underlying network
   * connection as this synchronous connection.
   *
   * @return An asynchronous connection sharing the same underlying network
   *         connection as this synchronous connection.
   */
  AsynchronousConnection getAsynchronousConnection();



  /**
   * Indicates whether or not this connection has been explicitly closed by
   * calling {@code close}. This method will not return {@code true} if a fatal
   * error has occurred on the connection unless {@code close} has been called.
   *
   * @return {@code true} if this connection has been explicitly closed by
   *         calling {@code close}, or {@code false} otherwise.
   */
  boolean isClosed();



  /**
   * Returns {@code true} if this connection has not been closed and no fatal
   * errors have been detected. This method is guaranteed to return {@code
   * false} only when it is called after the method {@code close} has been
   * called.
   *
   * @return {@code true} if this connection is valid, {@code false} otherwise.
   */
  boolean isValid();



  /**
   * Modifies an entry in the Directory Server using the provided modify
   * request.
   *
   * @param request
   *          The modify request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support modify operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  Result modify(ModifyRequest request) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Modifies an entry in the Directory Server using the provided lines of LDIF.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * ModifyRequest request = new ModifyRequest(name, ldifChanges);
   * connection.modify(request);
   * </pre>
   *
   * @param ldifLines
   *          Lines of LDIF containing the a single LDIF modify change record.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support modify operations.
   * @throws LocalizedIllegalArgumentException
   *           If {@code ldifLines} was empty, or contained invalid LDIF, or
   *           could not be decoded using the default schema.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code ldifLines} was {@code null} .
   */
  Result modify(String... ldifLines) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      LocalizedIllegalArgumentException, IllegalStateException,
      NullPointerException;



  /**
   * Renames an entry in the Directory Server using the provided modify DN
   * request.
   *
   * @param request
   *          The modify DN request.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support modify DN operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  Result modifyDN(ModifyDNRequest request) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Renames the named entry in the Directory Server using the provided new RDN.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * ModifyDNRequest request = new ModifyDNRequest(name, newRDN);
   * connection.modifyDN(request);
   * </pre>
   *
   * @param name
   *          The distinguished name of the entry to be renamed.
   * @param newRDN
   *          The new RDN of the entry.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} or {@code newRDN} could not be decoded using the
   *           default schema.
   * @throws UnsupportedOperationException
   *           If this connection does not support modify DN operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code name} or {@code newRDN} was {@code null}.
   */
  Result modifyDN(String name, String newRDN) throws ErrorResultException,
      LocalizedIllegalArgumentException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Reads the named entry from the Directory Server.
   * <p>
   * If the requested entry is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * SearchRequest request = new SearchRequest(name, SearchScope.BASE_OBJECT,
   *     &quot;(objectClass=*)&quot;, attributeDescriptions);
   * connection.searchSingleEntry(request);
   * </pre>
   *
   * @param name
   *          The distinguished name of the entry to be read.
   * @param attributeDescriptions
   *          The names of the attributes to be included with the entry, which
   *          may be {@code null} or empty indicating that all user attributes
   *          should be returned.
   * @return The single search result entry returned from the search.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If the {@code name} was {@code null}.
   */
  SearchResultEntry readEntry(DN name, String... attributeDescriptions)
      throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Reads the named entry from the Directory Server.
   * <p>
   * If the requested entry is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * SearchRequest request = new SearchRequest(name, SearchScope.BASE_OBJECT,
   *     &quot;(objectClass=*)&quot;, attributeDescriptions);
   * connection.searchSingleEntry(request);
   * </pre>
   *
   * @param name
   *          The distinguished name of the entry to be read.
   * @param attributeDescriptions
   *          The names of the attributes to be included with the entry.
   * @return The single search result entry returned from the search.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws LocalizedIllegalArgumentException
   *           If {@code baseObject} could not be decoded using the default
   *           schema.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If the {@code name} was {@code null}.
   */
  SearchResultEntry readEntry(String name, String... attributeDescriptions)
      throws ErrorResultException, InterruptedException,
      LocalizedIllegalArgumentException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;



  /**
   * Reads the Root DSE from the Directory Server.
   * <p>
   * If the Root DSE is not returned by the Directory Server then the request
   * will fail with an {@link EntryNotFoundException}. More specifically, this
   * method will never return {@code null}.
   *
   * @return The Directory Server's Root DSE.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   */
  RootDSE readRootDSE() throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException;



  /**
   * Reads the schema from the Directory Server contained in the named subschema
   * sub-entry.
   * <p>
   * If the requested schema is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}.
   * <p>
   * Implementations may choose to perform optimizations such as caching.
   *
   * @param name
   *          The distinguished name of the subschema sub-entry.
   * @return The schema from the Directory Server.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   */
  Schema readSchema(DN name) throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException;



  /**
   * Reads the schema from the Directory Server contained in the named subschema
   * sub-entry.
   * <p>
   * If the requested schema is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}.
   * <p>
   * Implementations may choose to perform optimizations such as caching.
   *
   * @param name
   *          The distinguished name of the subschema sub-entry.
   * @return The schema from the Directory Server.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} could not be decoded using the default schema.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   */
  Schema readSchema(String name) throws ErrorResultException,
      InterruptedException, LocalizedIllegalArgumentException,
      UnsupportedOperationException, IllegalStateException;



  /**
   * Reads the schema from the Directory Server which applies to the named
   * entry.
   * <p>
   * If the requested entry or its associated schema are not returned by the
   * Directory Server then the request will fail with an
   * {@link EntryNotFoundException}. More specifically, this method will never
   * return {@code null}.
   * <p>
   * A typical implementation will first read the {@code subschemaSubentry}
   * attribute of the entry in order to locate the schema. However,
   * implementations may choose to perform other optimizations, such as caching.
   *
   * @param name
   *          The distinguished name of the entry whose schema is to be located.
   * @return The schema from the Directory Server which applies to the named
   *         entry.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   */
  Schema readSchemaForEntry(DN name) throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException;



  /**
   * Reads the schema from the Directory Server which applies to the named
   * entry.
   * <p>
   * If the requested entry or its associated schema are not returned by the
   * Directory Server then the request will fail with an
   * {@link EntryNotFoundException}. More specifically, this method will never
   * return {@code null}.
   * <p>
   * A typical implementation will first read the {@code subschemaSubentry}
   * attribute of the entry in order to locate the schema. However,
   * implementations may choose to perform other optimizations, such as caching.
   *
   * @param name
   *          The distinguished name of the entry whose schema is to be located.
   * @return The schema from the Directory Server which applies to the named
   *         entry.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} could not be decoded using the default schema.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   */
  Schema readSchemaForEntry(String name) throws ErrorResultException,
      InterruptedException, LocalizedIllegalArgumentException,
      UnsupportedOperationException, IllegalStateException;



  /**
   * Reads the schema from the Directory Server which applies to the Root DSE.
   * <p>
   * If the requested schema is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}.
   * <p>
   * A typical implementation will first read the {@code subschemaSubentry}
   * attribute of the Root DSE in order to locate the schema. However,
   * implementations may choose to perform other optimizations, such as caching.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * connection.readSchemaForEntry(DN.rootDN());
   * </pre>
   *
   * @return The schema from the Directory Server which applies to the named
   *         entry.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   */
  Schema readSchemaForRootDSE() throws ErrorResultException,
      InterruptedException, UnsupportedOperationException,
      IllegalStateException;



  /**
   * Removes the provided connection event listener from this connection so that
   * it will no longer be notified when this connection is closed by the
   * application, receives an unsolicited notification, or experiences a fatal
   * error.
   *
   * @param listener
   *          The listener which no longer wants to be notified when events
   *          occur on this connection.
   * @throws NullPointerException
   *           If the {@code listener} was {@code null}.
   */
  void removeConnectionEventListener(ConnectionEventListener listener)
      throws NullPointerException;



  /**
   * Searches the Directory Server using the provided search request. Any
   * matching entries returned by the search will be added to {@code entries},
   * even if the final search result indicates that the search failed. Search
   * result references will be discarded.
   * <p>
   * <b>Warning:</b> Usage of this method is discouraged if the search request
   * is expected to yield a large number of search results since the entire set
   * of results will be stored in memory, potentially causing an {@code
   * OutOfMemoryError}.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * connection.search(request, entries, null);
   * </pre>
   *
   * @param request
   *          The search request.
   * @param entries
   *          The collection to which matching entries should be added.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} or {@code entries} was {@code null}.
   */
  Result search(SearchRequest request,
      Collection<? super SearchResultEntry> entries)
      throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Searches the Directory Server using the provided search request. Any
   * matching entries returned by the search will be added to {@code entries},
   * even if the final search result indicates that the search failed.
   * Similarly, search result references returned by the search will be added to
   * {@code references}.
   * <p>
   * <b>Warning:</b> Usage of this method is discouraged if the search request
   * is expected to yield a large number of search results since the entire set
   * of results will be stored in memory, potentially causing an {@code
   * OutOfMemoryError}.
   *
   * @param request
   *          The search request.
   * @param entries
   *          The collection to which matching entries should be added.
   * @param references
   *          The collection to which search result references should be added,
   *          or {@code null} if references are to be discarded.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} or {@code entries} was {@code null}.
   */
  Result search(SearchRequest request,
      Collection<? super SearchResultEntry> entries,
      Collection<? super SearchResultReference> references)
      throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Searches the Directory Server using the provided search request. Any
   * matching entries returned by the search as well as any search result
   * references will be passed to the provided search result handler.
   *
   * @param request
   *          The search request.
   * @param handler
   *          A search result handler which can be used to process the search
   *          result entries and references as they are received, may be {@code
   *          null}.
   * @return The result of the operation.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} was {@code null}.
   */
  Result search(SearchRequest request, SearchResultHandler handler)
      throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Searches the Directory Server using the provided search parameters. Any
   * matching entries returned by the search will be exposed through the
   * {@code EntryReader} interface.
   * <p>
   * <b>Warning:</b> When using a queue with an optional capacity bound,
   * the connection will stop reading responses and wait if necessary for
   * space to become available.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * SearchRequest request = new SearchRequest(baseDN, scope, filter,
   *     attributeDescriptions);
   * connection.search(request, new LinkedBlockingQueue&lt;Response&gt;());
   * </pre>
   *
   * @param baseObject
   *          The distinguished name of the base entry relative to which the
   *          search is to be performed.
   * @param scope
   *          The scope of the search.
   * @param filter
   *          The filter that defines the conditions that must be fulfilled in
   *          order for an entry to be returned.
   * @param attributeDescriptions
   *          The names of the attributes to be included with each entry.
   * @return An entry reader exposing the returned entries.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If the {@code baseObject}, {@code scope}, or {@code filter} were
   *           {@code null}.
   */
  ConnectionEntryReader search(String baseObject, SearchScope scope,
      String filter, String... attributeDescriptions)
      throws UnsupportedOperationException,
      IllegalStateException, NullPointerException;


  /**
   * Searches the Directory Server using the provided search parameters. Any
   * matching entries returned by the search will be exposed through the
   * {@code EntryReader} interface.
   * <p>
   * <b>Warning:</b> When using a queue with an optional capacity bound,
   * the connection will stop reading responses and wait if necessary for
   * space to become available.
   *
   * @param request
   *          The search request.
   * @param entries
   *          The queue to which matching entries should be added.
   * @return The result of the operation.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If {@code request} or {@code entries} was {@code null}.
   */
  ConnectionEntryReader search(SearchRequest request,
                               BlockingQueue<Response> entries)
      throws UnsupportedOperationException, IllegalStateException,
      NullPointerException;


  /**
   * Searches the Directory Server for a single entry using the provided search
   * request.
   * <p>
   * If the requested entry is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}. If multiple
   * matching entries are returned by the Directory Server then the request will
   * fail with an {@link MultipleEntriesFoundException}.
   *
   * @param request
   *          The search request.
   * @return The single search result entry returned from the search.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If the {@code request} was {@code null}.
   */
  SearchResultEntry searchSingleEntry(SearchRequest request)
      throws ErrorResultException, InterruptedException,
      UnsupportedOperationException, IllegalStateException,
      NullPointerException;



  /**
   * Searches the Directory Server for a single entry using the provided search
   * parameters.
   * <p>
   * If the requested entry is not returned by the Directory Server then the
   * request will fail with an {@link EntryNotFoundException}. More
   * specifically, this method will never return {@code null}. If multiple
   * matching entries are returned by the Directory Server then the request will
   * fail with an {@link MultipleEntriesFoundException}.
   * <p>
   * This method is equivalent to the following code:
   *
   * <pre>
   * SearchRequest request = new SearchRequest(baseObject, scope, filter,
   *     attributeDescriptions);
   * connection.searchSingleEntry(request);
   * </pre>
   *
   * @param baseObject
   *          The distinguished name of the base entry relative to which the
   *          search is to be performed.
   * @param scope
   *          The scope of the search.
   * @param filter
   *          The filter that defines the conditions that must be fulfilled in
   *          order for an entry to be returned.
   * @param attributeDescriptions
   *          The names of the attributes to be included with each entry.
   * @return The single search result entry returned from the search.
   * @throws ErrorResultException
   *           If the result code indicates that the request failed for some
   *           reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws LocalizedIllegalArgumentException
   *           If {@code baseObject} could not be decoded using the default
   *           schema or if {@code filter} is not a valid LDAP string
   *           representation of a filter.
   * @throws UnsupportedOperationException
   *           If this connection does not support search operations.
   * @throws IllegalStateException
   *           If this connection has already been closed, i.e. if {@code
   *           isClosed() == true}.
   * @throws NullPointerException
   *           If the {@code baseObject}, {@code scope}, or {@code filter} were
   *           {@code null}.
   */
  SearchResultEntry searchSingleEntry(String baseObject, SearchScope scope,
      String filter, String... attributeDescriptions)
      throws ErrorResultException, InterruptedException,
      LocalizedIllegalArgumentException, UnsupportedOperationException,
      IllegalStateException, NullPointerException;
}
