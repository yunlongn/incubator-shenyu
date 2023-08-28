package org.apache.shenyu.web.server;

import org.springframework.http.server.reactive.HttpHandler;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ShenyuServerExchange {
    
    private HttpServerRequest reactorRequest;
    
    private HttpServerResponse reactorResponse;
    
    private HttpHandler httpHandler;
    
    public ShenyuServerExchange() {
    }
    
    public ShenyuServerExchange(HttpServerRequest reactorRequest, HttpServerResponse reactorResponse, HttpHandler httpHandler) {
        this.reactorRequest = reactorRequest;
        this.reactorResponse = reactorResponse;
        this.httpHandler = httpHandler;
    }
    
    public HttpServerRequest getReactorRequest() {
        return reactorRequest;
    }
    
    public void setReactorRequest(final HttpServerRequest reactorRequest) {
        this.reactorRequest = reactorRequest;
    }
    
    public HttpServerResponse getReactorResponse() {
        return reactorResponse;
    }
    
    public void setReactorResponse(final HttpServerResponse reactorResponse) {
        this.reactorResponse = reactorResponse;
    }
    
    public HttpHandler getHttpHandler() {
        return httpHandler;
    }
    
    public void setHttpHandler(final HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }
}
