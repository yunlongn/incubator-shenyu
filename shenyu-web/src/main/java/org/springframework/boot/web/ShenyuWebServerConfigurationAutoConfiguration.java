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
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyRouteProvider;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
//import org.springframework.boot.web.embedded.netty.NettyWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.netty.ShenyuNettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnNotWarDeployment
@ConditionalOnWebApplication
@EnableConfigurationProperties(ServerProperties.class)
@Import(ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class)
public class ShenyuWebServerConfigurationAutoConfiguration {


    /**
     * nettyReactiveWebServerFactory.
     *
     * @param routes routes
     * @param serverCustomizers serverCustomizers
     * @return {@link ShenyuNettyReactiveWebServerFactory}
     */
    @Bean
    ShenyuNettyReactiveWebServerFactory nettyReactiveWebServerFactory(final ObjectProvider<NettyRouteProvider> routes,
                                                                      final ObjectProvider<NettyServerCustomizer> serverCustomizers) {
        ShenyuNettyReactiveWebServerFactory serverFactory = new ShenyuNettyReactiveWebServerFactory();
        routes.orderedStream().forEach(serverFactory::addRouteProviders);
        serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().collect(Collectors.toList()));
        return serverFactory;
    }

    /**
     * Nested configuration if Netty is being used.
     */
    @Configuration(proxyBeanMethods = false)
    public static class ShenyuNettyWebServerFactoryCustomizerConfiguration {

        /**
         * shenyuNettyWebServerFactoryCustomizer.
         *
         * @param environment environment
         * @param serverProperties serverProperties
         * @return {@link NettyWebServerFactoryCustomizer}
         */
        @Bean
        NettyWebServerFactoryCustomizer shenyuNettyWebServerFactoryCustomizer(final Environment environment,
                                                                              final ServerProperties serverProperties) {
            return new NettyWebServerFactoryCustomizer(environment, serverProperties);
        }

    }
}
