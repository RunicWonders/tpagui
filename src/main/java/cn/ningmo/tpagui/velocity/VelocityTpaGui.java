package cn.ningmo.tpagui.velocity;

import cn.ningmo.tpagui.UpdateChecker;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "tpagui",
    name = "TpaGui",
    version = "1.2.0-beta.6",
    description = "A simple TPA GUI plugin for Velocity",
    authors = {"lemwood"}
)
public class VelocityTpaGui {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("tpagui:main");

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
        
        // 注册插件消息通道
        server.getChannelRegistrar().register(IDENTIFIER);
        
        // 初始化更新检查器
        updateChecker = new UpdateChecker(
            "1.2.0-beta.6",
            msg -> logger.warn(msg),
            key -> getLogMessage(key),
            () -> config.node("update-check", "enabled").getBoolean(true)
        );
        
        // 检查更新
        server.getScheduler().buildTask(this, () -> {
            checkForUpdates();
        }).delay(5, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(IDENTIFIER)) {
            return;
        }

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ServerConnection connection = (ServerConnection) event.getSource();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();

        if (subChannel.equals("GetPlayers")) {
            sendPlayerList(connection);
        } else if (subChannel.equals("ShowRequest")) {
            handleShowRequest(in);
        }
    }

    private void handleShowRequest(ByteArrayDataInput in) {
        String targetName = in.readUTF();
        String requesterName = in.readUTF();
        boolean isTpaHere = in.readBoolean();

        server.getPlayer(targetName).ifPresent(target -> {
            target.getCurrentServer().ifPresent(serverConn -> {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("ShowRequest");
                out.writeUTF(targetName);
                out.writeUTF(requesterName);
                out.writeBoolean(isTpaHere);
                serverConn.sendPluginMessage(IDENTIFIER, out.toByteArray());
            });
        });
    }

    private void sendPlayerList(ServerConnection connection) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        
        List<Player> players = new ArrayList<>(server.getAllPlayers());
        out.writeInt(players.size());
        
        for (Player p : players) {
            out.writeUTF(p.getUsername());
            out.writeUTF(p.getUniqueId().toString());
            out.writeUTF(p.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown"));
        }
        
        connection.sendPluginMessage(IDENTIFIER, out.toByteArray());
    }

    private void checkForUpdates() {
        updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                logger.info(getLogMessage("update-available"));
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
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                } else {
                    Files.createFile(configFile);
                }
            } catch (IOException e) {
                logger.error("Could not create default config", e);
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
        String message = config.node("messages", "log", key).getString();
        if (message == null) {
            // 尝试从普通 messages 节点获取
            message = config.node("messages", key).getString();
        }
        if (message == null) return key;

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
}
