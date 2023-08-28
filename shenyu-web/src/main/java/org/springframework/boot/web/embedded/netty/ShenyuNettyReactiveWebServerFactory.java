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

import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ShenyuReactorHttpHandlerAdapter;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ShenyuNettyReactiveWebServerFactory} that can be used to create {@link ShenyuNettyWebServer}.
 *
 * @since 2.0.0
 */
public class ShenyuNettyReactiveWebServerFactory extends NettyReactiveWebServerFactory {

    private Set<NettyServerCustomizer> serverCustomizers = new LinkedHashSet<>();

    private List<NettyRouteProvider> routeProviders = new ArrayList<>();

    private Duration lifecycleTimeout;

    private boolean useForwardHeaders;

    private Shutdown shutdown;

    public ShenyuNettyReactiveWebServerFactory() {
        serverCustomizers = (Set<NettyServerCustomizer>) getServerCustomizers();
    }

    public ShenyuNettyReactiveWebServerFactory(final int port) {
        super(port);
    }

    /**
     * getWebServer.
     *
     * @param httpHandler httpHandler
     *
     * @return {@link WebServer}
     */
    public WebServer getWebServer(final HttpHandler httpHandler) {
        HttpServer httpServer = createHttpServer();
        ShenyuReactorHttpHandlerAdapter handlerAdapter = new ShenyuReactorHttpHandlerAdapter(httpHandler);
        ShenyuNettyWebServer webServer = createShenyuNettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout,
                getShutdown());
        webServer.setRouteProviders(this.routeProviders);
        return webServer;
    }

    ShenyuNettyWebServer createShenyuNettyWebServer(final HttpServer httpServer, final ShenyuReactorHttpHandlerAdapter handlerAdapter,
                                                    final Duration lifecycleTimeout, final Shutdown shutdown) {
        return new ShenyuNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown);
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

    /**
     * Set if x-forward-* headers should be processed.
     * @param useForwardHeaders if x-forward headers should be used
     * @since 2.1.0
     */
    public void setUseForwardHeaders(final boolean useForwardHeaders) {
        this.useForwardHeaders = useForwardHeaders;
    }
}
