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



import java.util.Collections;
import java.util.List;

import org.forgerock.opendj.ldap.ResultCode;



/**
 * Unmodifiable result implementation.
 *
 * @param <S>
 *          The type of result.
 */
abstract class AbstractUnmodifiableResultImpl<S extends Result> extends
    AbstractUnmodifiableResponseImpl<S> implements Result
{

  /**
   * Creates a new unmodifiable result implementation.
   *
   * @param impl
   *          The underlying result implementation to be made unmodifiable.
   */
  AbstractUnmodifiableResultImpl(final S impl)
  {
    super(impl);
  }



  public final S addReferralURI(final String uri)
      throws UnsupportedOperationException, NullPointerException
  {
    throw new UnsupportedOperationException();
  }



  public final Throwable getCause()
  {
    return impl.getCause();
  }



  public final String getDiagnosticMessage()
  {
    return impl.getDiagnosticMessage();
  }



  public final String getMatchedDN()
  {
    return impl.getMatchedDN();
  }



  public final List<String> getReferralURIs()
  {
    return Collections.unmodifiableList(impl.getReferralURIs());
  }



  public final ResultCode getResultCode()
  {
    return impl.getResultCode();
  }



  public final boolean isReferral()
  {
    return impl.isReferral();
  }



  public final boolean isSuccess()
  {
    return impl.isSuccess();
  }



  public final S setCause(final Throwable cause)
      throws UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }



  public final S setDiagnosticMessage(final String message)
      throws UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }



  public final S setMatchedDN(final String dn)
      throws UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }



  public final S setResultCode(final ResultCode resultCode)
      throws UnsupportedOperationException, NullPointerException
  {
    throw new UnsupportedOperationException();
  }

}
