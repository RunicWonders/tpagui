package cn.ningmo.tpagui.listener;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.form.BedrockFormManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.geysermc.floodgate.api.FloodgateApi;

public class TpaRequestListener implements Listener {
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        
        // 检查是否是tpa相关命令
        if (!command.startsWith("/tpa ") && !command.startsWith("/tpahere ")) {
            return;
        }
        
        String[] args = event.getMessage().split(" ");
        if (args.length < 2) return;
        
        // 获取目标玩家
        String targetName = args[1];
        Player target = event.getPlayer().getServer().getPlayer(targetName);
        if (target == null) return;
        
        // 检查目标玩家是否为基岩版玩家
        if (TpaGui.getInstance().isFloodgateEnabled() && 
            FloodgateApi.getInstance().isFloodgatePlayer(target.getUniqueId())) {
            
            // 发送表单
            BedrockFormManager.sendTpaRequestForm(
                target, 
                event.getPlayer().getName(), 
                command.startsWith("/tpahere ")
            );
            
            // 调试信息
            TpaGui.getInstance().getLogger().info(
                "发送传送请求表单给 " + target.getName() + 
                " 从 " + event.getPlayer().getName() + 
                " 类型: " + (command.startsWith("/tpahere ") ? "tpahere" : "tpa")
            );
            
            // 如果是从GUI发出的命令，取消事件以防止重复处理
            if (event.getPlayer().hasMetadata("TPAGUI_COMMAND")) {
                event.setCancelled(true);
            }
        }
    }
} 