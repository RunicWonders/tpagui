package cn.ningmo.tpagui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import cn.ningmo.tpagui.menu.GuiManager;
import org.bukkit.Bukkit;
import cn.ningmo.tpagui.form.BedrockFormManager;
import org.geysermc.floodgate.api.FloodgateApi;

public class TpaGuiCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            TpaGui plugin = TpaGui.getInstance();
            sender.sendMessage(plugin.getMessage("console-list-header"));
            if (plugin.getConfig().getBoolean("velocity.enabled", false)) {
                // Velocity 模式：列出 PlayerManager 汇总的跨服玩家（含所在服务器）
                for (cn.ningmo.tpagui.data.GlobalPlayer gp : cn.ningmo.tpagui.data.PlayerManager.getGlobalPlayers()) {
                    // 同服隐身玩家（SuperVanish 等）对控制台同样隐藏
                    Player localPlayer = Bukkit.getPlayer(gp.getUuid());
                    if (localPlayer != null && isVanished(localPlayer)) continue;
                    sender.sendMessage(plugin.getMessage("console-list-format",
                        "{player}", gp.getName() + " (" + gp.getServer() + ")"));
                }
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // 控制台没有"视角"，player.canSee 不适用，改用 metadata 检测隐身玩家
                    if (isVanished(player)) continue;
                    sender.sendMessage(plugin.getMessage("console-list-format", 
                        "{player}", player.getName()));
                }
            }
            return true;
        }
        
        Player player = (Player) sender;
        
        // 如果开启了 Velocity 模式，且需要更新玩家列表
        if (TpaGui.getInstance().getConfig().getBoolean("velocity.enabled", false)) {
            if (cn.ningmo.tpagui.data.PlayerManager.shouldUpdate()) {
                // 标记本玩家正在等待列表响应，避免重复请求
                cn.ningmo.tpagui.data.PlayerManager.markPending(player.getName());
                cn.ningmo.tpagui.data.PlayerManager.requestUpdate(player);
            }
        }
        
        // 检查是否为基岩版玩家
        if (TpaGui.getInstance().isFloodgateEnabled()) {
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                if (api != null && api.isFloodgatePlayer(player.getUniqueId())) {
                    BedrockFormManager.openTpaForm(player);
                    return true;
                }
            } catch (Exception e) {
                TpaGui.getInstance().getLogger().warning(
                    TpaGui.getInstance().getLogMessage("floodgate-check-error", 
                        "{error}", e.getMessage())
                );
            }
        }
        
        // Java版玩家或Floodgate不可用时使用GUI菜单
        player.openInventory(GuiManager.createTpaMenu(player, 0));
        
        return true;
    }
    
    /**
     * 检测玩家是否隐身（SuperVanish/PremiumVanish 等通过 "vanished" metadata 标记隐身玩家）
     * 用于控制台等无法使用 player.canSee 判断的场景
     */
    private boolean isVanished(Player player) {
        for (org.bukkit.metadata.MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) {
                return true;
            }
        }
        return false;
    }
} 