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
 *      Portions Copyright 2008 Sun Microsystems, Inc.
 */
package org.opends.scratch.txn;



import org.opends.scratch.txn.dummy.CanceledOperationException;



/**
 * A call-back interface which backend implementations should use to
 * determine if the active operation should be aborted.
 */
public interface CancellationHandler
{

  /**
   * A default cancellation handler which is never canceled.
   */
  public static final CancellationHandler DEFAULT = new CancellationHandler()
  {

    @Override
    public void checkIfCanceled() throws CanceledOperationException
    {
      // Do nothing.
    }

  };



  /**
   * Checks to see if the active operation has been canceled in which
   * case {@link CanceledOperationException} will be thrown.
   *
   * @throws CanceledOperationException
   *           If the active operation should be canceled.
   */
  void checkIfCanceled() throws CanceledOperationException;
}