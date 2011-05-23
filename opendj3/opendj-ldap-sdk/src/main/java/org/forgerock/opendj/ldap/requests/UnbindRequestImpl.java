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
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.ldap.requests;



/**
 * Unbind request implementation.
 */
final class UnbindRequestImpl extends AbstractRequestImpl<UnbindRequest>
    implements UnbindRequest
{

  /**
   * Creates a new unbind request.
   */
  UnbindRequestImpl()
  {
    // Do nothing.
  }



  /**
   * Creates a new unbind request that is an exact copy of the provided
   * request.
   *
   * @param unbindRequest
   *          The unbind request to be copied.
   * @throws NullPointerException
   *           If {@code unbindRequest} was {@code null} .
   */
  UnbindRequestImpl(final UnbindRequest unbindRequest)
      throws NullPointerException
  {
    super(unbindRequest);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("UnbindRequest(controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }



  @Override
  UnbindRequest getThis()
  {
    return this;
  }

}
