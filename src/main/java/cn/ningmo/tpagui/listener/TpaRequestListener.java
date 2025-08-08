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
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        
        // 从配置文件获取监听的命令列表
        java.util.List<String> listenCommands = TpaGui.getInstance().getConfig().getStringList("commands.listen-commands");
        
        // 检查是否是配置的tpa相关命令
        boolean isTpaCommand = false;
        for (String cmd : listenCommands) {
            if (command.startsWith("/" + cmd.toLowerCase() + " ")) {
                isTpaCommand = true;
                break;
            }
        }
        
        if (!isTpaCommand) {
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
            
            // 从配置文件获取tpahere命令
            String tpaHereCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.here", "tpahere");
            
            // 发送表单
            BedrockFormManager.sendTpaRequestForm(
                target, 
                event.getPlayer().getName(), 
                command.startsWith("/" + tpaHereCommand.toLowerCase() + " ")
            );
            
            // 调试信息
            TpaGui.getInstance().getLogger().info(
                "发送传送请求表单给 " + target.getName() + 
                " 从 " + event.getPlayer().getName() + 
                " 类型: " + (command.startsWith("/" + tpaHereCommand.toLowerCase() + " ") ? tpaHereCommand : TpaGui.getInstance().getConfig().getString("commands.tpa.to-player", "tpa"))
            );
        }
    }
}