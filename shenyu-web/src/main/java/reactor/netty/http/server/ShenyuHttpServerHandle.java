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

package reactor.netty.http.server;

import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import static reactor.netty.ReactorNetty.format;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.context.Context;

import java.util.function.BiFunction;

public final class ShenyuHttpServerHandle implements ConnectionObserver {
    private static final Logger log = Loggers.getLogger(ShenyuHttpServerHandle.class);

    private final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler;

    public ShenyuHttpServerHandle(final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
        this.handler = handler;
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void onStateChange(final Connection connection, final State newState) {
        //if (newState == HttpServerState.REQUEST_RECEIVED) {
        //    try {
        //        if (log.isDebugEnabled()) {
        //            log.debug(format(connection.channel(), "Handler is being applied: {}"), handler);
        //        }
        //        HttpServerOperations ops = (HttpServerOperations) connection;
        //        handler.apply(ops, ops);
        //    } catch (Throwable t) {
        //        log.error(format(connection.channel(), ""), t);
        //        //"FutureReturnValueIgnored" this is deliberate
        //        connection.channel()
        //                .close();
        //    }
        //}
        
        
        if (newState == HttpServerState.REQUEST_RECEIVED) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug(format(connection.channel(), "Handler is being applied: {}"), handler);
                }
                HttpServerOperations ops = (HttpServerOperations) connection;
                Publisher<Void> publisher = handler.apply(ops, ops);
                Mono<Void> mono = Mono.deferContextual(ctx -> {
                    ops.currentContext = Context.of(ctx);
                    return Mono.fromDirect(publisher);
                });
                if (ops.mapHandle != null) {
                    mono = ops.mapHandle.apply(mono, connection);
                }
                //mono.subscribe(ops.disposeSubscriber());
            } catch (Throwable t) {
                log.error(format(connection.channel(), ""), t);
                //"FutureReturnValueIgnored" this is deliberate
                connection.channel()
                        .close();
            }
        }
    }
}
