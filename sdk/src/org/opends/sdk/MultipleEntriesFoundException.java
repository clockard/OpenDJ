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
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 */

package org.opends.sdk;



import org.opends.sdk.responses.Result;



/**
 * Thrown when the result code returned in a Result indicates that the requested
 * single entry search operation or read operation failed because the Directory
 * Server returned multiple matching entries (or search references) when only a
 * single matching entry was expected. More specifically, this exception is used
 * for the {@link ResultCode#CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED
 * CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED} error result codes.
 */
@SuppressWarnings("serial")
public class MultipleEntriesFoundException extends ErrorResultException
{
  MultipleEntriesFoundException(final Result result)
  {
    super(result);
  }
}
