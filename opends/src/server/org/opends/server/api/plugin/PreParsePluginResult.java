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
package org.opends.server.api.plugin;



import static org.opends.server.loggers.Debug.*;



/**
 * This class defines a data structure that holds information about
 * the result of processing by a pre-parse plugin.
 */
public class PreParsePluginResult
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.api.plugin.PreParsePluginResult";



  // Indicates whether any further pre-operation plugins should be
  // invoked for this operation.
  private boolean continuePluginProcessing;

  // Indicates whether the pre-operation plugin terminated the client
  // connection.
  private boolean connectionTerminated;

  // Indicates whether the server should immediately send the response
  // from this plugin to the client with no further processing.
  private boolean sendResponseImmediately;



  /**
   * Creates a new pre-parse plugin result with the default settings.
   * In this case, it will indicate that the connection has not been
   * terminated, that further pre-parse plugin processing should
   * continue, that the core processing should not be skipped, and
   * that the pre-operation and post-operation plugin processing
   * should not be skipped.
   */
  public PreParsePluginResult()
  {
    assert debugConstructor(CLASS_NAME);

    this.connectionTerminated     = false;
    this.continuePluginProcessing = true;
    this.sendResponseImmediately  = false;
  }



  /**
   * Creates a new pre-operation plugin result with the provided
   * information.
   *
   * @param  connectionTerminated      Indicates whether the
   *                                   post-response plugin terminated
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
  public PreParsePluginResult(boolean connectionTerminated,
                              boolean continuePluginProcessing,
                              boolean sendResponseImmediately)
  {
    assert debugConstructor(CLASS_NAME,
                            String.valueOf(connectionTerminated),
                            String.valueOf(continuePluginProcessing),
                            String.valueOf(sendResponseImmediately));

    this.connectionTerminated     = connectionTerminated;
    this.continuePluginProcessing = continuePluginProcessing;
    this.sendResponseImmediately  = sendResponseImmediately;
  }



  /**
   * Indicates whether the post-response plugin terminated the client
   * connection.
   *
   * @return  <CODE>true</CODE> if the post-response plugin terminated
   *          the client connection, or <CODE>false</CODE> if not.
   */
  public boolean connectionTerminated()
  {
    assert debugEnter(CLASS_NAME, "connectionTerminated");

    return connectionTerminated;
  }



  /**
   * Specifies whether the post-response plugin terminated the client
   * connection.
   *
   * @param  connectionTerminated  Specifies whether the post-response
   *                               plugin terminated the client
   *                               connection.
   */
  public void setConnectionTerminated(boolean connectionTerminated)
  {
    assert debugEnter(CLASS_NAME, "setConnectionTerminated",
                      String.valueOf(connectionTerminated));

    this.connectionTerminated = connectionTerminated;
  }



  /**
   * Indicates whether any further post-response plugins should be
   * invoked for this operation.
   *
   * @return  <CODE>true</CODE> if any further post-response plugins
   *          should be invoked for this operation, or
   *          <CODE>false</CODE> if not.
   */
  public boolean continuePluginProcessing()
  {
    assert debugEnter(CLASS_NAME, "continuePluginProcessing");

    return continuePluginProcessing;
  }



  /**
   * Specifies whether any further post-response plugins should be
   * invoked for this operation.
   *
   * @param  continuePluginProcessing  Specifies whether any further
   *                                   post-response plugins should be
   *                                   invoked for this operation.
   */
  public void setContinuePluginProcessing(
                   boolean continuePluginProcessing)
  {
    assert debugEnter(CLASS_NAME, "setContinuePluginProcessing",
                      String.valueOf(continuePluginProcessing));

    this.continuePluginProcessing = continuePluginProcessing;
  }



  /**
   * Indicates whether the server should send the response set by this
   * plugin to the client immediately with no further processing on
   * the operation.
   *
   * @return  <CODE>true</CODE> if the server should send the response
   *          set by this plugin to the client immediately, or
   *          <CODE>false</CODE> if further processing should be
   *          performed on the operation.
   */
  public boolean sendResponseImmediately()
  {
    assert debugEnter(CLASS_NAME, "sendResponseImmediately");

    return sendResponseImmediately;
  }



  /**
   * Specifies whether the server should send the response set by this
   * plugin to the client immediately with no further processing on
   * the operation.
   *
   * @param  sendResponseImmediately  Indicates whether the server
   *                                  should send the response set by
   *                                  this plugin to the client
   *                                  immediately with no further
   *                                  processing on the operation.
   */
  public void setSendResponseImmediately(
                   boolean sendResponseImmediately)
  {
    assert debugEnter(CLASS_NAME, "setSendResponseImmediately",
                      String.valueOf(sendResponseImmediately));

    this.sendResponseImmediately = sendResponseImmediately;
  }



  /**
   * Retrieves a string representation of this post-response plugin
   * result.
   *
   * @return  A string representation of this post-response plugin
   *          result.
   */
  public String toString()
  {
    assert debugEnter(CLASS_NAME, "toString");

    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this post-response plugin
   * result to the provided buffer.
   *
   * @param  buffer  The buffer to which the information should be
   *                 appended.
   */
  public void toString(StringBuilder buffer)
  {
    assert debugEnter(CLASS_NAME, "toString",
                      "java.lang.StringBuilder");

    buffer.append("PostResponsePluginResult(connectionTerminated=");
    buffer.append(connectionTerminated);
    buffer.append(", continuePluginProcessing=");
    buffer.append(continuePluginProcessing);
    buffer.append(", sendResponseImmediately=");
    buffer.append(sendResponseImmediately);
    buffer.append(")");
  }
}

