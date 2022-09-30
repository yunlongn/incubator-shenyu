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

package org.apache.shenyu.plugin.logging.common.body;

import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.common.constant.Constants;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.DateUtils;
import org.apache.shenyu.common.utils.JsonUtils;
import org.apache.shenyu.plugin.api.context.ShenyuContext;
import org.apache.shenyu.plugin.api.result.ShenyuResult;
import org.apache.shenyu.plugin.api.result.ShenyuResultWrap;
import org.apache.shenyu.plugin.logging.common.collector.LogCollector;
import org.apache.shenyu.plugin.logging.common.constant.GenericLoggingConstant;
import org.apache.shenyu.plugin.logging.common.datamask.DataMaskInterface;
import org.apache.shenyu.plugin.logging.common.datamask.KeyWordMatch;
import org.apache.shenyu.plugin.logging.common.entity.ShenyuRequestLog;
import org.apache.shenyu.plugin.logging.common.utils.LogCollectConfigUtils;
import org.apache.shenyu.plugin.logging.common.utils.LogCollectUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * decorate ServerHttpResponse for read body.
 */
public class LoggingServerHttpResponse extends ServerHttpResponseDecorator {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingServerHttpResponse.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final ShenyuRequestLog logInfo;

    private ServerWebExchange exchange;

    private final LogCollector logCollector;

    private final boolean maskFlag;

    private final DataMaskInterface dataMaskInterface;

    private final KeyWordMatch keyWordMatch;

    /**
     * Constructor LoggingServerHttpResponse.
     *
     * @param delegate          delegate ServerHttpResponse
     * @param logInfo           access log
     * @param logCollector      LogCollector  instance
     * @param maskFlag          mask flag
     * @param keyWordSet        user keyWord set
     * @param dataMaskInterface mask function
     */
    public LoggingServerHttpResponse(final ServerHttpResponse delegate, final ShenyuRequestLog logInfo,
                                     final LogCollector logCollector, final boolean maskFlag,
                                     final Set<String> keyWordSet, final DataMaskInterface dataMaskInterface) {

        super(delegate);
        this.logInfo = logInfo;
        this.logCollector = logCollector;
        this.maskFlag = maskFlag;
        this.dataMaskInterface = dataMaskInterface;
        this.keyWordMatch = new KeyWordMatch(keyWordSet);
    }

