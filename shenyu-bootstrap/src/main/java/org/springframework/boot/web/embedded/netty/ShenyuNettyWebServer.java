/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.netty;

import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.unix.Errors;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.ReactorHttpHandlerAdapter;
import org.reactivestreams.Publisher;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import reactor.netty.ChannelBindException;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.ShenyuHttpServerHandle;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@link WebServer} that can be used to control a Reactor Netty web server. Usually this
 * class should be created using the {@link ShenyuNettyReactiveWebServerFactory} and not
 * directly.
 *
 * @since 2.0.0
 */
public class ShenyuNettyWebServer implements WebServer {

    /**
     * Permission denied error code from {@code errno.h}.
     */
    private static final int ERROR_NO_EACCES = -13;

    private static final Predicate<HttpServerRequest> ALWAYS = request -> true;

    private static final Log LOGGER = LogFactory.getLog(ShenyuNettyWebServer.class);

    private final HttpServer httpServer;

    private final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler;

    private final Duration lifecycleTimeout;

    private final GracefulShutdown gracefulShutdown;

    private volatile DisposableServer disposableServer;

    private List<NettyRouteProvider> routeProviders = Collections.emptyList();

    public ShenyuNettyWebServer(final HttpServer httpServer, final ReactorHttpHandlerAdapter handlerAdapter, final Duration lifecycleTimeout,
                                final Shutdown shutdown) {
        Assert.notNull(httpServer, "HttpServer must not be null");
        Assert.notNull(handlerAdapter, "HandlerAdapter must not be null");
        this.lifecycleTimeout = lifecycleTimeout;
        this.handler = handlerAdapter;
        this.httpServer = httpServer.channelGroup(new DefaultChannelGroup(new DefaultEventExecutor()));
        this.gracefulShutdown = (shutdown == Shutdown.GRACEFUL) ? new GracefulShutdown(() -> this.disposableServer)
                : null;
    }

    @Override
    public void start() throws WebServerException {
        if (this.disposableServer == null) {
            try {
                this.disposableServer = startHttpServer();
            } catch (Exception ex) {
                PortInUseException.ifCausedBy(ex, ChannelBindException.class, bindException -> {
                    if (bindException.localPort() > 0 && !isPermissionDenied(bindException.getCause())) {
                        throw new PortInUseException(bindException.localPort(), ex);
                    }
                });
                throw new WebServerException("Unable to start Netty", ex);
            }
            if (this.disposableServer != null) {
                LOGGER.info("Netty started" + getStartedOnMessage(this.disposableServer));
            }
            startDaemonAwaitThread(this.disposableServer);
        }
    }

    /**
     * setRouteProviders.
     *
     * @param routeProviders routeProviders
     */
    public void setRouteProviders(final List<NettyRouteProvider> routeProviders) {
        this.routeProviders = routeProviders;
    }

    private String getStartedOnMessage(final DisposableServer server) {
        StringBuilder message = new StringBuilder();
        tryAppend(message, "port %s", server::port);
        tryAppend(message, "path %s", server::path);
        return (message.length() > 0) ? " on " + message : "";
    }

    private void tryAppend(final StringBuilder message, final String format, final Supplier<Object> supplier) {
        Object value = supplier.get();
        message.append((message.length() != 0) ? " " : "");
        message.append(String.format(format, value));
    }

    DisposableServer startHttpServer() {
        HttpServer server = this.httpServer;
        server = server.childObserve(new ShenyuHttpServerHandle(this.handler));
        if (this.lifecycleTimeout != null) {
            return server.bindNow(this.lifecycleTimeout);
        }
        return server.bindNow();
    }

    private boolean isPermissionDenied(final Throwable bindExceptionCause) {
        if (bindExceptionCause instanceof Errors.NativeIoException) {
            return ((Errors.NativeIoException) bindExceptionCause).expectedErr() == ERROR_NO_EACCES;
        }
        return false;
    }

    @Override
    public void shutDownGracefully(final GracefulShutdownCallback callback) {
        if (this.gracefulShutdown == null) {
            callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
            return;
        }
        this.gracefulShutdown.shutDownGracefully(callback);
    }

    private void startDaemonAwaitThread(final DisposableServer disposableServer) {
        Thread awaitThread = new Thread("server") {

            @Override
            public void run() {
                disposableServer.onDispose().block();
            }

        };
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void stop() throws WebServerException {
        if (this.disposableServer != null) {
            if (this.gracefulShutdown != null) {
                this.gracefulShutdown.abort();
            }
            try {
                if (this.lifecycleTimeout != null) {
                    this.disposableServer.disposeNow(this.lifecycleTimeout);
                } else {
                    this.disposableServer.disposeNow();
                }
            } catch (IllegalStateException ex) {
                // Continue
            }
            this.disposableServer = null;
        }
    }

    @Override
    public int getPort() {
        if (this.disposableServer != null) {
            try {
                return this.disposableServer.port();
            } catch (UnsupportedOperationException ex) {
                return -1;
            }
        }
        return -1;
    }
}
