package cn.ningmo.tpagui;

import cn.ningmo.tpagui.listener.TpaRequestListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TpaGui extends JavaPlugin {
    private static final Pattern SNAPSHOT_VERSION = Pattern.compile(
        "^(\\d{2})w\\d{2}[a-z](?:[-.].*)?$", Pattern.CASE_INSENSITIVE);
    private static final int FIRST_DIALOG_SNAPSHOT_YEAR = 26;

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
        
        // 3. 检查是否在代理环境下（BungeeCord/Velocity 子服）
        // Paper 26.3 起 Server#spigot() 已标记移除，改用插件消息通道判断。
        boolean isProxy = getServer().getMessenger().getIncomingChannels().contains("velocity:main")
            || getServer().getMessenger().getIncomingChannels().contains("bungeecord:main");
        
        // 插件消息处理器只实例化一次，两个分支共用
        cn.ningmo.tpagui.messaging.PluginMessageHandler messageHandler = new cn.ningmo.tpagui.messaging.PluginMessageHandler();
        
        if (isProxy) {
            // 如果在代理环境下，检查是否开启了 Velocity 跨服模式
            if (getConfig().getBoolean("velocity.enabled", false)) {
                getLogger().info(getLogMessage("velocity-mode-enabled"));
                // 注册插件消息通道
                getServer().getMessenger().registerOutgoingPluginChannel(this, "tpagui:main");
                getServer().getMessenger().registerIncomingPluginChannel(this, "tpagui:main", messageHandler);
            } else {
                // 不再自动禁用插件，仅输出环境提示
                getLogger().info(getLogMessage("proxy-detected"));
                getLogger().info(getLogMessage("proxy-mode-local"));
            }
        } else {
            // 非代理环境下，如果开启了 velocity.enabled，也需要注册通道
            // 某些环境下 isProxy 检测可能不准确
            if (getConfig().getBoolean("velocity.enabled", false)) {
                getLogger().info(getLogMessage("velocity-registering-channels"));
                getServer().getMessenger().registerOutgoingPluginChannel(this, "tpagui:main");
                getServer().getMessenger().registerIncomingPluginChannel(this, "tpagui:main", messageHandler);
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
        String version = getServer().getBukkitVersion();
        if (isDialogVersionSupported(version)) {
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
        // update-check.interval > 0 时按该分钟数周期异步检查，0 表示仅在启动时检查
        long updateIntervalMinutes = getConfig().getLong("update-check.interval", 0);
        if (isFolia) {
            getServer().getAsyncScheduler().runDelayed(this, (task) -> {
                checkForUpdates();
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (updateIntervalMinutes > 0) {
                getServer().getAsyncScheduler().runAtFixedRate(this, (task) -> {
                    checkForUpdates();
                }, updateIntervalMinutes, updateIntervalMinutes, java.util.concurrent.TimeUnit.MINUTES);
            }
        } else {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                checkForUpdates();
            }, 100L); // 5秒 = 100 ticks
            if (updateIntervalMinutes > 0) {
                long intervalTicks = updateIntervalMinutes * 60 * 20L;
                getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                    checkForUpdates();
                }, intervalTicks, intervalTicks);
            }
        }
    }
    
    @Override
    public void onDisable() {
        cn.ningmo.tpagui.listener.TpaRequestListener.pendingRequests.clear();
        // 注销插件消息通道
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        // PlayerManager 暂无 clear 方法，其静态状态随插件卸载自然失效，此处无需清理
    }
    
    /**
     * 数值比较 Bukkit 版本号（如 "1.21.6-R0.1-SNAPSHOT" 或 "26.3.build.235-stable"）
     * @return 是否不低于 reqMajor.reqMinor.reqPatch
     */
    private boolean isDialogVersionSupported(String bukkitVersion) {
        Matcher snapshot = SNAPSHOT_VERSION.matcher(bukkitVersion);
        if (snapshot.matches()) {
            int year = Integer.parseInt(snapshot.group(1));
            // 25w snapshots span pre- and post-1.21.6 development; only newer years are unambiguous.
            return year >= FIRST_DIALOG_SNAPSHOT_YEAR;
        }
        return isVersionAtLeast(bukkitVersion, 1, 21, 6);
    }

    private boolean isVersionAtLeast(String bukkitVersion, int reqMajor, int reqMinor, int reqPatch) {
        try {
            // Paper 26.x 的 getBukkitVersion() 形如 26.3.build.235-stable，
            // 不能简单按 "." 取第三段，需跳过 build/pre 等非数字段。
            String[] parts = bukkitVersion.split("[-.]");
            int[] numbers = new int[3];
            int found = 0;
            for (String part : parts) {
                int end = 0;
                while (end < part.length() && Character.isDigit(part.charAt(end))) end++;
                if (end == 0) continue;
                numbers[found++] = Integer.parseInt(part.substring(0, end));
                if (found == 3) break;
            }
            if (found == 0) return false;
            int major = numbers[0];
            int minor = found > 1 ? numbers[1] : 0;
            int patch = found > 2 ? numbers[2] : 0;
            return major > reqMajor
                || (major == reqMajor && minor > reqMinor)
                || (major == reqMajor && minor == reqMinor && patch >= reqPatch);
        } catch (NumberFormatException e) {
            return false;
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
        
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            cn.ningmo.tpagui.form.SchedulerUtil.runTask(player, () -> {
                if (player.hasPermission("tpagui.admin")) player.sendMessage(playerMessage);
            });
        }
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
