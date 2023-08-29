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

package org.apache.shenyu.common.config;

/**
 * Shenyu work thread pool config.
 */
public class ShenyuWorkThreadPoolConfig {
    
    /**
     * Whether to enabled, shenyu work thread pool, default is true.
     * The original thread mode is the reactor-netty thread is blocked by the shenyu plugin execution.
     * <pre>
     *    Socket -> AcceptThread(Reactor-Netty Thread)
     * </pre>
     * The new thread mode is reactor-netty thread just handle request and return <code>Mono.empty()</code>,
     * Shenyu work thread pool is execute plugin logic.
     * <pre>
     *     Socket -> AcceptThread(Reactor-Netty Thread) -> DisruptorQueue -> ShenyuWorkThreadPool
     * </pre>
     */
    private Boolean enabled = Boolean.TRUE;
    
    private Integer coreThreadSize = Runtime.getRuntime().availableProcessors() << 1;
    
    private Integer maxThreadSize = Runtime.getRuntime().availableProcessors() << 1;
    
    private Integer queueSize = 1000;
    
    private Integer keepAliveTime = 60;
    
    private String threadNamePrefix = "shenyu-work-thread";
    
    /**
     * Get sheneyu work thread pool enabled.
     *
     * @return sheneyu work thread pool enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }
    
    /**
     * Set sheneyu work thread pool enabled.
     *
     * @param enabled enabled
     */
    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get core thread size.
     *
     * @return core thread size
     */
    public Integer getCoreThreadSize() {
        return coreThreadSize;
    }
    
    /**
     * Set core thread size.
     *
     * @param coreThreadSize core thread size
     */
    public void setCoreThreadSize(final Integer coreThreadSize) {
        this.coreThreadSize = coreThreadSize;
    }
    
    /**
     * Get max thread size.
     *
     * @return max thread size
     */
    public Integer getMaxThreadSize() {
        return maxThreadSize;
    }
    
    /**
     * Set max thread size.
     *
     * @param maxThreadSize max thread size
     */
    public void setMaxThreadSize(final Integer maxThreadSize) {
        this.maxThreadSize = maxThreadSize;
    }
    
    /**
     * Get queue size.
     *
     * @return queue size
     */
    public Integer getQueueSize() {
        return queueSize;
    }
    
    /**
     * Set queue size.
     *
     * @param queueSize the blocking queue size
     */
    public void setQueueSize(final Integer queueSize) {
        this.queueSize = queueSize;
    }
    
    /**
     * get core thread keep alive time.
     *
     * @return keep alive time
     */
    public Integer getKeepAliveTime() {
        return keepAliveTime;
    }
    
    /**
     * set core thread keep alive time.
     *
     * @param keepAliveTime keep alive time
     */
    public void setKeepAliveTime(final Integer keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }
    
}
