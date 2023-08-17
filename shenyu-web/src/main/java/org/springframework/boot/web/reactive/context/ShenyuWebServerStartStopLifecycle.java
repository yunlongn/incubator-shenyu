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

package org.springframework.boot.web.reactive.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.SmartLifecycle;

public class ShenyuWebServerStartStopLifecycle implements SmartLifecycle {

    private volatile boolean running;

    private final WebServer webServer;

    public ShenyuWebServerStartStopLifecycle(final WebServer webServer) {
        this.webServer = webServer;
    }

    @Override
    public void start() {
        this.webServer.start();
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        this.webServer.stop();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }

}
