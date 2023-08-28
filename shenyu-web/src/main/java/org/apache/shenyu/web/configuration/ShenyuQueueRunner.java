package org.apache.shenyu.web.configuration;

import org.apache.shenyu.web.disruptor.ShenyuRequestEventPublisher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ShenyuQueueRunner implements ApplicationRunner {
    @Override
    public void run(final ApplicationArguments args) throws Exception {
        System.out.println("start ShenyuQueueRunner");
        ShenyuRequestEventPublisher.getInstance().start(new ShenyuDisruptorConfig());
    }
}
