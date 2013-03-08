/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.opendj.rest2ldap;

import static org.forgerock.opendj.ldap.ErrorResultException.newErrorResult;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.json.resource.ServerContext;
import org.forgerock.opendj.ldap.AbstractAsynchronousConnection;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionEventListener;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.IntermediateResponseHandler;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.AbandonRequest;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.CompareRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ExtendedRequest;
import org.forgerock.opendj.ldap.requests.ModifyDNRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.requests.UnbindRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.CompareResult;
import org.forgerock.opendj.ldap.responses.ExtendedResult;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;

/**
 * Common context information passed to containers and mappers. A new context is
 * allocated for each REST request.
 */
final class Context implements Closeable {

    /*
     * A cached read request - see cachedReads for more information.
     */
    private static final class CachedRead implements SearchResultHandler {
        private SearchResultEntry cachedEntry;
        private final String cachedFilterString;
        private FutureResult<Result> cachedFuture; // Guarded by latch.
        private final CountDownLatch cachedFutureLatch = new CountDownLatch(1);
        private final SearchRequest cachedRequest;
        private volatile Result cachedResult;
        private final ConcurrentLinkedQueue<SearchResultHandler> waitingResultHandlers =
                new ConcurrentLinkedQueue<SearchResultHandler>();

        CachedRead(final SearchRequest request, final SearchResultHandler resultHandler) {
            this.cachedRequest = request;
            this.cachedFilterString = request.getFilter().toString();
            this.waitingResultHandlers.add(resultHandler);
        }

        @Override
        public boolean handleEntry(final SearchResultEntry entry) {
            cachedEntry = entry;
            return true;
        }

        @Override
        public void handleErrorResult(final ErrorResultException error) {
            handleResult(error.getResult());
        }

        @Override
        public boolean handleReference(final SearchResultReference reference) {
            // Ignore - should never happen for a base object search.
            return true;
        }

        @Override
        public void handleResult(final Result result) {
            cachedResult = result;
            drainQueue();
        }

        void addResultHandler(final SearchResultHandler resultHandler) {
            // Fast path.
            if (cachedResult != null) {
                invokeResultHandler(resultHandler);
                return;
            }
            // Enqueue and re-check.
            waitingResultHandlers.add(resultHandler);
            if (cachedResult != null) {
                drainQueue();
            }
        }

        FutureResult<Result> getFutureResult() {
            // Perform uninterrupted wait since this method is unlikely to block for a long time.
            boolean wasInterrupted = false;
            while (true) {
                try {
                    cachedFutureLatch.await();
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return cachedFuture;
                } catch (final InterruptedException e) {
                    wasInterrupted = true;
                }
            }
        }

        boolean isMatchingRead(final SearchRequest request) {
            // Cached reads are always base object.
            if (!request.getScope().equals(SearchScope.BASE_OBJECT)) {
                return false;
            }

            // Filters must match.
            if (!request.getFilter().toString().equals(cachedFilterString)) {
                return false;
            }

            // List of requested attributes must match.
            if (!request.getAttributes().equals(cachedRequest.getAttributes())) {
                return false;
            }

            // Don't need to check anything else.
            return true;
        }

        void setFuture(final FutureResult<Result> future) {
            cachedFuture = future;
            cachedFutureLatch.countDown();
        }

        private void drainQueue() {
            SearchResultHandler resultHandler;
            while ((resultHandler = waitingResultHandlers.poll()) != null) {
                invokeResultHandler(resultHandler);
            }
        }

        private void invokeResultHandler(final SearchResultHandler resultHandler) {
            if (cachedEntry != null) {
                resultHandler.handleEntry(cachedEntry);
            }
            if (cachedResult.isSuccess()) {
                resultHandler.handleResult(cachedResult);
            } else {
                resultHandler.handleErrorResult(newErrorResult(cachedResult));
            }
        }

    }

