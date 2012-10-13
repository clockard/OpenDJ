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
 */

package com.forgerock.opendj.ldap;

import static org.forgerock.opendj.ldap.TestCaseUtils.findFreeSocketAddress;
import static org.testng.Assert.assertTrue;

import java.net.Socket;
import java.net.SocketAddress;

import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.testng.annotations.Test;

/**
 * Tests DefaultTCPNIOTransport class.
 */
public class DefaultTCPNIOTransportTestCase extends LDAPTestCase {
    /**
     * Tests the default transport.
     * <p>
     * FIXME: this test is disable because it does not clean up any of the
     * connections it creates.
     *
     * @throws Exception
     *             If an unexpected error occurred.
     */
    @Test(enabled = true)
    public void testGetInstance() throws Exception {
        // Create a transport.
        final TCPNIOTransport transport = DefaultTCPNIOTransport.getInstance();
        SocketAddress socketAddress = findFreeSocketAddress();
        transport.bind(socketAddress);

        // Establish a socket connection to see if the transport factory works.
        final Socket socket = new Socket();
        socket.connect(socketAddress);

        // Successfully connected if there is no exception.
        assertTrue(socket.isConnected());
        // Don't stop the transport because it is shared with the ldap server.
    }
}
