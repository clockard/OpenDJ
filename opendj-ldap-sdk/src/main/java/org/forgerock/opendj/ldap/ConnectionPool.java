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
 *      Copyright 2011 ForgeRock AS
 */

package org.forgerock.opendj.ldap;



import java.io.Closeable;



/**
 * A connection factory which maintains and re-uses a pool of connections.
 * Connections obtained from a connection pool are returned to the connection
 * pool when closed, although connection pool implementations may choose to
 * physically close the connection if needed (e.g. in order to reduce the size
 * of the pool).
 * <p>
 * When connection pools are no longer needed they must be explicitly closed in
 * order to close any remaining pooled connections.
 * <p>
 * Since pooled connections are re-used, applications must use operations such
 * as binds and StartTLS with extreme caution.
 */
public interface ConnectionPool extends ConnectionFactory, Closeable
{
  /**
   * Releases any resources associated with this connection pool. Pooled
   * connections will be permanently closed and this connection pool will no
   * longer be available for use.
   * <p>
   * Attempts to use this connection pool after it has been closed will result
   * in an {@code IllegalStateException}.
   * <p>
   * Calling {@code close} on a connection pool which is already closed has no
   * effect.
   */
  void close();



  /**
   * Asynchronously obtains a connection from this connection pool, potentially
   * opening a new connection if needed.
   * <p>
   * The returned {@code FutureResult} can be used to retrieve the pooled
   * connection. Alternatively, if a {@code ResultHandler} is
   * provided, the handler will be notified when the pooled connection is
   * available and ready for use.
   * <p>
   * Closing the pooled connection will, depending on the connection pool
   * implementation, return the connection to this pool without closing it.
   *
   * @param handler
   *          The completion handler, or {@code null} if no handler is to be
   *          used.
   * @return A future which can be used to retrieve the pooled connection.
   * @throws IllegalStateException
   *           If this connection pool has already been closed.
   */
  FutureResult<Connection> getConnectionAsync(
      ResultHandler<? super Connection> handler);



  /**
   * Obtains a connection from this connection pool, potentially opening a new
   * connection if needed.
   * <p>
   * Closing the pooled connection will, depending on the connection pool
   * implementation, return the connection to this pool without closing it.
   *
   * @return A pooled connection.
   * @throws ErrorResultException
   *           If the connection request failed for some reason.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws IllegalStateException
   *           If this connection pool has already been closed.
   */
  Connection getConnection() throws ErrorResultException, InterruptedException;
}