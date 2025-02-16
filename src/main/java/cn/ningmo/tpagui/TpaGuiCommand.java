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
        
        // 检查是否为基岩版玩家
        if (TpaGui.getInstance().isFloodgateEnabled() && 
            FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            BedrockFormManager.openTpaForm(player);
        } else {
            player.openInventory(GuiManager.createTpaMenu(player, 0));
        }
        
        return true;
    }
} 