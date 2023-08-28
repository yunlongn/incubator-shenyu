package org.apache.shenyu.web.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@ConfigurationProperties(prefix = "shenyu.disruptor")
public class ShenyuDisruptorConfig {
    
    private Integer bufferSize = 1024;
    
    private Integer threadSize = 20;
    
    public Integer getBufferSize() {
        return bufferSize;
    }
    
    public void setBufferSize(final Integer bufferSize) {
        this.bufferSize = bufferSize;
    }
    
    public Integer getThreadSize() {
        return threadSize;
    }
    
    public void setThreadSize(final Integer threadSize) {
        this.threadSize = threadSize;
    }
}
