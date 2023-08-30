package org.apache.shenyu.web.disruptor;

import org.apache.shenyu.disruptor.DisruptorProviderManage;
import org.apache.shenyu.disruptor.provider.DisruptorProvider;
import org.apache.shenyu.web.configuration.ShenyuDisruptorConfig;
import org.apache.shenyu.web.disruptor.consumer.ShenyuResponseConsumerExecutor;
import reactor.core.publisher.Mono;

public class ShenyuResponseEventPublisher {
    
    private static final ShenyuResponseEventPublisher INSTANCE = new ShenyuResponseEventPublisher();
    
    private DisruptorProviderManage<Mono> providerManage;
    
    /**
     * Get instance.
     *
     * @return ShenyuClientRegisterEventPublisher instance
     */
    public static ShenyuResponseEventPublisher getInstance() {
        return INSTANCE;
    }
    
    
    /**
     * Start shenyu request disruptor.
     *
     * @param shenyuDisruptorConfig config
     */
    public void start(final ShenyuDisruptorConfig shenyuDisruptorConfig) {
        ShenyuResponseConsumerExecutor.ShenyuResponseConsumerExecutorFactory factory = new ShenyuResponseConsumerExecutor.ShenyuResponseConsumerExecutorFactory();
        providerManage = new DisruptorProviderManage<>(factory, shenyuDisruptorConfig.getThreadSize(), shenyuDisruptorConfig.getBufferSize());
        providerManage.startup();
    }
    
    /**
     * Publish event.
     *
     * @param responseMono the data
     */
    public void publishEvent(final Mono responseMono) {
        DisruptorProvider<Mono> provider = providerManage.getProvider();
        provider.onData(responseMono);
    }
}
