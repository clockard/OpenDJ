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



import static com.forgerock.opendj.ldap.LDAPConstants.TYPE_AUTHENTICATION_SASL;



/**
 * An abstract SASL Bind request which can be used as the basis for implementing
 * new SASL authentication methods.
 *
 * @param <R>
 *          The type of SASL Bind request.
 */
abstract class AbstractSASLBindRequest<R extends SASLBindRequest> extends
    AbstractBindRequest<R> implements SASLBindRequest
{

  AbstractSASLBindRequest()
  {

  }



  AbstractSASLBindRequest(SASLBindRequest saslBindRequest)
  {
    super(saslBindRequest);
  }



  public final byte getAuthenticationType()
  {
    return TYPE_AUTHENTICATION_SASL;
  }



  @Override
  public final String getName()
  {
    return "".intern();
  }

}
