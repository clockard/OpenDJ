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



import org.opends.server.api.ClientConnection;
import org.opends.server.api.ExtendedOperationHandler;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.ExtendedOperation;
import org.opends.server.core.InitializationException;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.ResultCode;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.ExtensionsMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class provides an implementation of the StartTLS extended operation as
 * defined in RFC 2830.  It can enable the TLS connection security provider on
 * an established connection upon receiving an appropriate request from a
 * client.
 */
public class StartTLSExtendedOperation
       extends ExtendedOperationHandler
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.extensions.StartTLSExtendedOperation";



  /**
   * Create an instance of this StartTLS extended operation handler.  All
   * initialization should be performed in the
   * <CODE>initializeExtendedOperationHandler</CODE> method.
   */
  public StartTLSExtendedOperation()
  {
    super();

    assert debugConstructor(CLASS_NAME);
  }




  /**
   * Initializes this extended operation handler based on the information in the
   * provided configuration entry.  It should also register itself with the
   * Directory Server for the particular kinds of extended operations that it
   * will process.
   *
   * @param  configEntry  The configuration entry that contains the information
   *                      to use to initialize this extended operation handler.
   *
   * @throws  ConfigException  If an unrecoverable problem arises in the
   *                           process of performing the initialization.
   *
   * @throws  InitializationException  If a problem occurs during initialization
   *                                   that is not related to the server
   *                                   configuration.
   */
  public void initializeExtendedOperationHandler(ConfigEntry configEntry)
         throws ConfigException, InitializationException
  {
    assert debugEnter(CLASS_NAME, "initializeExtendedOperationHandler",
                      String.valueOf(configEntry));

    // FIXME -- Are there any configurable options that we should support?
    DirectoryServer.registerSupportedExtension(OID_START_TLS_REQUEST, this);
  }



  /**
   * Performs any finalization that may be necessary for this extended
   * operation handler.  By default, no finalization is performed.
   */
  public void finalizeExtendedOperationHandler()
  {
    assert debugEnter(CLASS_NAME, "finalizeExtendedOperationHandler");

    DirectoryServer.deregisterSupportedExtension(OID_START_TLS_REQUEST);
  }



  /**
   * Processes the provided extended operation.
   *
   * @param  operation  The extended operation to be processed.
   */
  public void processExtendedOperation(ExtendedOperation operation)
  {
    assert debugEnter(CLASS_NAME, "processExtendedOperation",
                      String.valueOf(operation));


    // We should always include the StartTLS OID in the response (the same OID
    // is used for both the request and the response), so make sure that it will
    // happen.
    operation.setResponseOID(OID_START_TLS_REQUEST);


    // Get the reference to the client connection.  If there is none, then fail.
    ClientConnection clientConnection = operation.getClientConnection();
    if (clientConnection == null)
    {
      operation.setResultCode(ResultCode.UNAVAILABLE);

      int msgID = MSGID_STARTTLS_NO_CLIENT_CONNECTION;
      operation.appendErrorMessage(getMessage(msgID));
      return;
    }


    // Make sure that the client connection is capable of enabling TLS.  If not,
    // then fail.
    TLSCapableConnection tlsCapableConnection;
    if (clientConnection instanceof TLSCapableConnection)
    {
      tlsCapableConnection = (TLSCapableConnection) clientConnection;
    }
    else
    {
      operation.setResultCode(ResultCode.UNAVAILABLE);

      int msgID = MSGID_STARTTLS_NOT_TLS_CAPABLE;
      operation.appendErrorMessage(getMessage(msgID));
      return;
    }

    StringBuilder unavailableReason = new StringBuilder();
    if (! tlsCapableConnection.tlsProtectionAvailable(unavailableReason))
    {
      operation.setResultCode(ResultCode.UNAVAILABLE);
      operation.setErrorMessage(unavailableReason);
      return;
    }


    // If we've gotten here, then we are going to enable TLS protection or
    // close the client connection if an error occurs.  But we have to send the
    // response to the client now before enabling TLS.  Note that by doing this,
    // we forfeit the ability to send and error response if a failure occurs
    // later (e.g., in a post-operation plugin).
    operation.setResultCode(ResultCode.SUCCESS);
    operation.sendExtendedResponse();


    // Actually enable TLS protection on the client connection.  This may fail,
    // but if it does then the connection will be closed so we'll just need to
    // log it.
    try
    {
      tlsCapableConnection.enableTLSConnectionSecurityProvider();
    }
    catch (DirectoryException de)
    {
      assert debugException(CLASS_NAME, "processExtendedOperation", de);

      logError(ErrorLogCategory.CORE_SERVER, ErrorLogSeverity.MILD_ERROR,
               MSGID_STARTTLS_ERROR_ON_ENABLE,
               stackTraceToSingleLineString(de));
    }
  }
}

