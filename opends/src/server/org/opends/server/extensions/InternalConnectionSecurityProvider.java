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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.extensions;



import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.opends.server.api.ClientConnection;
import org.opends.server.api.ConnectionSecurityProvider;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.InitializationException;
import org.opends.server.types.DisconnectReason;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.ExtensionsMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This provides an implementation of a connection security provider that is
 * intended to be used for internal client connections.  It is exactly the same
 * as the null connection security provider in that it doesn't actually protect
 * anything, but the <CODE>isSecure</CODE> method always returns
 * <CODE>true</CODE> because it is inherently secure by being an internal
 * connection.
 */
public class InternalConnectionSecurityProvider
       extends NullConnectionSecurityProvider
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.extensions.InternalConnectionSecurityProvider";



  /**
   * {@inheritDoc}
   */
  public InternalConnectionSecurityProvider()
  {
    super();

    assert debugConstructor(CLASS_NAME);
  }



  /**
   * {@inheritDoc}
   */
  protected InternalConnectionSecurityProvider(
                 ClientConnection clientConnection, SocketChannel socketChannel)
  {
    super(clientConnection, socketChannel);

    assert debugConstructor(CLASS_NAME, String.valueOf(clientConnection));
  }



  /**
   * {@inheritDoc}
   */
  public String getSecurityMechanismName()
  {
    assert debugEnter(CLASS_NAME, "getSecurityMechanismName");

    return "INTERNAL";
  }



  /**
   * {@inheritDoc}
   */
  public boolean isSecure()
  {
    assert debugEnter(CLASS_NAME, "isSecure");

    // Internal connections are inherently secure.
    return true;
  }



  /**
   * Creates a new instance of this connection security provider that will be
   * used to encode and decode all communication on the provided client
   * connection.
   *
   * @param  clientConnection  The client connection with which this security
   *                           provider will be associated.
   * @param  socketChannel     The socket channel that may be used to
   *                           communicate with the client.
   *
   * @return  The created connection security provider instance.
   *
   * @throws  DirectoryException  If a problem occurs while creating a new
   *                              instance of this security provider for the
   *                              given client connection.
   */
  public ConnectionSecurityProvider newInstance(ClientConnection
                                                      clientConnection,
                                                SocketChannel socketChannel)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "newInstance",
                      String.valueOf(clientConnection),
                      String.valueOf(socketChannel));

    return new InternalConnectionSecurityProvider(clientConnection,
                                                  socketChannel);
  }
}

