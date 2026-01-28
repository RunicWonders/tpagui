package cn.ningmo.tpagui.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private final Path dataDirectory;
    private CommentedConfigurationNode config;

    @Inject
    public VelocityTpaGui(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        logger.info(getLogMessage("velocity-enabled"));
        logger.info(getLogMessage("velocity-environment"));
    }

    private void loadConfig() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Could not create data directory", e);
            }
        }

        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                }
            } catch (IOException e) {
                logger.error("Could not save default config", e);
            }
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();

        try {
            config = loader.load();
        } catch (ConfigurateException e) {
            logger.error("Could not load config", e);
        }
    }

    public String getLogMessage(String key, String... placeholders) {
        if (config == null) return key;
        String message = config.node("messages", "log", key).getString(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
