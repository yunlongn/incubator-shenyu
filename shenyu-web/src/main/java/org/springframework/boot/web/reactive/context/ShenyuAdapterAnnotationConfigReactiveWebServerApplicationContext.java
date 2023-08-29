package org.springframework.boot.web.reactive.context;

import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.boot.web.embedded.netty.ShenyuNettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.StartupStep;

public class ShenyuAdapterAnnotationConfigReactiveWebServerApplicationContext extends AnnotationConfigReactiveWebServerApplicationContext {
    
    private WebServer webServer;
    
    @Override
    protected void onRefresh() {
        try {
            createWebServer();
        } catch (Throwable ex) {
            throw new ApplicationContextException("Unable to start reactive web server", ex);
        }
    }
    
    private void createWebServer() {
        if (webServer == null) {
            StartupStep createWebServer = this.getApplicationStartup().start("spring.boot.webserver.create");
            String webServerFactoryBeanName = getWebServerFactoryBeanName();
            
            final ShenyuNettyReactiveWebServerFactory nettyReactiveWebServerFactory = getBeanFactory().getBean(ShenyuNettyReactiveWebServerFactory.class);
            createWebServer.tag("factory", nettyReactiveWebServerFactory.getClass().toString());
            
            //boolean lazyInit = getBeanFactory().getBeanDefinition(webServerFactoryBeanName).isLazyInit();
            //this.serverManager = new ShenyuWebServerManager(this, nettyReactiveWebServerFactory, this::getHttpHandler, lazyInit);
            webServer = nettyReactiveWebServerFactory.getWebServer(super.getHttpHandler());
            getBeanFactory().registerSingleton("webServerGracefulShutdown",
                    new WebServerGracefulShutdownLifecycle(webServer));
            getBeanFactory().registerSingleton("webServerStartStop",
                    new ShenyuWebServerStartStopLifecycle(webServer));
            createWebServer.end();
        }
        initPropertySources();
    }
    
    @Override
    protected String getWebServerFactoryBeanName() {
        return super.getWebServerFactoryBeanName();
    }
    
    @Override
    protected ReactiveWebServerFactory getWebServerFactory(final String factoryBeanName) {
        return super.getWebServerFactory(factoryBeanName);
    }
    
    @Override
    protected void doClose() {
        super.doClose();
    }
    
    public static class Factory implements ApplicationContextFactory {
        
        @Override
        public Class<? extends ConfigurableEnvironment> getEnvironmentType(final WebApplicationType webApplicationType) {
            return (webApplicationType != WebApplicationType.REACTIVE) ? null : ApplicationReactiveWebEnvironment.class;
        }
        
        @Override
        public ConfigurableEnvironment createEnvironment(final WebApplicationType webApplicationType) {
            return (webApplicationType != WebApplicationType.REACTIVE) ? null : new ApplicationReactiveWebEnvironment();
        }
        
        @Override
        public ConfigurableApplicationContext create(final WebApplicationType webApplicationType) {
            return (webApplicationType != WebApplicationType.REACTIVE) ? null
                    : new ShenyuAdapterAnnotationConfigReactiveWebServerApplicationContext();
        }
        
    }
}
