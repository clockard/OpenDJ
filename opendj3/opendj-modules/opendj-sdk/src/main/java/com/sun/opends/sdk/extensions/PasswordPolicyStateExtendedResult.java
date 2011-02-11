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

package com.sun.opends.sdk.extensions;



import java.util.ArrayList;
import java.util.List;

import org.opends.sdk.ByteString;
import org.opends.sdk.DN;
import org.opends.sdk.ResultCode;
import org.opends.sdk.responses.AbstractExtendedResult;



/**
 * The password policy state extended result.
 */
public final class PasswordPolicyStateExtendedResult extends
    AbstractExtendedResult<PasswordPolicyStateExtendedResult> implements
    PasswordPolicyStateOperationContainer
{
  private final String targetUser;

  private final List<PasswordPolicyStateOperation> operations =
    new ArrayList<PasswordPolicyStateOperation>();



  /**
   * Creates a new password policy state extended result with the provided
   * result code and target user.
   *
   * @param resultCode
   *          The result code.
   * @param targetUser
   *          The user name.
   */
  public PasswordPolicyStateExtendedResult(final ResultCode resultCode,
      final DN targetUser)
  {
    this(resultCode, String.valueOf(targetUser));
  }



  /**
   * Creates a new password policy state extended result with the provided
   * result code and target user.
   *
   * @param resultCode
   *          The result code.
   * @param targetUser
   *          The user name.
   */
  public PasswordPolicyStateExtendedResult(final ResultCode resultCode,
      final String targetUser)
  {
    super(resultCode);
    this.targetUser = targetUser;
  }



  /**
   * {@inheritDoc}
   */
  public void addOperation(final PasswordPolicyStateOperation operation)
  {
    operations.add(operation);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getOID()
  {
    // No response name defined.
    return PasswordPolicyStateExtendedRequest.OID;
  }



  /**
   * {@inheritDoc}
   */
  public Iterable<PasswordPolicyStateOperation> getOperations()
  {
    return operations;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ByteString getValue()
  {
    return PasswordPolicyStateExtendedRequest.encode(targetUser, operations);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasValue()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("PasswordPolicyStateExtendedResponse(resultCode=");
    builder.append(getResultCode());
    builder.append(", matchedDN=");
    builder.append(getMatchedDN());
    builder.append(", diagnosticMessage=");
    builder.append(getDiagnosticMessage());
    builder.append(", referrals=");
    builder.append(getReferralURIs());
    builder.append(", responseName=");
    builder.append(getOID());
    builder.append(", targetUser=");
    builder.append(targetUser);
    builder.append(", operations=");
    builder.append(operations);
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }
}