    /*
     * An LRU cache of recent reads requests. This is used in order to reduce
     * the number of repeated read operations performed when resolving DN
     * references.
     */
    @SuppressWarnings("serial")
    private final Map<DN, CachedRead> cachedReads = new LinkedHashMap<DN, CachedRead>() {
        private static final int MAX_CACHED_ENTRIES = 32;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<DN, CachedRead> eldest) {
            return size() > MAX_CACHED_ENTRIES;
        }
    };

    private final Config config;

    private final AtomicReference<Connection> connection = new AtomicReference<Connection>();

    private final ServerContext context;

    Context(final Config config, final ServerContext context) {
        this.config = config;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        final Connection c = connection.getAndSet(null);
        if (c != null) {
            c.close();
        }
    }

    Config getConfig() {
        return config;
    }

    Connection getConnection() {
        return connection.get();
    }

    ServerContext getServerContext() {
        return context;
    }

    void setConnection(final Connection connection) {
        if (!this.connection.compareAndSet(null, withCache(connection))) {
            throw new IllegalStateException("LDAP connection obtained multiple times");
        }
    }

    /*
     * Adds read caching support to the provided connection.
     */
    private Connection withCache(final Connection connection) {
        /*
         * We only use async methods so no need to wrap sync methods.
         */
        return new AbstractAsynchronousConnection() {

            @Override
            public FutureResult<Void> abandonAsync(final AbandonRequest request) {
                return connection.abandonAsync(request);
            }

            @Override
            public FutureResult<Result> addAsync(final AddRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super Result> resultHandler) {
                return connection.addAsync(request, intermediateResponseHandler, resultHandler);
            }

            @Override
            public void addConnectionEventListener(final ConnectionEventListener listener) {
                connection.addConnectionEventListener(listener);
            }

            @Override
            public FutureResult<BindResult> bindAsync(final BindRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super BindResult> resultHandler) {
                /*
                 * Simple brute force implementation in case the bind operation
                 * modifies an entry: clear the cachedReads.
                 */
                evictAll();
                return connection.bindAsync(request, intermediateResponseHandler, resultHandler);
            }

            @Override
            public void close(final UnbindRequest request, final String reason) {
                connection.close(request, reason);
            }

            @Override
            public FutureResult<CompareResult> compareAsync(final CompareRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super CompareResult> resultHandler) {
                return connection.compareAsync(request, intermediateResponseHandler, resultHandler);
            }

            @Override
            public FutureResult<Result> deleteAsync(final DeleteRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super Result> resultHandler) {
                evict(request.getName());
                return connection.deleteAsync(request, intermediateResponseHandler, resultHandler);
            }

            @Override
            public <R extends ExtendedResult> FutureResult<R> extendedRequestAsync(
                    final ExtendedRequest<R> request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super R> resultHandler) {
                /*
                 * Simple brute force implementation in case the extended
                 * operation modifies an entry: clear the cachedReads.
                 */
                evictAll();
                return connection.extendedRequestAsync(request, intermediateResponseHandler,
                        resultHandler);
            }

            @Override
            public boolean isClosed() {
                return connection.isClosed();
            }

            @Override
            public boolean isValid() {
                return connection.isValid();
            }

            @Override
            public FutureResult<Result> modifyAsync(final ModifyRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super Result> resultHandler) {
                evict(request.getName());
                return connection.modifyAsync(request, intermediateResponseHandler, resultHandler);
            }

            @Override
            public FutureResult<Result> modifyDNAsync(final ModifyDNRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final ResultHandler<? super Result> resultHandler) {
                // Simple brute force implementation: clear the cachedReads.
                evictAll();
                return connection
                        .modifyDNAsync(request, intermediateResponseHandler, resultHandler);
            }

            @Override
            public void removeConnectionEventListener(final ConnectionEventListener listener) {
                connection.removeConnectionEventListener(listener);
            }

            /*
             * Try and re-use a cached result if possible.
             */
            @Override
            public FutureResult<Result> searchAsync(final SearchRequest request,
                    final IntermediateResponseHandler intermediateResponseHandler,
                    final SearchResultHandler resultHandler) {

                /*
                 * Don't attempt caching if this search is not a read (base
                 * object), or if the search request passed in an intermediate
                 * response handler.
                 */
                if (!request.getScope().equals(SearchScope.BASE_OBJECT)
                        || intermediateResponseHandler != null) {
                    return connection.searchAsync(request, intermediateResponseHandler,
                            resultHandler);
                }

                // This is a read request and a candidate for caching.
                final CachedRead cachedRead;
                synchronized (cachedReads) {
                    cachedRead = cachedReads.get(request.getName());
                }
                if (cachedRead != null && cachedRead.isMatchingRead(request)) {
                    // The cached read matches this read request.
                    cachedRead.addResultHandler(resultHandler);
                    return cachedRead.getFutureResult();
                } else {
                    // Cache the read, possibly evicting a non-matching cached read.
                    final CachedRead pendingCachedRead = new CachedRead(request, resultHandler);
                    synchronized (cachedReads) {
                        cachedReads.put(request.getName(), pendingCachedRead);
                    }
                    final FutureResult<Result> future =
                            connection.searchAsync(request, intermediateResponseHandler,
                                    pendingCachedRead);
                    pendingCachedRead.setFuture(future);
                    return future;
                }
            }

            @Override
            public String toString() {
                return connection.toString();
            }

            private void evict(final DN name) {
                synchronized (cachedReads) {
                    cachedReads.remove(name);
                }
            }

            private void evictAll() {
                synchronized (cachedReads) {
                    cachedReads.clear();
                }
            }
        };
    }

}
