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
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 *      Portions copyright 2012-2013 ForgeRock AS.
 */

package org.forgerock.opendj.ldap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.opendj.ldif.LDIFEntryReader;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import com.forgerock.opendj.util.CompletedFutureResult;
import com.forgerock.opendj.util.StaticUtils;
import com.forgerock.opendj.util.TimeSource;

/**
 * This class defines some utility functions which can be used by test cases.
 */
public final class TestCaseUtils {
    /**
     * Creates a temporary text file with the specified contents. It will be
     * marked for automatic deletion when the JVM exits.
     *
     * @param lines
     *            The file contents.
     * @return The absolute path to the file that was created.
     * @throws Exception
     *             If an unexpected problem occurs.
     */
    public static String createTempFile(final String... lines) throws Exception {
        final File f = File.createTempFile("LDIFBasedTestCase", ".txt");
        f.deleteOnExit();

        final FileWriter w = new FileWriter(f);
        for (final String s : lines) {
            w.write(s + System.getProperty("line.separator"));
        }

        w.close();

        return f.getAbsolutePath();
    }

    /**
     * Return the canonical file path for a test file.
     * <p>
     * For example, the path to file "src/test/resources/somedir/somefile" is
     * obtained with <code>getTestFilePath("somedir/somefile")</code>.
     *
     * @param relativePathFromClasspath
     *            the relative path to any directory that is declared
     *            in the classpath (typically the src/test/resources
     *            directory)
     * @return the canonical path
     * @throws Exception
     *             if file is not found or can't be read
     */
    public static String getTestFilePath(String relativePathFromClasspath) throws Exception {
        return new File(TestCaseUtils.class.getClassLoader().getResource(relativePathFromClasspath).toURI())
                .getCanonicalPath();
    }

    /**
     * Finds a free server socket port on the local host.
     *
     * @return The free port.
     */
    public static SocketAddress findFreeSocketAddress() {
        try {
            ServerSocket serverLdapSocket = new ServerSocket();
            serverLdapSocket.setReuseAddress(true);
            serverLdapSocket.bind(new InetSocketAddress("127.0.0.1", 0));
            final SocketAddress address = serverLdapSocket.getLocalSocketAddress();
            serverLdapSocket.close();
            return address;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an internal client connection to the running ldap server.
     *
     * @return The internal client connection.
     * @throws Exception
     *             When an error occurs.
     */
    public static Connection getInternalConnection() throws Exception {
        startServer();
        final ConnectionFactory factory =
                Connections.newInternalConnectionFactory(LDAPServer.getInstance(), null);
        return factory.getConnection();
    }

    /**
     * Starts the test ldap server.
     *
     * @throws Exception
     *             If an error occurs when starting the server.
     */
    public static void startServer() throws Exception {
        LDAPServer.getInstance().start();
    }

    /**
     * Stops the test ldap server.
     */
    public static void stopServer() {
        LDAPServer.getInstance().stop();
    }

    /**
     * Returns the socket address of the server.
     *
     * @return The socket address of the server.
     */
    public static SocketAddress getServerSocketAddress() {
        return LDAPServer.getInstance().getSocketAddress();
    }

    /**
     * Creates a mock connection factory which will return the provided
     * connections in order.
     *
     * @param first
     *            The first connection to return.
     * @param remaining
     *            The remaining connections to return.
     * @return The connection factory.
     */
    @SuppressWarnings("unchecked")
    public static ConnectionFactory mockConnectionFactory(final Connection first,
            final Connection... remaining) {
        final ConnectionFactory factory = mock(ConnectionFactory.class);
        try {
            when(factory.getConnection()).thenReturn(first, remaining);
        } catch (ErrorResultException ignored) {
            // Cannot happen.
        }
        when(factory.getConnectionAsync(any(ResultHandler.class))).thenAnswer(
                new Answer<FutureResult<Connection>>() {
                    @Override
                    public FutureResult<Connection> answer(final InvocationOnMock invocation)
                            throws Throwable {
                        final Connection connection = factory.getConnection();
                        // Execute handler and return future.
                        final ResultHandler<? super Connection> handler =
                                (ResultHandler<? super Connection>) invocation.getArguments()[0];
                        if (handler != null) {
                            handler.handleResult(connection);
                        }
                        return new CompletedFutureResult<Connection>(connection);
                    }
                });
        return factory;
    }

    /**
     * Creates a mock connection which will store connection event listeners in
     * the provided list.
     *
     * @param listeners
     *            The list which should be used for storing event listeners.
     * @return The mock connection.
     */
    public static Connection mockConnection(final List<ConnectionEventListener> listeners) {
        final Connection mockConnection = mock(Connection.class);

        // Handle listener registration / deregistration in mock connection.
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final ConnectionEventListener listener =
                        (ConnectionEventListener) invocation.getArguments()[0];
                listeners.add(listener);
                return null;
            }
        }).when(mockConnection).addConnectionEventListener(any(ConnectionEventListener.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final ConnectionEventListener listener =
                        (ConnectionEventListener) invocation.getArguments()[0];
                listeners.remove(listener);
                return null;
            }
        }).when(mockConnection).removeConnectionEventListener(any(ConnectionEventListener.class));

        return mockConnection;
    }

    /**
     * Returns a mock {@link TimeSource} which can be used for injecting fake
     * time stamps into components.
     *
     * @param times
     *            The times in milli-seconds which should be returned by the
     *            time source.
     * @return The mock time source.
     */
    public static TimeSource mockTimeSource(final long... times) {
        final TimeSource mock = mock(TimeSource.class);
        OngoingStubbing<Long> stubbing = when(mock.currentTimeMillis());
        for (long t : times) {
            stubbing = stubbing.thenReturn(t);
        }
        return mock;
    }

    /**
     * Builds an entry from the provided lines of LDIF.
     * <p>
     * Here's a sample usage:
     * <pre>
     * Entry john = makeEntry(
     *   "dn: cn=John Smith,dc=example,dc=com",
     *   "objectclass: inetorgperson",
     *   "cn: John Smith",
     *   "sn: Smith",
     *   "givenname: John");
     * </pre>
     *
     * @param lines
     *          LDIF lines that contains entry definition.
     * @return an entry
     * @throws IOException
     *          If an error occurs.
     */
    public static Entry makeEntry(String... lines) throws IOException {
       return makeEntries(lines).get(0);
    }

    /**
     * Builds a list of entries from the provided lines of LDIF.
     * <p>
     * Here's a sample usage
     * <pre>
     * List<Entry> smiths = TestCaseUtils.makeEntries(
     *   "dn: cn=John Smith,dc=example,dc=com",
     *   "objectclass: inetorgperson",
     *   "cn: John Smith",
     *   "sn: Smith",
     *   "givenname: John",
     *   "",
     *   "dn: cn=Jane Smith,dc=example,dc=com",
     *   "objectclass: inetorgperson",
     *   "cn: Jane Smith",
     *   "sn: Smith",
     *   "givenname: Jane");
     * </pre>
     * @param ldifLines
     *          LDIF lines that contains entries definition.
     * @return a list of entries
     * @throws IOException
     *          If an error occurs.
     */
    public static List<Entry> makeEntries(String... ldifLines) throws IOException {
        List<Entry> entries = new ArrayList<Entry>();
        LDIFEntryReader reader = null;
        try {
            reader = new LDIFEntryReader(ldifLines);
            while (reader.hasNext()) {
                entries.add(reader.readEntry());
            }
        } finally {
            StaticUtils.closeSilently(reader);
        }
        return entries;
    }

}
