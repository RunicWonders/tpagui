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
            sender.sendMessage(TpaGui.getInstance().getMessage("console-list-header"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                sender.sendMessage(TpaGui.getInstance().getMessage("console-list-format", 
                    "{player}", player.getName()));
            }
            return true;
        }
        
        Player player = (Player) sender;
        
        // 如果开启了 Velocity 模式，且需要更新玩家列表
        if (TpaGui.getInstance().getConfig().getBoolean("velocity.enabled", false)) {
            if (cn.ningmo.tpagui.data.PlayerManager.shouldUpdate()) {
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
} 