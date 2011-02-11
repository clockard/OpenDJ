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

package org.opends.sdk.requests;

import org.opends.sdk.ByteString;
import org.opends.sdk.responses.PasswordModifyExtendedResult;

/**
 * Unmodifiable password modify extended request implementation.
 */
final class UnmodifiablePasswordModifyExtendedRequestImpl
    extends AbstractUnmodifiableExtendedRequest
    <PasswordModifyExtendedRequest, PasswordModifyExtendedResult>
    implements PasswordModifyExtendedRequest
{
  UnmodifiablePasswordModifyExtendedRequestImpl(
      PasswordModifyExtendedRequest impl) {
    super(impl);
  }

  public ByteString getNewPassword() {
    return impl.getNewPassword();
  }

  public ByteString getOldPassword() {
    return impl.getOldPassword();
  }

  public ByteString getUserIdentity() {
    return impl.getUserIdentity();
  }

  public String getUserIdentityAsString() {
    return impl.getUserIdentityAsString();
  }

  public PasswordModifyExtendedRequest setNewPassword(ByteString newPassword)
  {
    throw new UnsupportedOperationException();
  }

  public PasswordModifyExtendedRequest setNewPassword(char[] newPassword) {
    throw new UnsupportedOperationException();
  }

  public PasswordModifyExtendedRequest setOldPassword(ByteString oldPassword)
  {
    throw new UnsupportedOperationException();
  }

  public PasswordModifyExtendedRequest setOldPassword(char[] oldPassword) {
    throw new UnsupportedOperationException();
  }

  public PasswordModifyExtendedRequest setUserIdentity(
      ByteString userIdentity) {
    throw new UnsupportedOperationException();
  }

  public PasswordModifyExtendedRequest setUserIdentity(String userIdentity) {
    throw new UnsupportedOperationException();
  }
}
