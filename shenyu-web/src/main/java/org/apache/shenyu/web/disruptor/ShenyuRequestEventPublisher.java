package org.apache.shenyu.web.disruptor;

import org.apache.shenyu.disruptor.DisruptorProviderManage;
import org.apache.shenyu.disruptor.provider.DisruptorProvider;
import org.apache.shenyu.web.configuration.ShenyuDisruptorConfig;
import org.apache.shenyu.web.server.ShenyuServerExchange;
import org.springframework.http.server.reactive.ShenyuRequestConsumerExecutor.ShenyuRequestConsumerExecutorFactory;

public class ShenyuRequestEventPublisher {
    
    private static final ShenyuRequestEventPublisher INSTANCE = new ShenyuRequestEventPublisher();
    
    private DisruptorProviderManage<ShenyuServerExchange> providerManage;
    
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
        ShenyuRequestConsumerExecutorFactory<ShenyuServerExchange> factory = new ShenyuRequestConsumerExecutorFactory<>();
        providerManage = new DisruptorProviderManage<>(factory, shenyuDisruptorConfig.getThreadSize(), shenyuDisruptorConfig.getBufferSize());
        providerManage.startup();
    }
    
    /**
     * Publish event.
     *
     * @param shenyuServerExchange the data
     */
    public void publishEvent(final ShenyuServerExchange shenyuServerExchange) {
        DisruptorProvider<ShenyuServerExchange> provider = providerManage.getProvider();
        provider.onData(shenyuServerExchange);
    }
}
