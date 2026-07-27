package cn.ningmo.tpagui.velocity;

import cn.ningmo.tpagui.UpdateChecker;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
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
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String DEFAULT_LANGUAGE = "zh_CN";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private CommentedConfigurationNode config;
    private UpdateChecker updateChecker;
    private String currentLanguage = DEFAULT_LANGUAGE;
    private final Map<String, Map<String, Object>> languageFiles = new ConcurrentHashMap<>();

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
        loadLanguage();
        logger.info(getLogMessage("velocity-enabled"));
        logger.info(getLogMessage("velocity-environment"));
        
        // 注册插件消息通道
        server.getChannelRegistrar().register(IDENTIFIER);
        
        // 初始化更新检查器
        updateChecker = new UpdateChecker(
            "1.2.0-beta.6",
            msg -> logger.warn(msg),
            key -> getLogMessage(key),
            // config 加载失败时为 null，此时跳过更新检查
            () -> config != null && config.node("update-check", "enabled").getBoolean(true)
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
        
        // 注意：代理端无法感知子服的隐身（vanish）状态，这里只能发送代理可见的全部玩家。
        // 隐身玩家的过滤属于协议层面的限制，无法在不改动协议的前提下通用修复。
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

    /**
     * 加载语言文件（从 jar 内资源读取），按 config 的 language 键选择
     */
    private void loadLanguage() {
        if (config != null) {
            currentLanguage = config.node("language").getString(DEFAULT_LANGUAGE);
        }
        // 预加载当前语言与默认语言（用于缺失键回退）
        loadLanguageFile(currentLanguage);
        loadLanguageFile(DEFAULT_LANGUAGE);
    }

    private void loadLanguageFile(String language) {
        if (languageFiles.containsKey(language)) {
            return;
        }
        try (InputStream in = getClass().getResourceAsStream("/lang/" + language + ".yml")) {
            if (in == null) {
                logger.warn("Language file not found in jar: {}.yml", language);
                languageFiles.put(language, Collections.emptyMap());
                return;
            }
            Map<String, Object> data = new Yaml().load(in);
            languageFiles.put(language, data != null ? data : Collections.emptyMap());
        } catch (IOException e) {
            logger.error("Could not load language file: " + language, e);
            languageFiles.put(language, Collections.emptyMap());
        }
    }

    /**
     * 从语言文件按点分隔路径取值，优先当前语言，缺失时回退默认语言，再缺失返回 null
     */
    private String getRawMessage(String path) {
        String message = getRawMessage(currentLanguage, path);
        if (message == null && !DEFAULT_LANGUAGE.equals(currentLanguage)) {
            message = getRawMessage(DEFAULT_LANGUAGE, path);
        }
        return message;
    }

    private String getRawMessage(String language, String path) {
        Map<String, Object> langData = languageFiles.get(language);
        if (langData == null) {
            return null;
        }
        Object current = langData;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return current instanceof String ? (String) current : null;
    }

    public String getLogMessage(String key, String... placeholders) {
        // 优先取 log 节点下的日志文案，与 Bukkit 端 LanguageManager 行为一致
        String message = getRawMessage("log." + key);
        if (message == null) {
            message = getRawMessage(key);
        }
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
}
