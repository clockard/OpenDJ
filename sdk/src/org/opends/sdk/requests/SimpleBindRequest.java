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
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.opends.sdk.requests;



import java.util.List;

import org.opends.sdk.ByteString;
import org.opends.sdk.DecodeException;
import org.opends.sdk.DecodeOptions;
import org.opends.sdk.ErrorResultException;
import org.opends.sdk.controls.Control;
import org.opends.sdk.controls.ControlDecoder;



/**
 * The simple authentication method of the Bind Operation provides three
 * authentication mechanisms:
 * <ul>
 * <li>An anonymous authentication mechanism, in which both the name and
 * password are zero length.
 * <li>An unauthenticated authentication mechanism using credentials consisting
 * of a name and a zero length password.
 * <li>A name/password authentication mechanism using credentials consisting of
 * a name and a password.
 * </ul>
 */
public interface SimpleBindRequest extends BindRequest
{
  /**
   * {@inheritDoc}
   */
  SimpleBindRequest addControl(Control control)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * {@inheritDoc}
   */
  BindClient createBindClient(String serverName) throws ErrorResultException;



  /**
   * Returns the authentication mechanism identifier for this simple bind
   * request as defined by the LDAP protocol, which is always {@code 0x80}.
   *
   * @return The authentication mechanism identifier.
   */
  byte getAuthenticationType();



  /**
   * {@inheritDoc}
   */
  <C extends Control> C getControl(ControlDecoder<C> decoder,
      DecodeOptions options) throws NullPointerException, DecodeException;



  /**
   * {@inheritDoc}
   */
  List<Control> getControls();



  /**
   * {@inheritDoc}
   */
  String getName();



  /**
   * Returns the password of the Directory object that the client wishes to bind
   * as. The password may be empty (but never {@code null}) when used for of
   * anonymous or unauthenticated binds.
   *
   * @return The password of the Directory object that the client wishes to bind
   *         as.
   */
  ByteString getPassword();



  /**
   * Returns the password of the Directory object that the client wishes to bind
   * as decoded as a UTF-8 string. The password may be empty (but never {@code
   * null}) when used for of anonymous or unauthenticated binds.
   *
   * @return The password of the Directory object that the client wishes to bind
   *         as decoded as a UTF-8 string.
   */
  String getPasswordAsString();



  /**
   * Sets the name of the Directory object that the client wishes to bind as.
   * The name may be empty (but never {@code null} when used for of anonymous
   * binds, or when using SASL authentication. The server shall not dereference
   * any aliases in locating the named object.
   * <p>
   * The LDAP protocol defines the Bind name to be a distinguished name, however
   * some LDAP implementations have relaxed this constraint and allow other
   * identities to be used, such as the user's email address.
   *
   * @param name
   *          The name of the Directory object that the client wishes to bind
   *          as.
   * @return This bind request.
   * @throws UnsupportedOperationException
   *           If this bind request does not permit the distinguished name to be
   *           set.
   * @throws NullPointerException
   *           If {@code name} was {@code null}.
   */
  SimpleBindRequest setName(String name) throws UnsupportedOperationException,
      NullPointerException;



  /**
   * Sets the password of the Directory object that the client wishes to bind
   * as. The password may be empty (but never {@code null}) when used for of
   * anonymous or unauthenticated binds.
   *
   * @param password
   *          The password of the Directory object that the client wishes to
   *          bind as, which may be empty.
   * @return This simple bind request.
   * @throws UnsupportedOperationException
   *           If this simple bind request does not permit the password to be
   *           set.
   * @throws NullPointerException
   *           If {@code password} was {@code null}.
   */
  SimpleBindRequest setPassword(ByteString password)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * Sets the password of the Directory object that the client wishes to bind
   * as. The password will be converted to a UTF-8 octet string. The password
   * may be empty (but never {@code null}) when used for of anonymous or
   * unauthenticated binds.
   *
   * @param password
   *          The password of the Directory object that the client wishes to
   *          bind as, which may be empty.
   * @return This simple bind request.
   * @throws UnsupportedOperationException
   *           If this simple bind request does not permit the password to be
   *           set.
   * @throws NullPointerException
   *           If {@code password} was {@code null}.
   */
  SimpleBindRequest setPassword(String password)
      throws UnsupportedOperationException, NullPointerException;

}