    /**
     * set relevant ServerWebExchange.
     *
     * @param exchange ServerWebExchange
     */
    public void setExchange(final ServerWebExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * write with a publisher.
     *
     * @param body response body
     * @return Mono
     */
    @Override
    @NonNull
    public Mono<Void> writeWith(@NonNull final Publisher<? extends DataBuffer> body) {
        return super.writeWith(appendResponse(body));
    }

    /**
     * append response.
     *
     * @param body publisher
     * @return wrap Flux
     */
    @NonNull
    private Flux<? extends DataBuffer> appendResponse(final Publisher<? extends DataBuffer> body) {
        ShenyuContext shenyuContext = exchange.getAttribute(Constants.CONTEXT);
        assert shenyuContext != null;
        if (getStatusCode() != null) {
            logInfo.setStatus(getStatusCode().value());
        }
        logInfo.setResponseHeader(LogCollectUtils.getHeaders(getHeaders()));
        BodyWriter writer = new BodyWriter();
        logInfo.setTraceId(getTraceId());
        return Flux.from(body).doOnNext(buffer -> {
            if (LogCollectUtils.isNotBinaryType(getHeaders())) {
                writer.write(buffer.asByteBuffer().asReadOnlyBuffer());
            }
        }).doFinally(signal -> logResponse(shenyuContext, writer));
    }

    /**
     * record response log.
     *
     * @param shenyuContext request context
     * @param writer        bodyWriter
     */
    private void logResponse(final ShenyuContext shenyuContext, final BodyWriter writer) {
        if (StringUtils.isNotBlank(getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH))) {
            String size = StringUtils.defaultIfEmpty(getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH), "0");
            logInfo.setResponseContentLength(Integer.parseInt(size));
        } else {
            logInfo.setResponseContentLength(writer.size());
        }
        logInfo.setTimeLocal(shenyuContext.getStartDateTime().format(DATE_TIME_FORMATTER));
        logInfo.setModule(shenyuContext.getModule());
        long costTime = DateUtils.acquireMillisBetween(shenyuContext.getStartDateTime(), LocalDateTime.now());
        logInfo.setUpstreamResponseTime(costTime);
        logInfo.setMethod(shenyuContext.getMethod());
        logInfo.setRpcType(shenyuContext.getRpcType());
        if (StringUtils.isNotBlank(shenyuContext.getRpcType())) {
            logInfo.setUpstreamIp(getUpstreamIp());
        }
        int size = writer.size();
        String body = writer.output();
        if (size > 0 && !LogCollectConfigUtils.isResponseBodyTooLarge(size)) {
            logInfo.setResponseBody(body);
        }
        if (maskFlag) {
            mask(logInfo);
        }
        // collect log
        if (Objects.nonNull(logCollector)) {
            logCollector.collect(logInfo);
        }
    }

    /**
     * get upstream ip.
     *
     * @return upstream ip
     */
    private String getUpstreamIp() {
        ShenyuContext shenyuContext = exchange.getAttribute(Constants.CONTEXT);
        assert shenyuContext != null;
        if (RpcTypeEnum.HTTP.getName().equals(shenyuContext.getRpcType())) {
            URI uri = exchange.getAttribute(Constants.HTTP_URI);
            if (Objects.nonNull(uri)) {
                return uri.getHost();
            } else {
                return getUpstreamIpFromHttpDomain();
            }
        } else {
            String domain = (String) exchange.getAttributes().get(Constants.HTTP_DOMAIN);
            if (StringUtils.isNotBlank(domain)) {
                return getUpstreamIpFromHttpDomain();
            }
            // The current context is difficult to obtain the upstream IP of grpc and Dubbo. need change plugin code.
        }
        return "";
    }

    /**
     * Encourage developers to provide plugins to upstream like SkyWalking, ZipKin and OpenTelemetry
     * to implement the tracing features. These plug-ins can set a traceId to the context.
     *
     * @return traceId
     */
    private String getTraceId() {
        return (String) exchange.getAttributes().get(GenericLoggingConstant.SHENYU_AGENT_TRACE_ID);
    }

    /**
     * collect access error.
     *
     * @param throwable Exception occurred。
     */
    public void logError(final Throwable throwable) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (throwable instanceof ResponseStatusException) {
            httpStatus = ((ResponseStatusException) throwable).getStatus();
        }
        logInfo.setStatus(httpStatus.value());
        logInfo.setTraceId(getTraceId());
        // Do not collect stack
        Object result = ShenyuResultWrap.error(exchange, httpStatus.value(),
                httpStatus.getReasonPhrase(), throwable.getMessage());
        final ShenyuResult<?> shenyuResult = ShenyuResultWrap.shenyuResult();
        Object resultData = shenyuResult.format(exchange, result);
        final Object responseData = shenyuResult.result(exchange, resultData);
        assert null != responseData;
        final byte[] bytes = (responseData instanceof byte[])
                ? (byte[]) responseData
                : responseData.toString().getBytes(StandardCharsets.UTF_8);
        logInfo.setResponseContentLength(bytes.length);
        ShenyuContext shenyuContext = exchange.getAttribute(Constants.CONTEXT);
        assert shenyuContext != null;
        logInfo.setTimeLocal(shenyuContext.getStartDateTime().format(DATE_TIME_FORMATTER));
        logInfo.setModule(shenyuContext.getModule());
        long costTime = DateUtils.acquireMillisBetween(shenyuContext.getStartDateTime(), LocalDateTime.now());
        logInfo.setUpstreamResponseTime(costTime);
        logInfo.setResponseHeader(LogCollectUtils.getHeaders(exchange.getResponse().getHeaders()));
        logInfo.setRpcType(shenyuContext.getRpcType());
        logInfo.setMethod(shenyuContext.getMethod());
        if (StringUtils.isNotBlank(shenyuContext.getRpcType())) {
            logInfo.setUpstreamIp(getUpstreamIp());
        }

        int size = bytes.length;
        String body = new String(bytes, StandardCharsets.UTF_8);
        if (size > 0 && !LogCollectConfigUtils.isResponseBodyTooLarge(size)) {
            logInfo.setResponseBody(body);
        }
        if (maskFlag) {
            mask(logInfo);
        }
        // collect log
        if (Objects.nonNull(logCollector)) {
            logCollector.collect(logInfo);
        }
    }

    private String getUpstreamIpFromHttpDomain() {
        String domain = (String) exchange.getAttributes().get(Constants.HTTP_DOMAIN);
        try {
            if (StringUtils.isNotBlank(domain)) {
                URL url = new URL(domain);
                return url.getHost();
            }
        } catch (Exception e) {
            LOG.error("get upstream ip error");
        }
        return "";
    }

    private void mask(final ShenyuRequestLog logInfo) {

        logInfo.setClientIp(maskForSingle(GenericLoggingConstant.CLIENT_IP, logInfo.getClientIp()));
        logInfo.setTimeLocal(maskForSingle(GenericLoggingConstant.TIME_LOCAL, logInfo.getTimeLocal()));
        logInfo.setMethod(maskForSingle(GenericLoggingConstant.METHOD, logInfo.getMethod()));
        logInfo.setRequestUri(maskForSingle(GenericLoggingConstant.REQUEST_URI, logInfo.getRequestUri()));
        logInfo.setResponseContentLength(Integer.valueOf(maskForSingle(GenericLoggingConstant.RESPONSE_CONTENT_LENGTH,
                logInfo.getResponseContentLength().toString())));
        logInfo.setRpcType(maskForSingle(GenericLoggingConstant.RPC_TYPE, logInfo.getRpcType()));
        logInfo.setStatus(Integer.valueOf(maskForSingle(GenericLoggingConstant.STATUS, logInfo.getStatus().toString())));
        logInfo.setUpstreamIp(maskForSingle(GenericLoggingConstant.UP_STREAM_IP, logInfo.getUpstreamIp()));
        logInfo.setUpstreamResponseTime(Long.valueOf(maskForSingle(GenericLoggingConstant.UP_STREAM_RESPONSE_TIME,
                logInfo.getUpstreamResponseTime().toString())));
        logInfo.setUserAgent(maskForSingle(GenericLoggingConstant.USERAGENT, logInfo.getUserAgent()));
        logInfo.setHost(maskForSingle(GenericLoggingConstant.HOST, logInfo.getHost()));
        logInfo.setModule(maskForSingle(GenericLoggingConstant.MODULE, logInfo.getModule()));
        logInfo.setTraceId(maskForSingle(GenericLoggingConstant.TRACE_ID, logInfo.getTraceId()));
        logInfo.setPath(maskForSingle(GenericLoggingConstant.PATH, logInfo.getPath()));
        logInfo.setRequestHeader(maskForSingle(GenericLoggingConstant.REQUEST_HEADER, logInfo.getRequestHeader()));
        logInfo.setResponseHeader(maskForSingle(GenericLoggingConstant.RESPONSE_HEADER,
                logInfo.getResponseHeader()));
        logInfo.setQueryParams(maskForSingle(GenericLoggingConstant.QUERY_PARAMS, logInfo.getQueryParams()));
        logInfo.setRequestBody(maskForSingle(GenericLoggingConstant.REQUEST_BODY, logInfo.getRequestBody()));
        logInfo.setResponseBody(maskForSingle(GenericLoggingConstant.RESPONSE_BODY, logInfo.getResponseBody()));
        logInfo.setRequestHeader(maskForBody(logInfo.getRequestHeader()));
        logInfo.setResponseHeader(maskForBody(logInfo.getResponseHeader()));
        logInfo.setQueryParams(maskForBody(logInfo.getQueryParams()));
        logInfo.setRequestBody(maskForBody(logInfo.getRequestBody()));
        logInfo.setResponseBody(maskForBody(logInfo.getResponseBody()));
    }

    private String maskForSingle(final String keyWord, final String val) {

        return StringUtils.isNotBlank(val) && keyWordMatch.matches(keyWord) ? dataMaskInterface.mask(val) : val;
    }

    private String maskForBody(final String body) {

        if (StringUtils.isNotBlank(body)) {
            Map<String, String> bodyMap = JsonUtils.jsonToMap(body, String.class);
            bodyMap.forEach((key, value) -> {
                if (keyWordMatch.matches(key)) {
                    bodyMap.put(key, dataMaskInterface.mask(value));
                }
            });
            return bodyMap.toString();
        } else {
            return body;
        }
    }
}
