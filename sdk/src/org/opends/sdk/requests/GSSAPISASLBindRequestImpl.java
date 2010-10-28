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



import static com.sun.opends.sdk.messages.Messages.ERR_LDAPAUTH_GSSAPI_LOCAL_AUTHENTICATION_FAILED;
import static com.sun.opends.sdk.messages.Messages.ERR_SASL_CONTEXT_CREATE_ERROR;
import static com.sun.opends.sdk.messages.Messages.ERR_SASL_PROTOCOL_ERROR;
import static com.sun.opends.sdk.util.StaticUtils.getExceptionMessage;
import static com.sun.opends.sdk.util.StaticUtils.joinCollection;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.opends.sdk.*;
import org.opends.sdk.responses.BindResult;
import org.opends.sdk.responses.Responses;

import com.sun.opends.sdk.util.StaticUtils;
import com.sun.opends.sdk.util.Validator;
import com.sun.security.auth.callback.TextCallbackHandler;
import com.sun.security.auth.module.Krb5LoginModule;



/**
 * GSSAPI SASL bind request implementation.
 */
final class GSSAPISASLBindRequestImpl extends
    AbstractSASLBindRequest<GSSAPISASLBindRequest> implements
    GSSAPISASLBindRequest
{
  private final static class Client extends SASLBindClientImpl
  {
    private static Subject kerberos5Login(final String authenticationID,
        final ByteString password, final String realm, final String kdc)
        throws ErrorResultException
    {
      if (authenticationID == null)
      {
        // FIXME: I18N need to have a better error message.
        // FIXME: Is this the best result code?
        throw ErrorResultException.wrap(Responses.newResult(
            ResultCode.CLIENT_SIDE_LOCAL_ERROR).setDiagnosticMessage(
            "No authentication ID specified for GSSAPI SASL authentication"));
      }

      if (password == null)
      {
        // FIXME: I18N need to have a better error message.
        // FIXME: Is this the best result code?
        throw ErrorResultException.wrap(Responses.newResult(
            ResultCode.CLIENT_SIDE_LOCAL_ERROR).setDiagnosticMessage(
            "No password specified for GSSAPI SASL authentication"));
      }

      final Map<String, Object> state = new HashMap<String, Object>();
      state.put("javax.security.auth.login.name", authenticationID);
      state.put("javax.security.auth.login.password", password.toString()
          .toCharArray());
      state.put("javax.security.auth.useSubjectCredsOnly", "true");
      state.put("java.security.krb5.realm", realm);
      state.put("java.security.krb5.kdc", kdc);

      final Map<String, Object> options = new HashMap<String, Object>();
      options.put("tryFirstPass", "true");
      options.put("useTicketCache", "true");
      options.put("doNotPrompt", "true");
      options.put("storePass", "false");
      options.put("forwardable", "true");

      final Subject subject = new Subject();
      final Krb5LoginModule login = new Krb5LoginModule();
      login.initialize(subject, new TextCallbackHandler(), state, options);
      try
      {
        if (login.login())
        {
          login.commit();
        }
      }
      catch (final LoginException e)
      {
        // FIXME: Is this the best result code?
        final LocalizableMessage message = ERR_LDAPAUTH_GSSAPI_LOCAL_AUTHENTICATION_FAILED
            .get(StaticUtils.getExceptionMessage(e));
        throw ErrorResultException.wrap(Responses
            .newResult(ResultCode.CLIENT_SIDE_LOCAL_ERROR)
            .setDiagnosticMessage(message.toString()).setCause(e));
      }
      return subject;
    }



    private final SaslClient saslClient;
    private final String authorizationID;
    private final Subject subject;

    private BindResult lastResult;

    private final PrivilegedExceptionAction<Boolean> evaluateAction =
      new PrivilegedExceptionAction<Boolean>()
    {
      @Override
      public Boolean run() throws ErrorResultException
      {
        try
        {
          setNextSASLCredentials(saslClient.evaluateChallenge(lastResult
              .getServerSASLCredentials() == null ? new byte[0] : lastResult
              .getServerSASLCredentials().toByteArray()));
          return saslClient.isComplete();
        }
        catch (final SaslException e)
        {
          // FIXME: I18N need to have a better error message.
          // FIXME: Is this the best result code?
          throw ErrorResultException.wrap(Responses
              .newResult(ResultCode.CLIENT_SIDE_LOCAL_ERROR)
              .setDiagnosticMessage(
                  "An error occurred during multi-stage authentication")
              .setCause(e));
        }
      }
    };



    private Client(final GSSAPISASLBindRequestImpl initialBindRequest,
        final String serverName) throws ErrorResultException
    {
      super(initialBindRequest);

      this.authorizationID = initialBindRequest.getAuthorizationID();
      if (initialBindRequest.getSubject() != null)
      {
        this.subject = initialBindRequest.getSubject();
      }
      else
      {
        this.subject = kerberos5Login(initialBindRequest.getAuthenticationID(),
            initialBindRequest.getPassword(), initialBindRequest.getRealm(),
            initialBindRequest.getKDCAddress());
      }

      try
      {
        this.saslClient = Subject.doAs(subject,
            new PrivilegedExceptionAction<SaslClient>()
            {
              @Override
              public SaslClient run() throws ErrorResultException
              {
                // Create property map containing all the parameters.
                final Map<String, String> props = new HashMap<String, String>();

                final List<String> qopValues = initialBindRequest.getQOPs();
                if (!qopValues.isEmpty())
                {
                  props.put(Sasl.QOP, joinCollection(qopValues, ","));
                }

                final Boolean serverAuth = initialBindRequest.isServerAuth();
                if (serverAuth != null)
                {
                  props.put(Sasl.SERVER_AUTH, String.valueOf(serverAuth));
                }

                Integer size = initialBindRequest.getMaxReceiveBufferSize();
                if (size != null)
                {
                  props.put(Sasl.MAX_BUFFER, String.valueOf(size));
                }

                size = initialBindRequest.getMaxSendBufferSize();
                if (size != null)
                {
                  props.put("javax.security.sasl.sendmaxbuffer",
                      String.valueOf(size));
                }

                for (final Map.Entry<String, String> e : initialBindRequest
                    .getAdditionalAuthParams().entrySet())
                {
                  props.put(e.getKey(), e.getValue());
                }

                try
                {
                  final SaslClient saslClient = Sasl.createSaslClient(
                      new String[] { SASL_MECHANISM_NAME }, authorizationID,
                      SASL_DEFAULT_PROTOCOL, serverName, props, Client.this);
                  if (saslClient.hasInitialResponse())
                  {
                    setNextSASLCredentials(saslClient
                        .evaluateChallenge(new byte[0]));
                  }
                  else
                  {
                    setNextSASLCredentials((ByteString) null);
                  }
                  return saslClient;
                }
                catch (final SaslException e)
                {
                  throw ErrorResultException.wrap(Responses.newResult(
                      ResultCode.CLIENT_SIDE_LOCAL_ERROR).setCause(e));
                }
              }
            });
      }
      catch (final PrivilegedActionException e)
      {
        if (e.getCause() instanceof ErrorResultException)
        {
          throw (ErrorResultException) e.getCause();
        }
        else
        {
          // This should not happen. Must be a bug.
          final LocalizableMessage msg = ERR_SASL_CONTEXT_CREATE_ERROR.get(
              SASL_MECHANISM_NAME, getExceptionMessage(e));
          throw ErrorResultException.wrap(Responses
              .newResult(ResultCode.CLIENT_SIDE_LOCAL_ERROR)
              .setDiagnosticMessage(msg.toString()).setCause(e));
        }
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
      this.lastResult = result;
      try
      {
        return Subject.doAs(subject, evaluateAction);
      }
      catch (final PrivilegedActionException e)
      {
        if (e.getCause() instanceof ErrorResultException)
        {
          throw (ErrorResultException) e.getCause();
        }
        else
        {
          // This should not happen. Must be a bug.
          final LocalizableMessage msg = ERR_SASL_PROTOCOL_ERROR.get(
              SASL_MECHANISM_NAME, getExceptionMessage(e));
          throw ErrorResultException.wrap(Responses
              .newResult(ResultCode.CLIENT_SIDE_LOCAL_ERROR)
              .setDiagnosticMessage(msg.toString()).setCause(e));
        }
      }
    }



    @Override
    public ConnectionSecurityLayer getConnectionSecurityLayer()
    {
      final String qop = (String) saslClient.getNegotiatedProperty(Sasl.QOP);
      if (qop.equalsIgnoreCase("auth-int") || qop.equalsIgnoreCase("auth-conf"))
      {
        return this;
      }
      else
      {
        return null;
      }
    }



    @Override
    public byte[] unwrap(final byte[] incoming, final int offset, final int len)
        throws ErrorResultException
    {
      try
      {
        return saslClient.unwrap(incoming, offset, len);
      }
      catch (final SaslException e)
      {
        final LocalizableMessage msg = ERR_SASL_PROTOCOL_ERROR.get(
            SASL_MECHANISM_NAME, getExceptionMessage(e));
        throw ErrorResultException.wrap(Responses
            .newResult(ResultCode.CLIENT_SIDE_DECODING_ERROR)
            .setDiagnosticMessage(msg.toString()).setCause(e));
      }
    }



    @Override
    public byte[] wrap(final byte[] outgoing, final int offset, final int len)
        throws ErrorResultException
    {
      try
      {
        return saslClient.wrap(outgoing, offset, len);
      }
      catch (final SaslException e)
      {
        final LocalizableMessage msg = ERR_SASL_PROTOCOL_ERROR.get(
            SASL_MECHANISM_NAME, getExceptionMessage(e));
        throw ErrorResultException.wrap(Responses
            .newResult(ResultCode.CLIENT_SIDE_ENCODING_ERROR)
            .setDiagnosticMessage(msg.toString()).setCause(e));
      }
    }

  }



  // If null then authenticationID and password must be present.
  private Subject subject = null;

  // Ignored if subject is non-null.
  private String authenticationID = null;
  private ByteString password = null;
  private String realm = null;

  private String kdcAddress = null;

  // Optional authorization ID.
  private String authorizationID = null;

  private final Map<String, String> additionalAuthParams = new LinkedHashMap<String, String>();
  private final List<String> qopValues = new LinkedList<String>();

  // Don't use primitives for these so that we can distinguish between default
  // settings (null) and values set by the caller.
  private Boolean serverAuth = null;
  private Integer maxReceiveBufferSize = null;
  private Integer maxSendBufferSize = null;



  GSSAPISASLBindRequestImpl(final String authenticationID,
      final ByteString password)
  {
    Validator.ensureNotNull(authenticationID, password);
    this.authenticationID = authenticationID;
    this.password = password;
  }



  /**
   * Creates a new GSSAPI SASL bind request that is an exact copy of the
   * provided request.
   *
   * @param gssapiSASLBindRequest
   *          The GSSAPI SASL bind request to be copied.
   * @throws NullPointerException
   *           If {@code gssAPISASLBindRequest} was {@code null}.
   */
  GSSAPISASLBindRequestImpl(
      final GSSAPISASLBindRequest gssapiSASLBindRequest)
      throws NullPointerException
  {
    super(gssapiSASLBindRequest);
    this.subject = gssapiSASLBindRequest.getSubject();

    this.authenticationID = gssapiSASLBindRequest.getAuthenticationID();
    this.password = gssapiSASLBindRequest.getPassword();
    this.realm = gssapiSASLBindRequest.getRealm();

    this.kdcAddress = gssapiSASLBindRequest.getKDCAddress();

    this.authorizationID = gssapiSASLBindRequest.getAuthorizationID();

    this.additionalAuthParams.putAll(
        gssapiSASLBindRequest.getAdditionalAuthParams());
    this.qopValues.addAll(gssapiSASLBindRequest.getQOPs());

    this.serverAuth = gssapiSASLBindRequest.isServerAuth();
    this.maxReceiveBufferSize = gssapiSASLBindRequest.getMaxReceiveBufferSize();
    this.maxSendBufferSize = gssapiSASLBindRequest.getMaxSendBufferSize();
  }



  GSSAPISASLBindRequestImpl(final Subject subject)
  {
    Validator.ensureNotNull(subject);
    this.subject = subject;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest addAdditionalAuthParam(final String name,
      final String value) throws UnsupportedOperationException,
      NullPointerException
  {
    Validator.ensureNotNull(name, value);
    additionalAuthParams.put(name, value);
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest addQOP(final String... qopValues)
      throws UnsupportedOperationException, NullPointerException
  {
    for (final String qopValue : qopValues)
    {
      this.qopValues.add(Validator.ensureNotNull(qopValue));
    }
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public BindClient createBindClient(final String serverName)
      throws ErrorResultException
  {
    return new Client(this, serverName);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getAdditionalAuthParams()
  {
    return additionalAuthParams;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuthenticationID()
  {
    return authenticationID;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuthorizationID()
  {
    return authorizationID;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getKDCAddress()
  {
    return kdcAddress;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxReceiveBufferSize()
  {
    return maxReceiveBufferSize == null ? 65536 : maxReceiveBufferSize;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxSendBufferSize()
  {
    return maxSendBufferSize == null ? 65536 : maxSendBufferSize;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ByteString getPassword()
  {
    return password;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getQOPs()
  {
    return qopValues;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getRealm()
  {
    return realm;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String getSASLMechanism()
  {
    return SASL_MECHANISM_NAME;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Subject getSubject()
  {
    return subject;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isServerAuth()
  {
    return serverAuth == null ? false : serverAuth;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setAuthenticationID(final String authenticationID)
      throws NullPointerException
  {
    Validator.ensureNotNull(authenticationID);
    this.authenticationID = authenticationID;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setAuthorizationID(final String authorizationID)
  {
    this.authorizationID = authorizationID;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setKDCAddress(final String address)
  {
    this.kdcAddress = address;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setMaxReceiveBufferSize(final int size)
      throws UnsupportedOperationException
  {
    maxReceiveBufferSize = size;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setMaxSendBufferSize(final int size)
      throws UnsupportedOperationException
  {
    maxSendBufferSize = size;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setPassword(final ByteString password)
      throws NullPointerException
  {
    Validator.ensureNotNull(password);
    this.password = password;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setPassword(final String password)
      throws NullPointerException
  {
    Validator.ensureNotNull(password);
    this.password = ByteString.valueOf(password);
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setRealm(final String realm)
  {
    this.realm = realm;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setServerAuth(final boolean serverAuth)
      throws UnsupportedOperationException
  {
    this.serverAuth = serverAuth;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public GSSAPISASLBindRequest setSubject(final Subject subject)
      throws NullPointerException
  {
    this.subject = subject;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("GSSAPISASLBindRequest(bindDN=");
    builder.append(getName());
    builder.append(", authentication=SASL");
    builder.append(", saslMechanism=");
    builder.append(getSASLMechanism());
    if (subject != null)
    {
      builder.append(", subject=");
      builder.append(subject);
    }
    else
    {
      builder.append(", authenticationID=");
      builder.append(authenticationID);
      builder.append(", authorizationID=");
      builder.append(authorizationID);
      builder.append(", realm=");
      builder.append(realm);
      builder.append(", password=");
      builder.append(password);
    }
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }

}
