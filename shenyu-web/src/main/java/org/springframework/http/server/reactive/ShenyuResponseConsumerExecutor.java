package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.apache.shenyu.disruptor.consumer.QueueConsumerExecutor;
import org.apache.shenyu.disruptor.consumer.QueueConsumerFactory;
import org.springframework.http.HttpLogging;
import reactor.core.publisher.Mono;

public class ShenyuResponseConsumerExecutor<T extends Mono> extends QueueConsumerExecutor<T> {
    
    private static final Log LOGGER = HttpLogging.forLogName(ShenyuResponseConsumerExecutor.class);
    
    @Override
    public void run() {
        getData().subscribe();
    }
    
    public static class ShenyuResponseConsumerExecutorFactory<T extends Mono> implements QueueConsumerFactory<T> {
        @Override
        public ShenyuResponseConsumerExecutor<T> create() {
            
            return new ShenyuResponseConsumerExecutor<>();
        }
    
        @Override
        public String fixName() {
            return "shenyu_response";
        }
    }
}
