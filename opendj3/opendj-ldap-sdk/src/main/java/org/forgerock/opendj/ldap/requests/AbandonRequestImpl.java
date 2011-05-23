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
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.ldap.requests;



/**
 * Abandon request implementation.
 */
final class AbandonRequestImpl extends AbstractRequestImpl<AbandonRequest>
    implements AbandonRequest
{

  private int requestID;



  /**
   * Creates a new abandon request using the provided message ID.
   *
   * @param requestID
   *          The message ID of the request to be abandoned.
   */
  AbandonRequestImpl(final int requestID)
  {
    this.requestID = requestID;
  }



  /**
   * Creates a new abandon request that is an exact copy of the provided
   * request.
   *
   * @param abandonRequest
   *          The abandon request to be copied.
   * @throws NullPointerException
   *           If {@code abandonRequest} was {@code null} .
   */
  AbandonRequestImpl(final AbandonRequest abandonRequest)
  {
    super(abandonRequest);
    this.requestID = abandonRequest.getRequestID();
  }



  public int getRequestID()
  {
    return requestID;
  }



  /**
   * {@inheritDoc}
   */
  public AbandonRequest setRequestID(final int id)
      throws UnsupportedOperationException
  {
    this.requestID = id;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("AbandonRequest(requestID=");
    builder.append(getRequestID());
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }



  @Override
  AbandonRequest getThis()
  {
    return this;
  }

}
