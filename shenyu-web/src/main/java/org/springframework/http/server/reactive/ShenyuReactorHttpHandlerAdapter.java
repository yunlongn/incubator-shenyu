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
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URISyntaxException;
import java.util.function.BiFunction;

public class ShenyuReactorHttpHandlerAdapter extends ReactorHttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {
    
    private static final Log LOGGER = HttpLogging.forLogName(ReactorHttpHandlerAdapter.class);
    
    private final HttpHandler httpHandler;
    
    public ShenyuReactorHttpHandlerAdapter(final HttpHandler httpHandler) {
        super(httpHandler);
        Assert.notNull(httpHandler, "HttpHandler must not be null");
        this.httpHandler = httpHandler;
    }
    
    /**
     * apply.
     *
     * @param reactorRequest reactorRequest
     * @param reactorResponse reactorResponse
     * @return {@link Mono}
     */
    @Override
    public Mono<Void> apply(final HttpServerRequest reactorRequest, final HttpServerResponse reactorResponse) {
        NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(reactorResponse.alloc());
        try {
            ReactorServerHttpRequest request = new ReactorServerHttpRequest(reactorRequest, bufferFactory);
            ServerHttpResponse response = new ReactorServerHttpResponse(reactorResponse, bufferFactory);

            if (request.getMethod() == HttpMethod.HEAD) {
                response = new HttpHeadResponseDecorator(response);
            }
            return this.httpHandler.handle(request, response)
                    .doOnError(ex -> LOGGER.trace(request.getLogPrefix() + "Failed to complete: " + ex.getMessage()))
                    .doOnSuccess(aVoid -> LOGGER.trace(request.getLogPrefix() + "Handling completed"));
        } catch (URISyntaxException ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to get request URI: " + ex.getMessage());
            }
            reactorResponse.status(HttpResponseStatus.BAD_REQUEST);
            return Mono.empty();
        }
        //new Thread(() -> {
        //    try {
        //        Thread.sleep(5000L);
        //    } catch (InterruptedException e) {
        //        throw new RuntimeException(e);
        //    }
        //    reactorResponse.send(Mono.just(Unpooled.copiedBuffer("hello world".getBytes()))).then().subscribe();
        //}).start();
        //return Mono.empty();
    }

}
