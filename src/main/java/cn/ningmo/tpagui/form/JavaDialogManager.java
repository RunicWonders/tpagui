package cn.ningmo.tpagui.form;

import cn.ningmo.tpagui.TpaGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class JavaDialogManager {

    /**
     * 发送 TPA 请求对话框 (Java 1.21.6+ /dialog)
     * 注意：这需要服务器安装了预定义的 tpagui:request 对话框数据包
     * 
     * @param target 目标玩家
     * @param requester 请求者名称
     * @param isTpaHere 是否为 tpahere
     */
    public static void sendTpaRequestDialog(Player target, String requester, boolean isTpaHere) {
        TpaGui plugin = TpaGui.getInstance();
        
        // 检查配置和版本支持
        if (!plugin.isDialogSupported() || !plugin.getConfig().getBoolean("java-dialog-gui.enabled", true)) {
            return;
        }

        // 构造指令
        // 我们假设用户安装了配套的数据包，定义了 tpagui:request_to 和 tpagui:request_here
        String dialogId = isTpaHere ? "tpagui:request_here" : "tpagui:request_to";
        String command = "dialog open " + target.getName() + " " + dialogId;

        // 执行指令
        if (plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, (task) -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        
        plugin.getLogger().info(plugin.getLogMessage("dialog-sent", 
            "{player}", target.getName(), 
            "{dialog}", dialogId));
    }
}
