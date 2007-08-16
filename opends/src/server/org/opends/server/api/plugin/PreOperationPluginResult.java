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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.api.plugin;



/**
 * This class defines a data structure that holds information about
 * the result of processing by a pre-operation plugin.
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.UNCOMMITTED,
     mayInstantiate=true,
     mayExtend=false,
     mayInvoke=true)
public class PreOperationPluginResult
{
  /**
   * A pre-operation plugin result instance that indicates all
   * processing was successful.
   */
  public static final PreOperationPluginResult SUCCESS =
       new PreOperationPluginResult();



  // Indicates whether any further pre-operation plugins should be
  // invoked for this operation.
  private final boolean continuePluginProcessing;

  // Indicates whether the pre-operation plugin terminated the client
  // connection.
  private final boolean connectionTerminated;

  // Indicates whether the server should immediately send the response
  // from this plugin to the client with no further processing.
  private final boolean sendResponseImmediately;

  // Indicates whether the server should skip the core processing for
  // the associated operation.
  private final boolean skipCoreProcessing;



  /**
   * Creates a new pre-operation plugin result with the default
   * settings.  In this case, it will indicate that the connection has
   * not been terminated, that further pre-operation plugin processing
   * should continue, that the core processing should not be skipped,
   * and that the post-operation plugin processing should not be
   * skipped.
   */
  private PreOperationPluginResult()
  {
    this(false, true, false, false);
  }



  /**
   * Creates a new pre-operation plugin result with the provided
   * information.
   *
   * @param  connectionTerminated      Indicates whether the
   *                                   pre-operation plugin terminated
   *                                   the client connection.
   * @param  continuePluginProcessing  Indicates whether any further
   *                                   pre-operation plugins should be
   *                                   invoked for this operation.
   * @param  sendResponseImmediately   Indicates whether the server
   *                                   should send the response set by
   *                                   this plugin to the client
   *                                   immediately with no further
   *                                   processing on the operation.
   */
  public PreOperationPluginResult(boolean connectionTerminated,
                                  boolean continuePluginProcessing,
                                  boolean sendResponseImmediately)
  {
    this(connectionTerminated, continuePluginProcessing,
         sendResponseImmediately, false);
  }



  /**
   * Creates a new pre-operation plugin result with the provided
   * information.
   *
   * @param  connectionTerminated      Indicates whether the
   *                                   pre-operation plugin terminated
   *                                   the client connection.
   * @param  continuePluginProcessing  Indicates whether any further
   *                                   pre-operation plugins should be
   *                                   invoked for this operation.
   * @param  sendResponseImmediately   Indicates whether the server
   *                                   should send the response set by
   *                                   this plugin to the client
   *                                   immediately with no further
   *                                   processing on the operation.
   * @param  skipCoreProcessing        Indicates whether the server
   *                                   should skip the core processing
   *                                   for the operation.  If
   *                                   {@code sendResponseImmediately}
   *                                   is {@code false}, then any
   *                                   post-operation plugins will
   *                                   still be invoked.
   */
  public PreOperationPluginResult(boolean connectionTerminated,
                                  boolean continuePluginProcessing,
                                  boolean sendResponseImmediately,
                                  boolean skipCoreProcessing)
  {
    this.connectionTerminated     = connectionTerminated;
    this.continuePluginProcessing = continuePluginProcessing;
    this.sendResponseImmediately  = sendResponseImmediately;
    this.skipCoreProcessing       = skipCoreProcessing;
  }



  /**
   * Indicates whether the pre-operation plugin terminated the client
   * connection.
   *
   * @return  {@code true} if the pre-operation plugin terminated the
   *          client connection, or {@code false} if not.
   */
  public boolean connectionTerminated()
  {
    return connectionTerminated;
  }



  /**
   * Indicates whether any further pre-operation plugins should be
   * invoked for this operation.
   *
   * @return  {@code true} if any further pre-operation plugins should
   *          be invoked for this operation, or {@code false} if not.
   */
  public boolean continuePluginProcessing()
  {
    return continuePluginProcessing;
  }



  /**
   * Indicates whether the server should send the response set by this
   * plugin to the client immediately with no further processing on
   * the operation.
   *
   * @return  {@code true} if the server should send the response set
   *          by this plugin to the client immediately, or
   *          {@code false} if further processing should be performed
   *          on the operation.
   */
  public boolean sendResponseImmediately()
  {
    return sendResponseImmediately;
  }



  /**
   * Indicates whether the server should skip core processing for the
   * operation.  If {@code sendResponseImmediately} is {@code false},
   * then the server will still process any post-operation plugins
   * that may be registered with the server.
   *
   * @return  {@code true} if the server should skip core processing
   *          for the operation, or {@code false} if not.
   */
  public boolean skipCoreProcessing()
  {
    return skipCoreProcessing;
  }



  /**
   * Retrieves a string representation of this pre-operation plugin
   * result.
   *
   * @return  A string representation of this pre-operation plugin
   *          result.
   */
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this pre-operation plugin
   * result to the provided buffer.
   *
   * @param  buffer  The buffer to which the information should be
   *                 appended.
   */
  public void toString(StringBuilder buffer)
  {
    buffer.append("PreOperationPluginResult(connectionTerminated=");
    buffer.append(connectionTerminated);
    buffer.append(", continuePluginProcessing=");
    buffer.append(continuePluginProcessing);
    buffer.append(", sendResponseImmediately=");
    buffer.append(sendResponseImmediately);
    buffer.append(", skipCoreProcessing=");
    buffer.append(skipCoreProcessing);
    buffer.append(")");
  }
}

