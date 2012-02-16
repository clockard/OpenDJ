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
 *      Portions copyright 2012 ForgeRock AS.
 */

package com.forgerock.opendj.ldap;



import static com.forgerock.opendj.ldap.LDAPConstants.OID_NOTICE_OF_DISCONNECTION;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.forgerock.opendj.ldap.*;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.requests.*;
import org.forgerock.opendj.ldap.responses.*;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.ssl.SSLUtils;

import com.forgerock.opendj.util.StaticUtils;
import com.forgerock.opendj.util.Validator;



/**
 * Grizzly filter implementation for decoding LDAP requests and handling server
 * side logic for SSL and SASL operations over LDAP.
 */
final class LDAPServerFilter extends BaseFilter
{
  private abstract class AbstractHandler<R extends Result> implements
      IntermediateResponseHandler, ResultHandler<R>
  {
    protected final int messageID;
    protected final Connection<?> connection;



    protected AbstractHandler(final int messageID,
        final Connection<?> connection)
    {
      this.messageID = messageID;
      this.connection = connection;
    }



    @Override
    public final boolean handleIntermediateResponse(
        final IntermediateResponse response)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.intermediateResponse(asn1Writer, messageID, response);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
        return false;
      }
      finally
      {
        asn1Writer.recycle();
      }
      return true;
    }
  }



  private final class AddHandler extends AbstractHandler<Result>
  {
    private AddHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      handleResult(error.getResult());
    }



    @Override
    public void handleResult(final Result result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.addResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class BindHandler extends AbstractHandler<BindResult>
  {
    private BindHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      final Result result = error.getResult();
      if (result instanceof BindResult)
      {
        handleResult((BindResult) result);
      }
      else
      {
        final BindResult newResult = Responses.newBindResult(result
            .getResultCode());
        newResult.setDiagnosticMessage(result.getDiagnosticMessage());
        newResult.setMatchedDN(result.getMatchedDN());
        newResult.setCause(result.getCause());
        for (final Control control : result.getControls())
        {
          newResult.addControl(control);
        }
        handleResult(newResult);
      }
    }



    @Override
    public void handleResult(final BindResult result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.bindResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class ClientContextImpl implements LDAPClientContext
  {
    private final Connection<?> connection;

    private volatile boolean isClosed = false;

    private ServerConnection<Integer> serverConnection = null;



    private ClientContextImpl(final Connection<?> connection)
    {
      this.connection = connection;
    }



    @Override
    public void disconnect()
    {
      LDAPServerFilter.notifyConnectionDisconnected(connection, null, null);
    }



    @Override
    public void disconnect(final ResultCode resultCode, final String message)
    {
      Validator.ensureNotNull(resultCode);

      final GenericExtendedResult notification = Responses
          .newGenericExtendedResult(resultCode)
          .setOID(OID_NOTICE_OF_DISCONNECTION).setDiagnosticMessage(message);
      sendUnsolicitedNotification(notification);
      LDAPServerFilter.notifyConnectionDisconnected(connection, resultCode,
          message);
    }



    @Override
    public InetSocketAddress getLocalAddress()
    {
      return (InetSocketAddress) connection.getLocalAddress();
    }



    @Override
    public InetSocketAddress getPeerAddress()
    {
      return (InetSocketAddress) connection.getPeerAddress();
    }



    @Override
    public int getSecurityStrengthFactor()
    {
      int ssf = 0;
      final SSLEngine sslEngine = SSLUtils.getSSLEngine(connection);
      if (sslEngine != null)
      {
        final String cipherString = sslEngine.getSession().getCipherSuite();
        for (final Map.Entry<String, Integer> mapEntry : CIPHER_KEY_SIZES
            .entrySet())
        {
          if (cipherString.indexOf(mapEntry.getKey()) >= 0)
          {
            ssf = mapEntry.getValue();
            break;
          }
        }
      }

      return ssf;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed()
    {
      return isClosed;
    }



    @Override
    public void sendUnsolicitedNotification(final ExtendedResult notification)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.extendedResult(asn1Writer, 0, notification);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        LDAPServerFilter.notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }



    @Override
    public void enableConnectionSecurityLayer(
        final ConnectionSecurityLayer layer)
    {
      synchronized (this)
      {
        installFilter(new ConnectionSecurityLayerFilter(layer, connection
            .getTransport().getMemoryManager()));
      }
    }



    @Override
    public void enableTLS(final SSLContext sslContext,
        final String[] protocols, final String[] suites,
        final boolean wantClientAuth, final boolean needClientAuth)
    {
      Validator.ensureNotNull(sslContext);
      synchronized (this)
      {
        if (isTLSEnabled())
        {
          throw new IllegalStateException("TLS already enabled");
        }

        SSLEngineConfigurator sslEngineConfigurator =
            new SSLEngineConfigurator(sslContext, false, false, false);
        sslEngineConfigurator.setEnabledCipherSuites(suites);
        sslEngineConfigurator.setEnabledProtocols(protocols);
        sslEngineConfigurator.setWantClientAuth(wantClientAuth);
        sslEngineConfigurator.setNeedClientAuth(needClientAuth);
        installFilter(new SSLFilter(sslEngineConfigurator, null));
      }
    }



    /**
     * {@inheritDoc}
     */
    public String toString()
    {
      StringBuilder builder = new StringBuilder();
      builder.append("LDAPClientContext(");
      builder.append(getLocalAddress());
      builder.append(',');
      builder.append(getPeerAddress());
      builder.append(')');
      return builder.toString();
    }



    private void close()
    {
      isClosed = true;
    }



    private ServerConnection<Integer> getServerConnection()
    {
      return serverConnection;
    }



    private void setServerConnection(
        final ServerConnection<Integer> serverConnection)
    {
      this.serverConnection = serverConnection;
    }



    /**
     * Installs a new Grizzly filter (e.g. SSL/SASL) beneath the top-level LDAP
     * filter.
     *
     * @param filter
     *          The filter to be installed.
     */
    private void installFilter(final Filter filter)
    {
      // Determine the index where the filter should be added.
      final FilterChain oldFilterChain = (FilterChain) connection.getProcessor();
      int filterIndex = oldFilterChain.size() - 1;
      if (filter instanceof SSLFilter)
      {
        // Beneath any ConnectionSecurityLayerFilters if present, otherwise
        // beneath the LDAP filter.
        for (int i = oldFilterChain.size() - 2; i >= 0; i--)
        {
          if (!(oldFilterChain.get(i) instanceof ConnectionSecurityLayerFilter))
          {
            filterIndex = i + 1;
            break;
          }
        }
      }

      // Create the new filter chain.
      final FilterChain newFilterChain = FilterChainBuilder.stateless()
          .addAll(oldFilterChain).add(filterIndex, filter).build();
      connection.setProcessor(newFilterChain);
    }



    /**
     * Indicates whether or not TLS is enabled this provided connection.
     *
     * @return {@code true} if TLS is enabled on this connection, otherwise
     *         {@code false}.
     */
    private boolean isTLSEnabled()
    {
      synchronized (this)
      {
        final FilterChain currentFilterChain = (FilterChain) connection
            .getProcessor();
        for (Filter filter : currentFilterChain)
        {
          if (filter instanceof SSLFilter)
          {
            return true;
          }
        }
        return false;
      }
    }

  }



  private final class CompareHandler extends AbstractHandler<CompareResult>
  {
    private CompareHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      final Result result = error.getResult();
      if (result instanceof CompareResult)
      {
        handleResult((CompareResult) result);
      }
      else
      {
        final CompareResult newResult = Responses.newCompareResult(result
            .getResultCode());
        newResult.setDiagnosticMessage(result.getDiagnosticMessage());
        newResult.setMatchedDN(result.getMatchedDN());
        newResult.setCause(result.getCause());
        for (final Control control : result.getControls())
        {
          newResult.addControl(control);
        }
        handleResult(newResult);
      }
    }



    @Override
    public void handleResult(final CompareResult result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.compareResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class DeleteHandler extends AbstractHandler<Result>
  {
    private DeleteHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      handleResult(error.getResult());
    }



    @Override
    public void handleResult(final Result result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.deleteResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class ExtendedHandler<R extends ExtendedResult> extends
      AbstractHandler<R>
  {
    private ExtendedHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      final Result result = error.getResult();
      if (result instanceof ExtendedResult)
      {
        handleResult((ExtendedResult) result);
      }
      else
      {
        final ExtendedResult newResult = Responses
            .newGenericExtendedResult(result.getResultCode());
        newResult.setDiagnosticMessage(result.getDiagnosticMessage());
        newResult.setMatchedDN(result.getMatchedDN());
        newResult.setCause(result.getCause());
        for (final Control control : result.getControls())
        {
          newResult.addControl(control);
        }
        handleResult(newResult);
      }
    }



    @Override
    public void handleResult(final ExtendedResult result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.extendedResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class ModifyDNHandler extends AbstractHandler<Result>
  {
    private ModifyDNHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      handleResult(error.getResult());
    }



    @Override
    public void handleResult(final Result result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.modifyDNResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class ModifyHandler extends AbstractHandler<Result>
  {
    private ModifyHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      handleResult(error.getResult());
    }



    @Override
    public void handleResult(final Result result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.modifyResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  private final class SearchHandler extends AbstractHandler<Result> implements
      SearchResultHandler
  {
    private SearchHandler(final int messageID, final Connection<?> connection)
    {
      super(messageID, connection);
    }



    @Override
    public boolean handleEntry(final SearchResultEntry entry)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.searchResultEntry(asn1Writer, messageID, entry);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
        return false;
      }
      finally
      {
        asn1Writer.recycle();
      }
      return true;
    }



    @Override
    public void handleErrorResult(final ErrorResultException error)
    {
      handleResult(error.getResult());
    }



    @Override
    public boolean handleReference(final SearchResultReference reference)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.searchResultReference(asn1Writer, messageID, reference);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
        return false;
      }
      finally
      {
        asn1Writer.recycle();
      }
      return true;
    }



    @Override
    public void handleResult(final Result result)
    {
      final ASN1BufferWriter asn1Writer = ASN1BufferWriter.getWriter();
      try
      {
        LDAP_WRITER.searchResult(asn1Writer, messageID, result);
        connection.write(asn1Writer.getBuffer(), null);
      }
      catch (final IOException ioe)
      {
        notifyConnectionException(connection, ioe);
      }
      finally
      {
        asn1Writer.recycle();
      }
    }
  }



  // Map of cipher phrases to effective key size (bits). Taken from the
  // following RFCs: 5289, 4346, 3268,4132 and 4162.
  private static final Map<String, Integer> CIPHER_KEY_SIZES;

  static
  {
    CIPHER_KEY_SIZES = new LinkedHashMap<String, Integer>();
    CIPHER_KEY_SIZES.put("_WITH_AES_256_CBC_", 256);
    CIPHER_KEY_SIZES.put("_WITH_CAMELLIA_256_CBC_", 256);
    CIPHER_KEY_SIZES.put("_WITH_AES_256_GCM_", 256);
    CIPHER_KEY_SIZES.put("_WITH_3DES_EDE_CBC_", 112);
    CIPHER_KEY_SIZES.put("_WITH_AES_128_GCM_", 128);
    CIPHER_KEY_SIZES.put("_WITH_SEED_CBC_", 128);
    CIPHER_KEY_SIZES.put("_WITH_CAMELLIA_128_CBC_", 128);
    CIPHER_KEY_SIZES.put("_WITH_AES_128_CBC_", 128);
    CIPHER_KEY_SIZES.put("_WITH_IDEA_CBC_", 128);
    CIPHER_KEY_SIZES.put("_WITH_DES_CBC_", 56);
    CIPHER_KEY_SIZES.put("_WITH_RC2_CBC_40_", 40);
    CIPHER_KEY_SIZES.put("_WITH_RC4_40_", 40);
    CIPHER_KEY_SIZES.put("_WITH_DES40_CBC_", 40);
    CIPHER_KEY_SIZES.put("_WITH_NULL_", 0);
  }

  private static final LDAPWriter LDAP_WRITER = new LDAPWriter();

  private static final Attribute<ClientContextImpl> LDAP_CONNECTION_ATTR =
    Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("LDAPServerConnection");

  private static final Attribute<ASN1BufferReader> LDAP_ASN1_READER_ATTR =
    Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("LDAPASN1Reader");



  private static void notifyConnectionClosed(final Connection<?> connection,
      final int messageID, final UnbindRequest unbindRequest)
  {
    final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR
        .remove(connection);
    if (clientContext != null)
    {
      // Close the connection context.
      clientContext.close();

      // Notify the server connection: it may be null if disconnect is invoked
      // during accept.
      final ServerConnection<Integer> serverConnection = clientContext
          .getServerConnection();
      if (serverConnection != null)
      {
        serverConnection.handleConnectionClosed(messageID, unbindRequest);
      }

      // If this close was a result of an unbind request then the connection
      // won't actually be closed yet. To avoid TIME_WAIT TCP state, let the
      // client disconnect.
      if (unbindRequest != null)
      {
        return;
      }

      // Close the connection.
      try
      {
        connection.close();
      }
      catch (final IOException e)
      {
        StaticUtils.DEBUG_LOG.warning("Error closing connection: " + e);
      }
    }
  }



  private static void notifyConnectionDisconnected(
      final Connection<?> connection, final ResultCode resultCode,
      final String message)
  {
    final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR
        .remove(connection);
    if (clientContext != null)
    {
      // Close the connection context.
      clientContext.close();

      // Notify the server connection: it may be null if disconnect is invoked
      // during accept.
      final ServerConnection<Integer> serverConnection = clientContext
          .getServerConnection();
      if (serverConnection != null)
      {
        serverConnection.handleConnectionDisconnected(resultCode, message);
      }

      // Close the connection.
      try
      {
        connection.close();
      }
      catch (final IOException e)
      {
        StaticUtils.DEBUG_LOG.warning("Error closing connection: " + e);
      }
    }
  }



  private static void notifyConnectionException(final Connection<?> connection,
      final Throwable error)
  {
    final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR
        .remove(connection);
    if (clientContext != null)
    {
      // Close the connection context.
      clientContext.close();

      // Notify the server connection: it may be null if disconnect is invoked
      // during accept.
      final ServerConnection<Integer> serverConnection = clientContext
          .getServerConnection();
      if (serverConnection != null)
      {
        serverConnection.handleConnectionError(error);
      }

      // Close the connection.
      try
      {
        connection.close();
      }
      catch (final IOException e)
      {
        StaticUtils.DEBUG_LOG.warning("Error closing connection: " + e);
      }
    }
  }



  private final AbstractLDAPMessageHandler<FilterChainContext> serverRequestHandler =
    new AbstractLDAPMessageHandler<FilterChainContext>()
  {
    @Override
    public void abandonRequest(final FilterChainContext ctx,
        final int messageID, final AbandonRequest request)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        conn.handleAbandon(messageID, request);
      }
    }



    @Override
    public void addRequest(final FilterChainContext ctx, final int messageID,
        final AddRequest request) throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final AddHandler handler = new AddHandler(messageID,
            ctx.getConnection());
        conn.handleAdd(messageID, request, handler, handler);
      }
    }



    @Override
    public void bindRequest(final FilterChainContext ctx, final int messageID,
        final int version, final GenericBindRequest bindContext)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final BindHandler handler = new BindHandler(messageID,
            ctx.getConnection());
        conn.handleBind(messageID, version, bindContext, handler, handler);
      }
    }



    @Override
    public void compareRequest(final FilterChainContext ctx,
        final int messageID, final CompareRequest request)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final CompareHandler handler = new CompareHandler(messageID,
            ctx.getConnection());
        conn.handleCompare(messageID, request, handler, handler);
      }
    }



    @Override
    public void deleteRequest(final FilterChainContext ctx,
        final int messageID, final DeleteRequest request)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final DeleteHandler handler = new DeleteHandler(messageID,
            ctx.getConnection());
        conn.handleDelete(messageID, request, handler, handler);
      }
    }



    @Override
    public <R extends ExtendedResult> void extendedRequest(
        final FilterChainContext ctx, final int messageID,
        final ExtendedRequest<R> request) throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final ExtendedHandler<R> handler = new ExtendedHandler<R>(messageID,
            ctx.getConnection());
        conn.handleExtendedRequest(messageID, request, handler, handler);
      }
    }



    @Override
    public void modifyDNRequest(final FilterChainContext ctx,
        final int messageID, final ModifyDNRequest request)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final ModifyDNHandler handler = new ModifyDNHandler(messageID,
            ctx.getConnection());
        conn.handleModifyDN(messageID, request, handler, handler);
      }
    }



    @Override
    public void modifyRequest(final FilterChainContext ctx,
        final int messageID, final ModifyRequest request)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final ModifyHandler handler = new ModifyHandler(messageID,
            ctx.getConnection());
        conn.handleModify(messageID, request, handler, handler);
      }
    }



    @Override
    public void searchRequest(final FilterChainContext ctx,
        final int messageID, final SearchRequest request)
        throws UnexpectedRequestException
    {
      final ClientContextImpl clientContext = LDAP_CONNECTION_ATTR.get(ctx
          .getConnection());
      if (clientContext != null)
      {
        final ServerConnection<Integer> conn = clientContext
            .getServerConnection();
        final SearchHandler handler = new SearchHandler(messageID,
            ctx.getConnection());
        conn.handleSearch(messageID, request, handler, handler);
      }
    }



    @Override
    public void unbindRequest(final FilterChainContext ctx,
        final int messageID, final UnbindRequest request)
    {
      notifyConnectionClosed(ctx.getConnection(), messageID, request);
    }



    @Override
    public void unrecognizedMessage(final FilterChainContext ctx,
        final int messageID, final byte messageTag,
        final ByteString messageBytes)
    {
      notifyConnectionException(ctx.getConnection(),
          new UnsupportedMessageException(messageID, messageTag, messageBytes));
    }
  };

  private final int maxASN1ElementSize;

  private final LDAPReader ldapReader;

  private final LDAPListenerImpl listener;



  LDAPServerFilter(final LDAPListenerImpl listener,
      final LDAPReader ldapReader, final int maxASN1ElementSize)
  {
    this.listener = listener;
    this.ldapReader = ldapReader;
    this.maxASN1ElementSize = maxASN1ElementSize;
  }



  @Override
  public void exceptionOccurred(final FilterChainContext ctx,
      final Throwable error)
  {
    notifyConnectionException(ctx.getConnection(), error);
  }



  @Override
  public NextAction handleAccept(final FilterChainContext ctx)
      throws IOException
  {
    final Connection<?> connection = ctx.getConnection();
    connection.configureBlocking(true);
    try
    {
      final ClientContextImpl clientContext = new ClientContextImpl(connection);
      final ServerConnection<Integer> serverConn = listener
          .getConnectionFactory().handleAccept(clientContext);
      clientContext.setServerConnection(serverConn);
      LDAP_CONNECTION_ATTR.set(connection, clientContext);
    }
    catch (final ErrorResultException e)
    {
      connection.close();
    }

    return ctx.getStopAction();
  }



  @Override
  public NextAction handleClose(final FilterChainContext ctx)
      throws IOException
  {
    notifyConnectionClosed(ctx.getConnection(), -1, null);
    return ctx.getStopAction();
  }



  @Override
  public NextAction handleRead(final FilterChainContext ctx) throws IOException
  {
    final Buffer buffer = (Buffer) ctx.getMessage();
    ASN1BufferReader asn1Reader = LDAP_ASN1_READER_ATTR
        .get(ctx.getConnection());
    if (asn1Reader == null)
    {
      asn1Reader = new ASN1BufferReader(maxASN1ElementSize, ctx.getConnection()
          .getTransport().getMemoryManager());
      LDAP_ASN1_READER_ATTR.set(ctx.getConnection(), asn1Reader);
    }
    asn1Reader.appendBytesRead(buffer);

    try
    {
      while (asn1Reader.elementAvailable())
      {
        ldapReader.decode(asn1Reader, serverRequestHandler, ctx);
      }
    }
    finally
    {
      asn1Reader.disposeBytesRead();
    }

    return ctx.getStopAction();
  }
}
