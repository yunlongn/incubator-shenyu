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

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServer;
import org.springframework.util.Assert;
import org.apache.shenyu.web.adater.ReactorHttpHandlerAdapter;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ShenyuNettyReactiveWebServerFactory} that can be used to create {@link ShenyuNettyWebServer}.
 *
 * @since 2.0.0
 */
public class ShenyuNettyReactiveWebServerFactory extends AbstractConfigurableWebServerFactory {

    private Set<NettyServerCustomizer> serverCustomizers = new LinkedHashSet<>();

    private List<NettyRouteProvider> routeProviders = new ArrayList<>();

    private Duration lifecycleTimeout;

    private boolean useForwardHeaders;

    private Shutdown shutdown;

    public ShenyuNettyReactiveWebServerFactory() {
    }

    public ShenyuNettyReactiveWebServerFactory(final int port) {
        super(port);
    }

    /**
     * getWebServer.
     *
     * @return {@link WebServer}
     */
    public WebServer getWebServer() {
        HttpServer httpServer = createHttpServer();
        ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter();
        ShenyuNettyWebServer webServer = createShenyuNettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout,
                getShutdown());
        webServer.setRouteProviders(this.routeProviders);
        return webServer;
    }

    ShenyuNettyWebServer createShenyuNettyWebServer(final HttpServer httpServer, final ReactorHttpHandlerAdapter handlerAdapter,
                                                    final Duration lifecycleTimeout, final Shutdown shutdown) {
        return new ShenyuNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown);
    }

    /**
     * Returns a mutable collection of the {@link NettyServerCustomizer}s that will be
     * applied to the Netty server builder.
     * @return the customizers that will be applied
     */
    public Collection<NettyServerCustomizer> getServerCustomizers() {
        return this.serverCustomizers;
    }

    /**
     * Set {@link NettyServerCustomizer}s that should be applied to the Netty server
     * builder. Calling this method will replace any existing customizers.
     * @param serverCustomizers the customizers to set
     */
    public void setServerCustomizers(final Collection<? extends NettyServerCustomizer> serverCustomizers) {
        Assert.notNull(serverCustomizers, "ServerCustomizers must not be null");
        this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
    }

    /**
     * Add {@link NettyServerCustomizer}s that should be applied while building the
     * server.
     * @param serverCustomizers the customizers to add
     */
    public void addServerCustomizers(final NettyServerCustomizer... serverCustomizers) {
        Assert.notNull(serverCustomizers, "ServerCustomizer must not be null");
        this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
    }

    /**
     * Add {@link NettyRouteProvider}s that should be applied, in order, before the
     * handler for the Spring application.
     * @param routeProviders the route providers to add
     */
    public void addRouteProviders(final NettyRouteProvider... routeProviders) {
        Assert.notNull(routeProviders, "NettyRouteProvider must not be null");
        this.routeProviders.addAll(Arrays.asList(routeProviders));
    }

    /**
     * Set the maximum amount of time that should be waited when starting or stopping the
     * server.
     * @param lifecycleTimeout the lifecycle timeout
     */
    public void setLifecycleTimeout(final Duration lifecycleTimeout) {
        this.lifecycleTimeout = lifecycleTimeout;
    }

    /**
     * Set if x-forward-* headers should be processed.
     * @param useForwardHeaders if x-forward headers should be used
     * @since 2.1.0
     */
    public void setUseForwardHeaders(final boolean useForwardHeaders) {
        this.useForwardHeaders = useForwardHeaders;
    }

    @Override
    public void setShutdown(final Shutdown shutdown) {
        this.shutdown = shutdown;
    }

    @Override
    public Shutdown getShutdown() {
        return this.shutdown;
    }

    private HttpServer createHttpServer() {
        HttpServer server = HttpServer.create();
        server = server.bindAddress(this::getListenAddress);
        if (getSsl() != null && getSsl().isEnabled()) {
            server = customizeSslConfiguration(server);
        }
        if (getCompression() != null && getCompression().getEnabled()) {
            CompressionCustomizer compressionCustomizer = new CompressionCustomizer(getCompression());
            server = compressionCustomizer.apply(server);
        }
        server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
        return applyCustomizers(server);
    }

    @SuppressWarnings("deprecation")
    private HttpServer customizeSslConfiguration(final HttpServer httpServer) {
        SslServerCustomizer sslServerCustomizer = new SslServerCustomizer(getSsl(), getHttp2(),
                getOrCreateSslStoreProvider());
        return sslServerCustomizer.apply(httpServer);
    }

    private HttpProtocol[] listProtocols() {
        List<HttpProtocol> protocols = new ArrayList<>();
        protocols.add(HttpProtocol.HTTP11);
        if (getHttp2() != null && getHttp2().isEnabled()) {
            if (getSsl() != null && getSsl().isEnabled()) {
                protocols.add(HttpProtocol.H2);
            } else {
                protocols.add(HttpProtocol.H2C);
            }
        }
        return protocols.toArray(new HttpProtocol[0]);
    }

    private InetSocketAddress getListenAddress() {
        if (getAddress() != null) {
            return new InetSocketAddress(getAddress().getHostAddress(), getPort());
        }
        return new InetSocketAddress(getPort());
    }

    private HttpServer applyCustomizers(final HttpServer server) {
        HttpServer resultServer = server;
        for (NettyServerCustomizer customizer : this.serverCustomizers) {
            resultServer = customizer.apply(server);
        }
        return resultServer;
    }
}
