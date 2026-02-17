package cn.ningmo.tpagui;

import cn.ningmo.tpagui.listener.TpaRequestListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;

public class TpaGui extends JavaPlugin {
    private static TpaGui instance;
    private boolean floodgateEnabled = false;
    private boolean isFolia = false;
    private boolean isDialogSupported = false;
    private UpdateChecker updateChecker;
    private LanguageManager languageManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 1. 确保配置文件已释放到磁盘
        // 即使之后由于环境检查而禁用，也要确保用户能看到配置文件
        try {
            saveDefaultConfig();
            reloadConfig();
        } catch (Exception e) {
            getLogger().severe("无法释放或加载配置文件: " + e.getMessage());
        }
        
        // 2. 初始化语言管理器
        languageManager = new LanguageManager(this);
        
        // 2. 检查是否在代理环境下（BungeeCord/Velocity 子服）
        boolean isProxy = false;
        try {
            // 检查 Spigot 的 BungeeCord 设置
            isProxy = getServer().spigot().getConfig().getBoolean("settings.bungeecord", false);
        } catch (Throwable ignored) {
            // 非 Spigot 环境，尝试通过消息通道判断
            isProxy = getServer().getMessenger().getIncomingChannels().contains("velocity:main") || 
                      getServer().getMessenger().getIncomingChannels().contains("bungeecord:main");
        }
        
        if (isProxy) {
            // 如果在代理环境下，检查是否开启了 Velocity 跨服模式
            if (getConfig().getBoolean("velocity.enabled", false)) {
                getLogger().info(getLogMessage("velocity-mode-enabled"));
                // 注册插件消息通道
                getServer().getMessenger().registerOutgoingPluginChannel(this, "tpagui:main");
                getServer().getMessenger().registerIncomingPluginChannel(this, "tpagui:main", new cn.ningmo.tpagui.messaging.PluginMessageHandler());
            } else {
                // 不再自动禁用插件，仅输出环境提示
                getLogger().info(getLogMessage("proxy-detected"));
                getLogger().info("当前处于代理子服环境，但未开启跨服模式 (velocity.enabled: false)。");
                getLogger().info("插件将以本地模式运行。");
            }
        } else {
            // 非代理环境下，如果开启了 velocity.enabled，也需要注册通道
            // 某些环境下 isProxy 检测可能不准确
            if (getConfig().getBoolean("velocity.enabled", false)) {
                getLogger().info("未检测到代理环境，但 velocity.enabled 为 true，正在注册消息通道...");
                getServer().getMessenger().registerOutgoingPluginChannel(this, "tpagui:main");
                getServer().getMessenger().registerIncomingPluginChannel(this, "tpagui:main", new cn.ningmo.tpagui.messaging.PluginMessageHandler());
            }
        }

        // 检查是否为Folia
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
        
        if (isFolia) {
            getLogger().info(getLogMessage("folia-detected"));
        }

        // 检查 1.21.6+ /dialog 支持
        // 25w02a 对应的版本是 1.21.6
        String version = getServer().getBukkitVersion();
        if (version.contains("1.21.6") || version.contains("1.21.7") || version.contains("1.22")) {
            isDialogSupported = true;
            getLogger().info(getLogMessage("dialog-supported"));
        }
        
        // 检查Floodgate
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateEnabled = true;
            getLogger().info(getLogMessage("floodgate-enabled"));
        }
        
        // 注册命令
        PluginCommand command = getCommand("tpagui");
        if (command != null) {
            command.setExecutor(new TpaGuiCommand());
            command.setTabCompleter(new TpaGuiTabCompleter());
        }
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new TpaRequestListener(), this);
        
        // 初始化更新检查器
        updateChecker = new UpdateChecker(this);
        
        // 检查更新（延迟5秒，避免影响启动速度）
        if (isFolia) {
            getServer().getAsyncScheduler().runDelayed(this, (task) -> {
                checkForUpdates();
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                checkForUpdates();
            }, 100L); // 5秒 = 100 ticks
        }
    }
    
    public boolean isFolia() {
        return isFolia;
    }

    public boolean isDialogSupported() {
        return isDialogSupported;
    }
    
    /**
     * 检查更新
     */
    private void checkForUpdates() {
        if (!getConfig().getBoolean("update-check.enabled", true)) {
            return;
        }
        
        updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                updateChecker.notifyUpdate();
                // 发送给在线管理员
                if (isFolia) {
                    getServer().getGlobalRegionScheduler().run(this, (task) -> sendUpdateMessageToAdmins());
                } else {
                    getServer().getScheduler().runTask(this, this::sendUpdateMessageToAdmins);
                }
            } else {
                getLogger().info(getLogMessage("update-latest",
                    "{version}", updateChecker.getCurrentVersion()));
            }
        });
    }

    /**
     * 发送更新消息给管理员
     */
    private void sendUpdateMessageToAdmins() {
        String playerMessage = getMessage("update-available",
            "{current}", updateChecker.getCurrentVersion(),
            "{latest}", updateChecker.getLatestVersion(),
            "{url}", updateChecker.getDownloadUrl());
        
        getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("tpagui.admin"))
            .forEach(player -> player.sendMessage(playerMessage));
    }
    
    /**
     * 获取更新检查器
     * @return 更新检查器
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    public static TpaGui getInstance() {
        return instance;
    }
    
    public boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public String getMessage(String path) {
        return languageManager.getMessage(path);
    }
    
    public String getMessage(String path, String... placeholders) {
        return languageManager.getMessage(path, placeholders);
    }
    
    /**
     * 获取日志消息（不带颜色代码）
     * @param path 消息路径
     * @param placeholders 占位符，格式: key1, value1, key2, value2, ...
     * @return 日志消息
     */
    public String getLogMessage(String path, String... placeholders) {
        return languageManager.getLogMessage(path, placeholders);
    }
} 