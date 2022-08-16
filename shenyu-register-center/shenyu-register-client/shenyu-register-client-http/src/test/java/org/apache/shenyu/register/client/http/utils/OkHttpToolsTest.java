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
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wiremock.com.google.common.net.HttpHeaders;
import wiremock.org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Test case for {@link OkHttpTools}.
 */
public final class OkHttpToolsTest {

    private WireMockServer wireMockServer;

    private String url;

    private String getUrl;

    private String postUrl;

    private final String json = "{\"appName\":\"shenyu\"}";

    private final Map<String, Object> request = new HashMap<>();

    @BeforeEach
    public void setUpWireMock() {
        this.wireMockServer = new WireMockServer(
                options()
                        .extensions(new ResponseTemplateTransformer(false))
                        .dynamicPort());
        this.wireMockServer.start();
        wireMockServer.stubFor(post(urlPathEqualTo("/post"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(json)
                        .withStatus(200))
        );
        wireMockServer.stubFor(get(urlPathEqualTo("/get"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(json)
                        .withStatus(200))
        );
        url = "http://localhost:" + wireMockServer.port();
        getUrl = url + "/get";
        postUrl = url + "/post";
    }

    @Test
    public void testPostReturnString() throws IOException {
        assertThat(json, is(OkHttpTools.getInstance().post(postUrl, json)));
        Headers headers = Headers.of().newBuilder().build();
        assertThat(json, is(OkHttpTools.getInstance().post(postUrl, json, headers)));
    }

    @Test
    public void testGetReturnString() throws IOException {
        request.put("get", "request");
        assertThat(json, is(OkHttpTools.getInstance().get(getUrl, request)));
        assertThat(json, is(OkHttpTools.getInstance().get(getUrl, "userName", "passWord")));
    }
}
