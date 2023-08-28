package org.springframework.boot.web.reactive.context;

import org.springframework.boot.web.embedded.netty.ShenyuNettyReactiveWebServerFactory;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;

import java.util.function.Supplier;

public class ShenyuWebServerManager extends WebServerManager {
    
    private final WebServer webServer;
    ShenyuWebServerManager(final ReactiveWebServerApplicationContext applicationContext, final ShenyuNettyReactiveWebServerFactory factory,
                           final Supplier<HttpHandler> handlerSupplier, final boolean lazyInit) {
        super(applicationContext, factory, handlerSupplier, lazyInit);
        this.webServer = factory.getWebServer(this.getHandler());
    }
    
    @Override
    void start() {
        //super.start();
        this.webServer.start();
    }
    
    @Override
    void shutDownGracefully(final GracefulShutdownCallback callback) {
        this.webServer.shutDownGracefully(callback);
    }
    
    @Override
    void stop() {
        this.webServer.stop();
    }
}
