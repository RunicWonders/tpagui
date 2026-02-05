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
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 保存并加载配置
        saveDefaultConfig();
        reloadConfig();
        
        // 检查是否在 Velocity 代理环境下
        if (getServer().getMessenger().getIncomingChannels().contains("velocity:main") || 
            getServer().spigot().getConfig().getBoolean("settings.bungeecord", false)) {
            getLogger().info(getLogMessage("proxy-detected"));
            getServer().getPluginManager().disablePlugin(this);
            return;
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
    
    public String getMessage(String path) {
        String message = getConfig().getString("messages." + path);
        if (message == null) {
            return path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        if (message.equals(path)) return path;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
    
    /**
     * 获取日志消息（不带颜色代码）
     * @param path 消息路径
     * @param placeholders 占位符，格式: key1, value1, key2, value2, ...
     * @return 日志消息
     */
    public String getLogMessage(String path, String... placeholders) {
        String message = getConfig().getString("messages.log." + path);
        if (message == null) {
            message = getConfig().getString("messages." + path);
        }
        if (message == null) {
            return path;
        }
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
} 