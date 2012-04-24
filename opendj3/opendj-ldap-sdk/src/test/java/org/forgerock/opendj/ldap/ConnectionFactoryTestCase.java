/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2010 Sun Microsystems, Inc.
 *      Portions copyright 2011 ForgeRock AS
 */

package org.forgerock.opendj.ldap;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.forgerock.opendj.ldap.requests.DigestMD5SASLBindRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldap.schema.SchemaBuilder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.forgerock.opendj.util.CompletedFutureResult;
import com.forgerock.opendj.util.StaticUtils;

/**
 * Tests the {@code ConnectionFactory} classes.
 */
@SuppressWarnings("javadoc")
public class ConnectionFactoryTestCase extends SdkTestCase {
    class MyResultHandler implements ResultHandler<Connection> {
        // latch.
        private final CountDownLatch latch;
        // invalid flag.
        private volatile ErrorResultException error;

        MyResultHandler(final CountDownLatch latch) {
            this.latch = latch;
        }

        public void handleErrorResult(final ErrorResultException error) {
            // came here.
            this.error = error;
            latch.countDown();
        }

        public void handleResult(final Connection con) {
            //
            latch.countDown();
        }
    }

    /**
     * Ensures that the LDAP Server is running.
     *
     * @throws Exception
     *             If an unexpected problem occurs.
     */
    @BeforeClass()
    public void startServer() throws Exception {
        TestCaseUtils.startServer();
    }

    /**
     * Disables logging before the tests.
     */
    @BeforeClass()
    public void disableLogging() {
        StaticUtils.DEBUG_LOG.setLevel(Level.SEVERE);
    }

    /**
     * Re-enable logging after the tests.
     */
    @AfterClass()
    public void enableLogging() {
        StaticUtils.DEBUG_LOG.setLevel(Level.INFO);
    }

