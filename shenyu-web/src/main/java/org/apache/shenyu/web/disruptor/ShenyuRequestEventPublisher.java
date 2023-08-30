package org.apache.shenyu.web.disruptor;

import org.apache.shenyu.disruptor.DisruptorProviderManage;
import org.apache.shenyu.disruptor.provider.DisruptorProvider;
import org.apache.shenyu.web.configuration.ShenyuDisruptorConfig;
import org.apache.shenyu.web.disruptor.consumer.ShenyuRequestConsumerExecutor.ShenyuRequestConsumerExecutorFactory;
import reactor.core.publisher.Mono;

public class ShenyuRequestEventPublisher {
    
    private static final ShenyuRequestEventPublisher INSTANCE = new ShenyuRequestEventPublisher();
    
    private DisruptorProviderManage<Mono> providerManage;
    
    /**
     * Get instance.
     *
     * @return ShenyuClientRegisterEventPublisher instance
     */
    public static ShenyuRequestEventPublisher getInstance() {
        return INSTANCE;
    }
    
    
    /**
     * Start shenyu request disruptor.
     *
     * @param shenyuDisruptorConfig config
     */
    public void start(final ShenyuDisruptorConfig shenyuDisruptorConfig) {
        ShenyuRequestConsumerExecutorFactory<Mono> factory = new ShenyuRequestConsumerExecutorFactory<>();
        providerManage = new DisruptorProviderManage<>(factory, shenyuDisruptorConfig.getThreadSize(), shenyuDisruptorConfig.getBufferSize());
        providerManage.startup();
    }
    
    /**
     * Publish event.
     *
     * @param shenyuServerExchange the data
     */
    public void publishEvent(final Mono shenyuServerExchange) {
        DisruptorProvider<Mono> provider = providerManage.getProvider();
        provider.onData(shenyuServerExchange);
    }
}
