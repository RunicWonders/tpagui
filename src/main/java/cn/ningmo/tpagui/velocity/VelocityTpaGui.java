package cn.ningmo.tpagui.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
    id = "tpagui",
    name = "TpaGui",
    version = "1.2.0-beta.1",
    description = "A simple TPA GUI plugin for Velocity",
    authors = {"NingMo"}
)
public class VelocityTpaGui {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public VelocityTpaGui(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("TpaGui Velocity version is enabling...");
        logger.info("Velocity environment detected. All connected players will be managed by this proxy instance.");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
