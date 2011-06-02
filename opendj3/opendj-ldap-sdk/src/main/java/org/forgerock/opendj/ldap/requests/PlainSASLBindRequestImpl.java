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



import static org.forgerock.opendj.ldap.ErrorResultException.newErrorResult;

import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.responses.BindResult;

import com.forgerock.opendj.util.Validator;



/**
 * Plain SASL bind request implementation.
 */
final class PlainSASLBindRequestImpl extends
    AbstractSASLBindRequest<PlainSASLBindRequest> implements
    PlainSASLBindRequest
{
  private final static class Client extends SASLBindClientImpl
  {
    private final SaslClient saslClient;
    private final String authenticationID;
    private final ByteString password;



    private Client(final PlainSASLBindRequestImpl initialBindRequest,
        final String serverName) throws ErrorResultException
    {
      super(initialBindRequest);

      this.authenticationID = initialBindRequest.getAuthenticationID();
      this.password = initialBindRequest.getPassword();

      try
      {
        saslClient = Sasl.createSaslClient(
            new String[] { SASL_MECHANISM_NAME }, initialBindRequest
                .getAuthorizationID(), SASL_DEFAULT_PROTOCOL, serverName, null,
            this);

        if (saslClient.hasInitialResponse())
        {
          setNextSASLCredentials(saslClient.evaluateChallenge(new byte[0]));
        }
        else
        {
          setNextSASLCredentials((ByteString) null);
        }
      }
      catch (final SaslException e)
      {
        throw newErrorResult(ResultCode.CLIENT_SIDE_LOCAL_ERROR, e);
      }
    }



    @Override
    public void dispose()
    {
      try
      {
        saslClient.dispose();
      }
      catch (final SaslException ignored)
      {
        // Ignore the SASL exception.
      }
    }



    @Override
    public boolean evaluateResult(final BindResult result)
    {
      return saslClient.isComplete();
    }



    @Override
    void handle(final NameCallback callback)
        throws UnsupportedCallbackException
    {
      callback.setName(authenticationID);
    }



    @Override
    void handle(final PasswordCallback callback)
        throws UnsupportedCallbackException
    {
      callback.setPassword(password.toString().toCharArray());
    }
  }



  private String authenticationID;
  private String authorizationID;

  private ByteString password;



  PlainSASLBindRequestImpl(final String authenticationID,
      final ByteString password)
  {
    Validator.ensureNotNull(authenticationID, password);
    this.authenticationID = authenticationID;
    this.password = password;
  }



  /**
   * Creates a new plain SASL bind request that is an exact copy of the
   * provided request.
   *
   * @param plainSASLBindRequest
   *          The plain SASL bind request to be copied.
   * @throws NullPointerException
   *           If {@code plainSASLBindRequest} was {@code null} .
   */
  PlainSASLBindRequestImpl(
      final PlainSASLBindRequest plainSASLBindRequest)
      throws NullPointerException
  {
    super(plainSASLBindRequest);
    this.authenticationID = plainSASLBindRequest.getAuthenticationID();
    this.authorizationID = plainSASLBindRequest.getAuthorizationID();
    this.password = plainSASLBindRequest.getPassword();
  }



  public BindClient createBindClient(final String serverName)
      throws ErrorResultException
  {
    return new Client(this, serverName);
  }



  public String getAuthenticationID()
  {
    return authenticationID;
  }



  public String getAuthorizationID()
  {
    return authorizationID;
  }



  public ByteString getPassword()
  {
    return password;
  }



  public String getSASLMechanism()
  {
    return SASL_MECHANISM_NAME;
  }



  public PlainSASLBindRequest setAuthenticationID(final String authenticationID)
  {
    Validator.ensureNotNull(authenticationID);
    this.authenticationID = authenticationID;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public PlainSASLBindRequest setAuthorizationID(final String authorizationID)
  {
    this.authorizationID = authorizationID;
    return this;
  }



  public PlainSASLBindRequest setPassword(final ByteString password)
  {
    Validator.ensureNotNull(password);
    this.password = password;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public PlainSASLBindRequest setPassword(final char[] password)
      throws NullPointerException
  {
    Validator.ensureNotNull(password);
    this.password = ByteString.valueOf(password);
    return this;
  }



  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("PlainSASLBindRequest(bindDN=");
    builder.append(getName());
    builder.append(", authentication=SASL");
    builder.append(", saslMechanism=");
    builder.append(getSASLMechanism());
    builder.append(", authenticationID=");
    builder.append(authenticationID);
    builder.append(", authorizationID=");
    builder.append(authorizationID);
    builder.append(", password=");
    builder.append(password);
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }
}
