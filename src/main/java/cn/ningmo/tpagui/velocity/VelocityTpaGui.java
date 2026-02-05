package cn.ningmo.tpagui.velocity;

import cn.ningmo.tpagui.UpdateChecker;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
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
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "tpagui",
    name = "TpaGui",
    version = "1.2.0-beta.4",
    description = "A simple TPA GUI plugin for Velocity",
    authors = {"lemwood"}
)
public class VelocityTpaGui {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private CommentedConfigurationNode config;
    private UpdateChecker updateChecker;

    @Inject
    public VelocityTpaGui(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public ProxyServer getServer() {
        return server;
    }

    public CommentedConfigurationNode getConfig() {
        return config;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        logger.info(getLogMessage("velocity-enabled"));
        logger.info(getLogMessage("velocity-environment"));
        
        // 注册命令
        CommandManager commandManager = server.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("tpagui")
                .aliases("tpag", "tgui")
                .plugin(this)
                .build();
        commandManager.register(meta, new VelocityTpaGuiCommand(this, server));
        
        // 初始化更新检查器
        updateChecker = new UpdateChecker(
            "1.2.0-beta.4",
            msg -> logger.warn(msg),
            key -> getLogMessage(key),
            () -> config.node("update-check", "enabled").getBoolean(true)
        );
        
        // 检查更新
        server.getScheduler().buildTask(this, () -> {
            checkForUpdates();
        }).delay(5, TimeUnit.SECONDS).schedule();
    }

    private void checkForUpdates() {
        updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                updateChecker.notifyUpdate();
            } else {
                logger.info(getLogMessage("update-latest",
                    "{version}", updateChecker.getCurrentVersion()));
            }
        });
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
        
        // 尝试从 messages.log 获取
        String message = config.node("messages", "log", key).getString();
        
        // 如果没有，尝试从 messages 直接获取
        if (message == null) {
            message = config.node("messages", key).getString();
        }

        // 如果还是没有，返回 key 本身
        if (message == null) {
            return key;
        }
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    public Logger getLogger() {
        return logger;
    }
}
