package org.apache.shenyu.web.server;

import org.apache.shenyu.plugin.api.ShenyuPlugin;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

public class ShenyuRequestExchange {
    
    private ServerWebExchange exchange;
    
    private List<ShenyuPlugin> plugins;

    public ShenyuRequestExchange(final ServerWebExchange exchange, final List<ShenyuPlugin> plugins) {
        this.exchange = exchange;
        this.plugins = plugins;
    }

    /**
     * exchange.
     *
     * @return Exchange
     */
    public ServerWebExchange getExchange() {
        return exchange;
    }

    /**
     * plugins.
     *
     * @return Plugins
     */
    public List<ShenyuPlugin> getPlugins() {
        return plugins;
    }
}
