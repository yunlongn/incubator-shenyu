package org.springframework.web.server.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shenyu.common.constant.Constants;
import org.apache.shenyu.web.disruptor.ShenyuResponseEventPublisher;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ShenyuHttpWebHandlerAdapter extends HttpWebHandlerAdapter {

    /**
     * Dedicated log category for disconnected client exceptions.
     * <p>Servlet containers don't expose a client disconnected callback; see
     * <a href="https://github.com/eclipse-ee4j/servlet-api/issues/44">eclipse-ee4j/servlet-api#44</a>.
     * <p>To avoid filling logs with unnecessary stack traces, we make an
     * effort to identify such network failures on a per-server basis, and then
     * log under a separate log category a simple one-line message at DEBUG level
     * or a full stack trace only at TRACE level.
     */
    private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
            "org.springframework.web.server.DisconnectedClient";

    // Similar declaration exists in AbstractSockJsSession..
    private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = new HashSet<>(
            Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));

    private static final Log LOGGER = LogFactory.getLog(ShenyuHttpWebHandlerAdapter.class);

    private static final Log LOST_CLIENT_LOGGER = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

    private final ShenyuResponseEventPublisher shenyuResponseEventPublisher = ShenyuResponseEventPublisher.getInstance();

    public ShenyuHttpWebHandlerAdapter(final WebHandler delegate) {
        super(delegate);
    }

    @Override
    public Mono<Void> handle(final ServerHttpRequest request, final ServerHttpResponse response) {
        ServerWebExchange exchange = createExchange(request, response);

        LogFormatUtils.traceDebug(LOGGER, traceOn ->
                exchange.getLogPrefix() + formatRequest(exchange.getRequest())
                        + (traceOn ? ", headers=" + exchange.getResponse().getHeaders() : ""));

        return getDelegate().handle(exchange)
                .doOnSuccess(aVoid -> sendResponse(exchange))
                .onErrorResume(ex -> handleUnresolvedError(exchange, ex));
    }

    private void sendResponse(final ServerWebExchange exchange) {
        shenyuResponseEventPublisher.publishEvent(exchange.getAttribute(Constants.RESPONSE_WRITE_WITH));
        LogFormatUtils.traceDebug(LOGGER, traceOn -> {
            HttpStatus status = exchange.getResponse().getStatusCode();
            return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK")
                    + (traceOn ? ", headers=" + exchange.getResponse().getHeaders() : "");
        });
    }

    private Mono<Void> handleUnresolvedError(final ServerWebExchange exchange, final Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String logPrefix = exchange.getLogPrefix();

        // Sometimes a remote call error can look like a disconnected client.
        // Try to set the response first before the "isDisconnectedClient" check.

        if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
            LOGGER.error(logPrefix + "500 Server Error for " + formatRequest(request), ex);
            return Mono.empty();
        } else if (isDisconnectedClientError(ex)) {
            if (LOST_CLIENT_LOGGER.isTraceEnabled()) {
                LOST_CLIENT_LOGGER.trace(logPrefix + "Client went away", ex);
            } else if (LOST_CLIENT_LOGGER.isDebugEnabled()) {
                LOST_CLIENT_LOGGER.debug(logPrefix + "Client went away: " + ex
                        + " (stacktrace at TRACE level for '" + DISCONNECTED_CLIENT_LOG_CATEGORY + "')");
            }
            return Mono.empty();
        } else {
            // After the response is committed, propagate errors to the server...
            LOGGER.error(logPrefix + "Error [" + ex + "] for " + formatRequest(request)
                    + ", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
            return Mono.error(ex);
        }
    }

    private boolean isDisconnectedClientError(final Throwable ex) {
        String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        if (message != null) {
            String text = message.toLowerCase();
            if (text.contains("broken pipe") || text.contains("connection reset by peer")) {
                return true;
            }
        }
        return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName());
    }
}