    @DataProvider(name = "connectionFactories")
    public Object[][] getConnectionFactories() throws Exception {
        Object[][] factories = new Object[21][1];

        // HeartBeatConnectionFactory
        // Use custom search request.
        SearchRequest request =
                Requests.newSearchRequest("uid=user.0,ou=people,o=test", SearchScope.BASE_OBJECT,
                        "objectclass=*", "cn");

        factories[0][0] =
                new HeartBeatConnectionFactory(new LDAPConnectionFactory("localhost", TestCaseUtils
                        .getLdapPort()), 1000, TimeUnit.MILLISECONDS, request);

        // InternalConnectionFactory
        factories[1][0] = Connections.newInternalConnectionFactory(LDAPServer.getInstance(), null);

        // AuthenticatedConnectionFactory
        factories[2][0] =
                new AuthenticatedConnectionFactory(new LDAPConnectionFactory("localhost",
                        TestCaseUtils.getLdapPort()), Requests
                        .newSimpleBindRequest("", new char[0]));

        // AuthenticatedConnectionFactory with multi-stage SASL
        factories[3][0] =
                new AuthenticatedConnectionFactory(new LDAPConnectionFactory("localhost",
                        TestCaseUtils.getLdapPort()), Requests.newCRAMMD5SASLBindRequest("id:user",
                            "password".toCharArray()));

        // LDAPConnectionFactory with default options
        factories[4][0] = new LDAPConnectionFactory("localhost", TestCaseUtils.getLdapPort());

        // LDAPConnectionFactory with startTLS
        SSLContext sslContext =
                new SSLContextBuilder().setTrustManager(TrustManagers.trustAll()).getSSLContext();
        LDAPOptions options =
                new LDAPOptions().setSSLContext(sslContext).setUseStartTLS(true)
                        .addEnabledCipherSuite(
                                new String[] { "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                                    "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                                    "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                                    "SSL_DH_anon_WITH_DES_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5",
                                    "TLS_DH_anon_WITH_AES_128_CBC_SHA",
                                    "TLS_DH_anon_WITH_AES_256_CBC_SHA" });
        factories[5][0] =
                new LDAPConnectionFactory("localhost", TestCaseUtils.getLdapPort(), options);

        // startTLS + SASL confidentiality
        // Use IP address here so that DIGEST-MD5 host verification works if
        // local
        // host name is not localhost (e.g. on some machines it might be
        // localhost.localdomain).
        factories[6][0] =
                new AuthenticatedConnectionFactory(new LDAPConnectionFactory(new InetSocketAddress(
                        "127.0.0.1", TestCaseUtils.getLdapPort()), options), Requests
                        .newDigestMD5SASLBindRequest("id:user", "password".toCharArray()).addQOP(
                                DigestMD5SASLBindRequest.QOP_AUTH_CONF).setCipher(
                                DigestMD5SASLBindRequest.CIPHER_LOW));

        // Connection pool and load balancing tests.
        ConnectionFactory offlineServer1 =
                Connections.newNamedConnectionFactory(new LDAPConnectionFactory("localhost",
                        TestCaseUtils.findFreePort()), "offline1");
        ConnectionFactory offlineServer2 =
                Connections.newNamedConnectionFactory(new LDAPConnectionFactory("localhost",
                        TestCaseUtils.findFreePort()), "offline2");
        ConnectionFactory onlineServer =
                Connections.newNamedConnectionFactory(new LDAPConnectionFactory("localhost",
                        TestCaseUtils.getLdapPort()), "online");

        // Connection pools.
        factories[7][0] = Connections.newFixedConnectionPool(onlineServer, 10);

        // Round robin.
        factories[8][0] =
                Connections.newLoadBalancer(new RoundRobinLoadBalancingAlgorithm(Arrays.asList(
                        onlineServer, offlineServer1)));
        factories[9][0] = factories[8][0];
        factories[10][0] = factories[8][0];
        factories[11][0] =
                Connections.newLoadBalancer(new RoundRobinLoadBalancingAlgorithm(Arrays.asList(
                        offlineServer1, onlineServer)));
        factories[12][0] =
                Connections.newLoadBalancer(new RoundRobinLoadBalancingAlgorithm(Arrays.asList(
                        offlineServer1, offlineServer2, onlineServer)));
        factories[13][0] =
                Connections.newLoadBalancer(new RoundRobinLoadBalancingAlgorithm(Arrays
                        .<ConnectionFactory> asList(Connections.newFixedConnectionPool(
                                offlineServer1, 10), Connections.newFixedConnectionPool(
                                onlineServer, 10))));

        // Fail-over.
        factories[14][0] =
                Connections.newLoadBalancer(new FailoverLoadBalancingAlgorithm(Arrays.asList(
                        onlineServer, offlineServer1)));
        factories[15][0] = factories[14][0];
        factories[16][0] = factories[14][0];
        factories[17][0] =
                Connections.newLoadBalancer(new FailoverLoadBalancingAlgorithm(Arrays.asList(
                        offlineServer1, onlineServer)));
        factories[18][0] =
                Connections.newLoadBalancer(new FailoverLoadBalancingAlgorithm(Arrays.asList(
                        offlineServer1, offlineServer2, onlineServer)));
        factories[19][0] =
                Connections.newLoadBalancer(new FailoverLoadBalancingAlgorithm(Arrays
                        .<ConnectionFactory> asList(Connections.newFixedConnectionPool(
                                offlineServer1, 10), Connections.newFixedConnectionPool(
                                onlineServer, 10))));

        factories[20][0] = Connections.newFixedConnectionPool(onlineServer, 10);

        return factories;
    }

    /**
     * Tests the async connection in the blocking mode. This is not fully async
     * as it blocks on the future.
     *
     * @throws Exception
     */
    @Test(dataProvider = "connectionFactories")
    public void testBlockingFutureNoHandler(ConnectionFactory factory) throws Exception {
        final FutureResult<Connection> future = factory.getConnectionAsync(null);
        final Connection con = future.get();
        // quickly check if it is a valid connection.
        // Don't use a result handler.
        assertNotNull(con.readEntryAsync(DN.rootDN(), null, null).get());
        con.close();
    }

