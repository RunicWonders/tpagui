package cn.ningmo.tpagui;

import cn.ningmo.tpagui.listener.TpaRequestListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;

public class TpaGui extends JavaPlugin {
    private static TpaGui instance;
    private boolean floodgateEnabled = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 检查Floodgate
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateEnabled = true;
            getLogger().info("已检测到Floodgate，基岩版表单支持已启用");
        }
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 注册命令
        PluginCommand command = getCommand("tpagui");
        if (command != null) {
            command.setExecutor(new TpaGuiCommand());
            command.setTabCompleter(new TpaGuiTabCompleter());
        }
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new TpaRequestListener(), this);
    }
    
    public static TpaGui getInstance() {
        return instance;
    }
    
    public boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }
    
    public String getMessage(String path) {
        String message = getConfig().getString("messages." + path, path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
} 