package cn.ningmo.tpagui.listener;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.form.BedrockFormManager;
import cn.ningmo.tpagui.form.JavaDialogManager;
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

        TpaGui plugin = TpaGui.getInstance();
        String tpaHereCommand = plugin.getConfig().getString("commands.tpa.here", "tpahere");
        boolean isTpaHere = command.startsWith("/" + tpaHereCommand.toLowerCase() + " ");
        
        // 检查目标玩家是否为基岩版玩家
        if (plugin.isFloodgateEnabled()) {
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                if (api != null && api.isFloodgatePlayer(target.getUniqueId())) {
                    // 发送基岩版表单
                    BedrockFormManager.sendTpaRequestForm(
                        target, 
                        event.getPlayer().getName(), 
                        isTpaHere
                    );
                    
                    // 调试信息
                    String commandType = isTpaHere ? tpaHereCommand : plugin.getConfig().getString("commands.tpa.to-player", "tpa");
                    plugin.getLogger().info(
                        plugin.getLogMessage("send-request-form",
                            "{target}", target.getName(),
                            "{requester}", event.getPlayer().getName(),
                            "{type}", commandType)
                    );
                    return; // 已处理，返回
                }
            } catch (Exception e) {
                plugin.getLogger().warning(
                    plugin.getLogMessage("floodgate-request-error",
                        "{error}", e.getMessage())
                );
            }
        }

        // 如果不是基岩版玩家，检查是否支持 Java 1.21.6+ /dialog
        if (plugin.isDialogSupported() && plugin.getConfig().getBoolean("java-dialog-gui.enabled", true)) {
            JavaDialogManager.sendTpaRequestDialog(target, event.getPlayer().getName(), isTpaHere);
        }
    }
}