    /**
     * Tests the non-blocking fully async connection using a handler.
     *
     * @throws Exception
     */
    @Test(dataProvider = "connectionFactories")
    public void testNonBlockingFutureWithHandler(ConnectionFactory factory) throws Exception {
        // Use the handler to get the result asynchronously.
        final CountDownLatch latch = new CountDownLatch(1);
        final MyResultHandler handler = new MyResultHandler(latch);
        factory.getConnectionAsync(handler);

        // Since we don't have anything to do, we would rather
        // be notified by the latch when the other thread calls our handler.
        latch.await(); // should do a timed wait rather?
        if (handler.error != null) {
            throw handler.error;
        }
    }

    /**
     * Tests the synchronous connection.
     *
     * @throws Exception
     */
    @Test(dataProvider = "connectionFactories")
    public void testSynchronousConnection(ConnectionFactory factory) throws Exception {
        final Connection con = factory.getConnection();
        assertNotNull(con);
        // quickly check if it is a valid connection.
        assertTrue(con.readEntry("").getName().isRootDN());
        con.close();
    }

    /**
     * Verifies that LDAP connections take into consideration changes to the
     * default schema post creation. See OPENDJ-159.
     * <p>
     * This test is disabled because it cannot be run in parallel with rest of
     * the test suite, because it modifies the global default schema.
     *
     * @throws Exception
     *             If an unexpected error occurred.
     */
    @Test(enabled = false)
    public void testSchemaUsage() throws Exception {
        // Create a connection factory: this should always use the default
        // schema,
        // even if it is updated.
        final ConnectionFactory factory =
                new LDAPConnectionFactory("localhost", TestCaseUtils.getLdapPort());
        final Schema defaultSchema = Schema.getDefaultSchema();

        final Connection connection = factory.getConnection();
        try {
            // Simulate a client which reads the schema from the server and then
            // sets it as the application default. We then want subsequent
            // operations to use this schema, not the original default.
            final SchemaBuilder builder = new SchemaBuilder(Schema.getCoreSchema());
            builder.addAttributeType("( 0.9.2342.19200300.100.1.3 NAME 'mail' EQUALITY "
                    + "caseIgnoreIA5Match SUBSTR caseIgnoreIA5SubstringsMatch "
                    + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.26{256} )", false);
            final Schema testSchema = builder.toSchema().asNonStrictSchema();
            assertThat(testSchema.getWarnings()).isEmpty();
            Schema.setDefaultSchema(testSchema);

            // Read an entry containing the mail attribute.
            final SearchResultEntry e = connection.readEntry("uid=user.0,ou=people,o=test");

            assertThat(e.getAttribute("mail")).isNotNull();
            assertThat(e.getAttribute(AttributeDescription.valueOf("mail", testSchema)))
                    .isNotNull();
        } finally {
            // Restore original schema.
            Schema.setDefaultSchema(defaultSchema);

            // Close connection.
            connection.close();
        }
    }

