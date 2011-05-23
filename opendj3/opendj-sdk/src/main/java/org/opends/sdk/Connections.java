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



import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opends.sdk.requests.BindRequest;
import org.opends.sdk.requests.SearchRequest;

import com.forgerock.opendj.util.Validator;



/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Connections
{
  /**
   * Creates a new authenticated connection factory which will obtain
   * connections using the provided connection factory and immediately perform
   * the provided Bind request.
   * <p>
   * The connections returned by an authenticated connection factory support all
   * operations with the exception of Bind requests. Attempts to perform a Bind
   * will result in an {@code UnsupportedOperationException}.
   * <p>
   * If the Bind request fails for some reason (e.g. invalid credentials), then
   * the connection attempt will fail and an {@code ErrorResultException} will
   * be thrown.
   *
   * @param factory
   *          The connection factory to use for connecting to the Directory
   *          Server.
   * @param request
   *          The Bind request to use for authentication.
   * @return The new connection pool.
   * @throws NullPointerException
   *           If {@code factory} or {@code request} was {@code null}.
   */
  public static ConnectionFactory newAuthenticatedConnectionFactory(
      final ConnectionFactory factory, final BindRequest request)
      throws NullPointerException
  {
    Validator.ensureNotNull(factory, request);

    return new AuthenticatedConnectionFactory(factory, request);
  }



  /**
   * Creates a new connection pool which will maintain {@code poolSize}
   * connections created using the provided connection factory.
   *
   * @param factory
   *          The connection factory to use for creating new connections.
   * @param poolSize
   *          The maximum size of the connection pool.
   * @return The new connection pool.
   * @throws IllegalArgumentException
   *           If {@code poolSize} is negative.
   * @throws NullPointerException
   *           If {@code factory} was {@code null}.
   */
  public static ConnectionFactory newConnectionPool(
      final ConnectionFactory factory, final int poolSize)
      throws IllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(factory);
    Validator.ensureTrue(poolSize >= 0, "negative pool size");
    return new ConnectionPool(factory, poolSize);
  }



  /**
   * Creates a new heart-beat connection factory which will create connections
   * using the provided connection factory and periodically ping any created
   * connections in order to detect that they are still alive every 10 seconds
   * using the default scheduler.
   *
   * @param factory
   *          The connection factory to use for creating connections.
   * @return The new heart-beat connection factory.
   * @throws NullPointerException
   *           If {@code factory} was {@code null}.
   */
  public static ConnectionFactory newHeartBeatConnectionFactory(
      final ConnectionFactory factory) throws NullPointerException
  {
    return new HeartBeatConnectionFactory(factory);
  }



  /**
   * Creates a new heart-beat connection factory which will create connections
   * using the provided connection factory and periodically ping any created
   * connections in order to detect that they are still alive using the
   * specified frequency and the default scheduler.
   *
   * @param factory
   *          The connection factory to use for creating connections.
   * @param interval
   *          The interval between keepalive pings.
   * @param unit
   *          The time unit for the interval between keepalive pings.
   * @return The new heart-beat connection factory.
   * @throws IllegalArgumentException
   *           If {@code interval} was negative.
   * @throws NullPointerException
   *           If {@code factory} or {@code unit} was {@code null}.
   */
  public static ConnectionFactory newHeartBeatConnectionFactory(
      final ConnectionFactory factory, final long interval, final TimeUnit unit)
      throws IllegalArgumentException, NullPointerException
  {
    return new HeartBeatConnectionFactory(factory, interval, unit);
  }






  /**
   * Creates a new heart-beat connection factory which will create connections
   * using the provided connection factory and periodically ping any created
   * connections using the specified search request in order to detect that they
   * are still alive.
   *
   * @param factory
   *          The connection factory to use for creating connections.
   * @param interval
   *          The interval between keepalive pings.
   * @param unit
   *          The time unit for the interval between keepalive pings.
   * @param heartBeat
   *          The search request to use for keepalive pings.
   * @return The new heart-beat connection factory.
   * @throws IllegalArgumentException
   *           If {@code interval} was negative.
   * @throws NullPointerException
   *           If {@code factory}, {@code unit}, or {@code heartBeat} was {@code null}.
   */
  public static ConnectionFactory newHeartBeatConnectionFactory(
      final ConnectionFactory factory, final long interval, final TimeUnit unit,
      final SearchRequest heartBeat) throws IllegalArgumentException,
      NullPointerException
  {
    return new HeartBeatConnectionFactory(factory, interval, unit, heartBeat);
  }






  /**
   * Creates a new heart-beat connection factory which will create connections
   * using the provided connection factory and periodically ping any created
   * connections using the specified search request in order to detect that they
   * are still alive.
   *
   * @param factory
   *          The connection factory to use for creating connections.
   * @param interval
   *          The interval between keepalive pings.
   * @param unit
   *          The time unit for the interval between keepalive pings.
   * @param heartBeat
   *          The search request to use for keepalive pings.
   * @param scheduler
   *          The scheduler which should for periodically sending keepalive
   *          pings.
   * @return The new heart-beat connection factory.
   * @throws IllegalArgumentException
   *           If {@code interval} was negative.
   * @throws NullPointerException
   *           If {@code factory}, {@code unit}, or {@code heartBeat} was
   *           {@code null}.
   */
  public static ConnectionFactory newHeartBeatConnectionFactory(
      final ConnectionFactory factory, final long interval,
      final TimeUnit unit, final SearchRequest heartBeat,
      final ScheduledExecutorService scheduler)
      throws IllegalArgumentException, NullPointerException
  {
    return new HeartBeatConnectionFactory(factory, interval, unit, heartBeat,
        scheduler);
  }



  /**
   * Creates a new connection factory which binds internal client connections to
   * {@link ServerConnection}s created using the provided
   * {@link ServerConnectionFactory}.
   * <p>
   * When processing requests, {@code ServerConnection} implementations are
   * passed an integer as the first parameter. This integer represents a pseudo
   * {@code requestID} which is incremented for each successive internal request
   * on a per client connection basis. The request ID may be useful for logging
   * purposes.
   * <p>
   * An internal connection factory does not require {@code ServerConnection}
   * implementations to return a result when processing requests. However, it is
   * recommended that implementations do always return results even for
   * abandoned requests. This is because application client threads may block
   * indefinitely waiting for results.
   *
   * @param <C>
   *          The type of client context.
   * @param factory
   *          The server connection factory to use for creating connections.
   * @param clientContext
   *          The client context.
   * @return The new internal connection factory.
   * @throws NullPointerException
   *           If {@code factory} was {@code null}.
   */
  public static <C> ConnectionFactory newInternalConnectionFactory(
      final ServerConnectionFactory<C, Integer> factory, final C clientContext)
      throws NullPointerException
  {
    Validator.ensureNotNull(factory);
    return new InternalConnectionFactory<C>(factory, clientContext);
  }



  /**
   * Creates a new load balancer which will obtain connections using the
   * provided load balancing algorithm.
   *
   * @param algorithm
   *          The load balancing algorithm which will be used to obtain the next
   * @return The new load balancer.
   * @throws NullPointerException
   *           If {@code algorithm} was {@code null}.
   */
  public static ConnectionFactory newLoadBalancer(
      final LoadBalancingAlgorithm algorithm) throws NullPointerException
  {
    return new LoadBalancer(algorithm);
  }



  /**
   * Creates a new connection factory which forwards connection requests to the
   * provided factory, but whose {@code toString} method will always return
   * {@code name}.
   * <p>
   * This method may be useful for debugging purposes in order to more easily
   * identity connection factories.
   *
   * @param factory
   *          The connection factory to be named.
   * @param name
   *          The name of the connection factory.
   * @return The named connection factory.
   * @throws NullPointerException
   *           If {@code factory} or {@code name} was {@code null}.
   */
  public static ConnectionFactory newNamedConnectionFactory(
      final ConnectionFactory factory, final String name)
      throws NullPointerException
  {
    Validator.ensureNotNull(factory, name);

    return new ConnectionFactory()
    {

      @Override
      public FutureResult<AsynchronousConnection> getAsynchronousConnection(
          final ResultHandler<? super AsynchronousConnection> handler)
      {
        return factory.getAsynchronousConnection(handler);
      }



      @Override
      public Connection getConnection() throws ErrorResultException,
          InterruptedException
      {
        return factory.getConnection();
      }



      /**
       * {@inheritDoc}
       */
      @Override
      public String toString()
      {
        return name;
      }

    };
  }



  // Prevent instantiation.
  private Connections()
  {
    // Do nothing.
  }
}
