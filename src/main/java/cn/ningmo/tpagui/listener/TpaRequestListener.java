package cn.ningmo.tpagui.listener;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.form.JavaDialogManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TpaRequestListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
        TpaGui plugin = TpaGui.getInstance();
        String tpaHereCommand = plugin.getConfig().getString("commands.tpa.here", "tpahere");
        boolean isTpaHere = command.startsWith("/" + tpaHereCommand.toLowerCase() + " ");

        if (target == null) {
            // 如果本地找不到，检查是否为跨服玩家
            if (plugin.getConfig().getBoolean("velocity.enabled", false)) {
                cn.ningmo.tpagui.data.GlobalPlayer gp = cn.ningmo.tpagui.data.PlayerManager.getGlobalPlayer(targetName);
                if (gp != null) {
                    // 发送跨服请求通知给 Velocity
                    com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                    out.writeUTF("ShowRequest");
                    out.writeUTF(targetName);
                    out.writeUTF(event.getPlayer().getName());
                    out.writeBoolean(isTpaHere);
                    event.getPlayer().sendPluginMessage(plugin, "tpagui:main", out.toByteArray());
                }
            }
            return;
        }

        // 分发传送请求：基岩版表单 -> Java Dialog -> 聊天消息兜底
        JavaDialogManager.dispatchTpaRequest(target, event.getPlayer().getName(), isTpaHere);
    }
}