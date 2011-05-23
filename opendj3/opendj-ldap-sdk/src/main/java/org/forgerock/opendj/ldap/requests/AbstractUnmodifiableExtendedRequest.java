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

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.responses.ExtendedResult;
import org.forgerock.opendj.ldap.responses.ExtendedResultDecoder;

/**
 * An abstract unmodifiable Extended request which can be used as the basis for
 * implementing new unmodifiable Extended operations.
 *
 * @param <R>
 *          The type of extended request.
 * @param <S>
 *          The type of result.
 */
abstract class
    AbstractUnmodifiableExtendedRequest<R extends ExtendedRequest<S>,
                                        S extends ExtendedResult>
    extends AbstractUnmodifiableRequest<R>
    implements ExtendedRequest<S>
{
  AbstractUnmodifiableExtendedRequest(R impl) {
    super(impl);
  }

  public final String getOID() {
    return impl.getOID();
  }

  public final ExtendedResultDecoder<S> getResultDecoder() {
    return impl.getResultDecoder();
  }

  public final ByteString getValue() {
    return impl.getValue();
  }

  public final boolean hasValue() {
    return impl.hasValue();
  }
}
