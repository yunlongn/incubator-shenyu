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

package org.springframework.boot.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.embedded.netty.ShenyuNettyReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.ShenyuHttpWebHandlerAdapter;
import org.springframework.web.server.handler.FilteringWebHandler;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.server.adapter.WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME;

@AutoConfiguration
@ConditionalOnNotWarDeployment
@ConditionalOnWebApplication
@EnableConfigurationProperties(ServerProperties.class)
@Import(ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class)
public class ShenyuWebServerConfigurationAutoConfiguration {


    /**
     * nettyReactiveWebServerFactory.
     *
     * @param serverCustomizers serverCustomizers
     * @return {@link ShenyuNettyReactiveWebServerFactory}
     */
    @Bean
    ShenyuNettyReactiveWebServerFactory nettyReactiveWebServerFactory(final ObjectProvider<NettyServerCustomizer> serverCustomizers) {
        ShenyuNettyReactiveWebServerFactory serverFactory = new ShenyuNettyReactiveWebServerFactory();
        serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().collect(Collectors.toList()));
        return serverFactory;
    }

    @Bean
    HttpHandler httpHandler(final ApplicationContext applicationContext) {
        final WebHandler webHandler = applicationContext.getBean(WEB_HANDLER_BEAN_NAME, WebHandler.class);
        List<WebFilter> webFilters = applicationContext.getBeanProvider(WebFilter.class)
                .orderedStream().collect(Collectors.toList());
        WebHandler decorated = new FilteringWebHandler(webHandler, webFilters);
        return new ShenyuHttpWebHandlerAdapter(decorated);
    }
}
