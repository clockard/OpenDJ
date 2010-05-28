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



import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.opends.sdk.ByteString;
import org.opends.sdk.ErrorResultException;
import org.opends.sdk.ResultCode;
import org.opends.sdk.responses.BindResult;
import org.opends.sdk.responses.Responses;

import com.sun.opends.sdk.util.Validator;



/**
 * CRAM-MD5 SASL bind request implementation.
 */
final class CRAMMD5SASLBindRequestImpl extends
    AbstractSASLBindRequest<CRAMMD5SASLBindRequest> implements
    CRAMMD5SASLBindRequest
{
  private final static class Client extends SASLBindClientImpl
  {
    private final SaslClient saslClient;
    private final String authenticationID;
    private final ByteString password;



    private Client(final CRAMMD5SASLBindRequestImpl initialBindRequest,
        final String serverName) throws ErrorResultException
    {
      super(initialBindRequest);

      this.authenticationID = initialBindRequest.getAuthenticationID();
      this.password = initialBindRequest.getPassword();

      try
      {
        saslClient = Sasl.createSaslClient(
            new String[] { SASL_MECHANISM_NAME }, null, SASL_DEFAULT_PROTOCOL,
            serverName, null, this);
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
        throw ErrorResultException.wrap(Responses.newResult(
            ResultCode.CLIENT_SIDE_LOCAL_ERROR).setCause(e));
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
        throws ErrorResultException
    {
      if (result.getResultCode() == ResultCode.SASL_BIND_IN_PROGRESS
          && result.getServerSASLCredentials() != null)
      {
        try
        {
          setNextSASLCredentials(saslClient.evaluateChallenge(result
              .getServerSASLCredentials().toByteArray()));
          return false;
        }
        catch (final SaslException e)
        {
          // FIXME: I18N need to have a better error message.
          // FIXME: Is this the best result code?
          throw ErrorResultException.wrap(Responses.newResult(
              ResultCode.CLIENT_SIDE_LOCAL_ERROR).setDiagnosticMessage(
              "An error occurred during multi-stage authentication")
              .setCause(e));
        }
      }

      return true;
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

  private ByteString password;



  CRAMMD5SASLBindRequestImpl(final String authenticationID,
      final ByteString password)
  {
    Validator.ensureNotNull(authenticationID, password);
    this.authenticationID = authenticationID;
    this.password = password;
  }



  /**
   * {@inheritDoc}
   */
  public BindClient createBindClient(final String serverName)
      throws ErrorResultException
  {
    return new Client(this, serverName);
  }



  /**
   * {@inheritDoc}
   */
  public String getAuthenticationID()
  {
    return authenticationID;
  }



  /**
   * {@inheritDoc}
   */
  public ByteString getPassword()
  {
    return password;
  }



  /**
   * {@inheritDoc}
   */
  public String getSASLMechanism()
  {
    return SASL_MECHANISM_NAME;
  }



  /**
   * {@inheritDoc}
   */
  public CRAMMD5SASLBindRequest setAuthenticationID(
      final String authenticationID) throws NullPointerException
  {
    Validator.ensureNotNull(authenticationID);
    this.authenticationID = authenticationID;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public CRAMMD5SASLBindRequest setPassword(final ByteString password)
      throws NullPointerException
  {
    Validator.ensureNotNull(password);
    this.password = password;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public CRAMMD5SASLBindRequest setPassword(final String password)
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
    builder.append("CRAMMD5SASLBindRequest(bindDN=");
    builder.append(getName());
    builder.append(", authentication=SASL");
    builder.append(", saslMechanism=");
    builder.append(getSASLMechanism());
    builder.append(", authenticationID=");
    builder.append(authenticationID);
    builder.append(", password=");
    builder.append(password);
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }
}
