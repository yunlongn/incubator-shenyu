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

package org.springframework.boot.web.reactive.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.boot.web.embedded.netty.ShenyuNettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.metrics.StartupStep;

public class ShenyuReactiveWebServerApplicationContext extends GenericReactiveWebApplicationContext
        implements ConfigurableWebServerApplicationContext {

    private WebServer webServer;

    private String serverNamespace;

    /**
     * Create a new {@link ReactiveWebServerApplicationContext}.
     */
    public ShenyuReactiveWebServerApplicationContext() {
    }

    /**
     * Create a new {@link ReactiveWebServerApplicationContext} with the given
     * {@code DefaultListableBeanFactory}.
     * @param beanFactory the DefaultListableBeanFactory instance to use for this context
     */
    public ShenyuReactiveWebServerApplicationContext(final DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    @Override
    public final void refresh() throws BeansException, IllegalStateException {
        try {
            super.refresh();
        } catch (RuntimeException ex) {
            // delete
            throw ex;
        }
    }

    @Override
    protected void onRefresh() {
        super.onRefresh();
        try {
            createWebServer();
        } catch (Throwable ex) {
            throw new ApplicationContextException("Unable to start reactive web server", ex);
        }
    }

    private void createWebServer() {
        if (webServer == null) {
            StartupStep createWebServer = this.getApplicationStartup().start("spring.boot.webserver.create");
            final ShenyuNettyReactiveWebServerFactory nettyReactiveWebServerFactory = getBeanFactory().getBean(ShenyuNettyReactiveWebServerFactory.class);
            createWebServer.tag("factory", nettyReactiveWebServerFactory.getClass().toString());
            webServer = nettyReactiveWebServerFactory.getWebServer();
            getBeanFactory().registerSingleton("webServerGracefulShutdown",
                    new WebServerGracefulShutdownLifecycle(webServer));
            getBeanFactory().registerSingleton("webServerStartStop",
                    new ShenyuWebServerStartStopLifecycle(webServer));
            createWebServer.end();
        }
        initPropertySources();
    }

    @Override
    protected void doClose() {
        if (isActive()) {
            AvailabilityChangeEvent.publish(this, ReadinessState.REFUSING_TRAFFIC);
        }
        super.doClose();
    }

    /**
     * Returns the {@link WebServer} that was created by the context or {@code null} if
     * the server has not yet been created.
     * @return the web server
     */
    @Override
    public WebServer getWebServer() {
        return webServer;
    }

    @Override
    public String getServerNamespace() {
        return this.serverNamespace;
    }

    @Override
    public void setServerNamespace(final String serverNamespace) {
        this.serverNamespace = serverNamespace;
    }

}
