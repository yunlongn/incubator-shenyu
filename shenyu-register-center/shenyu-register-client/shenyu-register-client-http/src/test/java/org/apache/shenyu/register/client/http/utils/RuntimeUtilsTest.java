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

package org.apache.shenyu.register.client.http.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link RuntimeUtils}.
 */
class RuntimeUtilsTest {

    private WireMockServer wireMockServer;

    @Test
    void listenByOther() throws IOException {
        wireMockServer = new WireMockServer(options().extensions(new ResponseTemplateTransformer(false)).dynamicPort());
        wireMockServer.start();
        try (MockedStatic<SystemUtils> systemUtilsMockedStatic = mockStatic(SystemUtils.class)) {
            systemUtilsMockedStatic.when(SystemUtils::getCurrentPID).thenReturn("0");
            systemUtilsMockedStatic.when(SystemUtils::isWindows).thenReturn(true);
            assertTrue(RuntimeUtils.listenByOther(wireMockServer.port()));

            systemUtilsMockedStatic.when(SystemUtils::isWindows).thenReturn(false);
            assertFalse(RuntimeUtils.listenByOther(wireMockServer.port()));
        }
        try (MockedStatic<Runtime> runtimeMockedStatic = mockStatic(Runtime.class)) {
            runtimeMockedStatic.when(Runtime::getRuntime).thenThrow(RuntimeException.class);
            assertFalse(RuntimeUtils.listenByOther(wireMockServer.port()));
            final Runtime runtime = mock(Runtime.class);
            runtimeMockedStatic.when(Runtime::getRuntime).thenReturn(runtime);
            final Process process = mock(Process.class);
            when(runtime.exec(any(String[].class))).thenReturn(process);
            final String text = "LISTEN xx:" + wireMockServer.port() + " xx ";
            InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            when(process.getInputStream()).thenReturn(stream);
            assertFalse(RuntimeUtils.listenByOther(wireMockServer.port()));
            when(runtime.exec(any(String[].class))).thenReturn(null);
            assertFalse(RuntimeUtils.listenByOther(wireMockServer.port()));
        }
        assertFalse(RuntimeUtils.listenByOther(wireMockServer.port()));
        assertFalse(RuntimeUtils.listenByOther(99999));
        assertFalse(RuntimeUtils.listenByOther(0));
    }
}