    /**
     * Tests connection pool closure.
     *
     * @throws Exception
     *             If an unexpected exception occurred.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConnectionPoolClose() throws Exception {
        // We'll use a pool of 4 connections.
        final int size = 4;

        // Count number of real connections which are open.
        final AtomicInteger realConnectionCount = new AtomicInteger();
        final boolean[] realConnectionIsClosed = new boolean[size];
        Arrays.fill(realConnectionIsClosed, true);

        // Mock underlying connection factory which always succeeds.
        final ConnectionFactory mockFactory = mock(ConnectionFactory.class);
        when(mockFactory.getConnectionAsync(any(ResultHandler.class))).thenAnswer(
                new Answer<FutureResult<Connection>>() {

                    public FutureResult<Connection> answer(InvocationOnMock invocation)
                            throws Throwable {
                        // Update state.
                        final int connectionID = realConnectionCount.getAndIncrement();
                        realConnectionIsClosed[connectionID] = false;

                        // Mock connection decrements counter on close.
                        Connection mockConnection = mock(Connection.class);
                        doAnswer(new Answer<Void>() {
                            public Void answer(InvocationOnMock invocation) throws Throwable {
                                realConnectionCount.decrementAndGet();
                                realConnectionIsClosed[connectionID] = true;
                                return null;
                            }
                        }).when(mockConnection).close();
                        when(mockConnection.isValid()).thenReturn(true);
                        when(mockConnection.toString()).thenReturn(
                                "Mock connection " + connectionID);

                        // Execute handler and return future.
                        ResultHandler<? super Connection> handler =
                                (ResultHandler<? super Connection>) invocation.getArguments()[0];
                        if (handler != null) {
                            handler.handleResult(mockConnection);
                        }
                        return new CompletedFutureResult<Connection>(mockConnection);
                    }
                });

        ConnectionPool pool = Connections.newFixedConnectionPool(mockFactory, size);
        Connection[] pooledConnections = new Connection[size];
        for (int i = 0; i < size; i++) {
            pooledConnections[i] = pool.getConnection();
        }

        // Pool is fully utilized.
        assertThat(realConnectionCount.get()).isEqualTo(size);
        assertThat(pooledConnections[0].isClosed()).isFalse();
        assertThat(pooledConnections[1].isClosed()).isFalse();
        assertThat(pooledConnections[2].isClosed()).isFalse();
        assertThat(pooledConnections[3].isClosed()).isFalse();
        assertThat(realConnectionIsClosed[0]).isFalse();
        assertThat(realConnectionIsClosed[1]).isFalse();
        assertThat(realConnectionIsClosed[2]).isFalse();
        assertThat(realConnectionIsClosed[3]).isFalse();

        // Release two connections.
        pooledConnections[0].close();
        pooledConnections[1].close();
        assertThat(realConnectionCount.get()).isEqualTo(4);
        assertThat(pooledConnections[0].isClosed()).isTrue();
        assertThat(pooledConnections[1].isClosed()).isTrue();
        assertThat(pooledConnections[2].isClosed()).isFalse();
        assertThat(pooledConnections[3].isClosed()).isFalse();
        assertThat(realConnectionIsClosed[0]).isFalse();
        assertThat(realConnectionIsClosed[1]).isFalse();
        assertThat(realConnectionIsClosed[2]).isFalse();
        assertThat(realConnectionIsClosed[3]).isFalse();

        // Close the pool closing the two connections immediately.
        pool.close();
        assertThat(realConnectionCount.get()).isEqualTo(2);
        assertThat(pooledConnections[0].isClosed()).isTrue();
        assertThat(pooledConnections[1].isClosed()).isTrue();
        assertThat(pooledConnections[2].isClosed()).isFalse();
        assertThat(pooledConnections[3].isClosed()).isFalse();
        assertThat(realConnectionIsClosed[0]).isTrue();
        assertThat(realConnectionIsClosed[1]).isTrue();
        assertThat(realConnectionIsClosed[2]).isFalse();
        assertThat(realConnectionIsClosed[3]).isFalse();

        // Release two remaining connections and check that they get closed.
        pooledConnections[2].close();
        pooledConnections[3].close();
        assertThat(realConnectionCount.get()).isEqualTo(0);
        assertThat(pooledConnections[0].isClosed()).isTrue();
        assertThat(pooledConnections[1].isClosed()).isTrue();
        assertThat(pooledConnections[2].isClosed()).isTrue();
        assertThat(pooledConnections[3].isClosed()).isTrue();
        assertThat(realConnectionIsClosed[0]).isTrue();
        assertThat(realConnectionIsClosed[1]).isTrue();
        assertThat(realConnectionIsClosed[2]).isTrue();
        assertThat(realConnectionIsClosed[3]).isTrue();
    }
}
