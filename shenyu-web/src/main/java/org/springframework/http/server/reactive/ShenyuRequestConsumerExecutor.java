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

package org.springframework.http.server.reactive;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import org.apache.shenyu.disruptor.consumer.QueueConsumerExecutor;
import org.apache.shenyu.disruptor.consumer.QueueConsumerFactory;
import org.apache.shenyu.web.server.ShenyuServerExchange;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URISyntaxException;

public class ShenyuRequestConsumerExecutor<T extends ShenyuServerExchange> extends QueueConsumerExecutor<T> {
    
    private static final Log LOGGER = HttpLogging.forLogName(ShenyuRequestConsumerExecutor.class);
    
    @Override
    public void run() {
        T data = getData();
        ShenyuServerExchange exchange = data;
        LOGGER.info("get request...");
        new Thread(() -> {
            LOGGER.info("handle request...");
            HttpServerRequest reactorRequest = exchange.getReactorRequest();
            HttpServerResponse reactorResponse = exchange.getReactorResponse();
            HttpHandler httpHandler = exchange.getHttpHandler();
            
            NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(reactorResponse.alloc());
            try {
                ReactorServerHttpRequest request = new ReactorServerHttpRequest(reactorRequest, bufferFactory);
                ServerHttpResponse response = new ReactorServerHttpResponse(reactorResponse, bufferFactory);
                
                if (request.getMethod() == HttpMethod.HEAD) {
                    response = new HttpHeadResponseDecorator(response);
                }
                httpHandler.handle(request, response)
                        .doOnError(ex -> LOGGER.trace(request.getLogPrefix() + "Failed to complete: " + ex.getMessage()))
                        .doOnSuccess(aVoid -> LOGGER.trace(request.getLogPrefix() + "Handling completed"))
                        .subscribe();
            } catch (URISyntaxException ex) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Failed to get request URI: " + ex.getMessage());
                }
                reactorResponse.status(HttpResponseStatus.BAD_REQUEST);
            }
        }).start();
    }
    
    public static class ShenyuRequestConsumerExecutorFactory<T extends ShenyuServerExchange> implements QueueConsumerFactory<T> {
        @Override
        public ShenyuRequestConsumerExecutor<T> create() {
            
            return new ShenyuRequestConsumerExecutor<>();
        }
    
        @Override
        public String fixName() {
            return "shenyu_request";
        }
    }
}
