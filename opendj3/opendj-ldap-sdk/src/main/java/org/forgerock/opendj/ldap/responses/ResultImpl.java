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

package org.forgerock.opendj.ldap.responses;



import org.forgerock.opendj.ldap.ResultCode;



/**
 * A generic result indicates the final status of an operation.
 */
final class ResultImpl extends AbstractResultImpl<Result> implements Result
{

  /**
   * Creates a new generic result using the provided result code.
   *
   * @param resultCode
   *          The result code.
   * @throws NullPointerException
   *           If {@code resultCode} was {@code null}.
   */
  ResultImpl(final ResultCode resultCode) throws NullPointerException
  {
    super(resultCode);
  }



  /**
   * Creates a new result that is an exact copy of the provided result.
   *
   * @param result
   *          The result to be copied.
   * @throws NullPointerException
   *           If {@code result} was {@code null} .
   */
  ResultImpl(final Result result) throws NullPointerException
  {
    super(result);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("Result(resultCode=");
    builder.append(getResultCode());
    builder.append(", matchedDN=");
    builder.append(getMatchedDN());
    builder.append(", diagnosticMessage=");
    builder.append(getDiagnosticMessage());
    builder.append(", referrals=");
    builder.append(getReferralURIs());
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }



  @Override
  Result getThis()
  {
    return this;
  }

}
