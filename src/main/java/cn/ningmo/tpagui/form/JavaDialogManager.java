package cn.ningmo.tpagui.form;

import cn.ningmo.tpagui.TpaGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class JavaDialogManager {

    /**
     * 统一分发传送请求：基岩版表单 -> Java Dialog -> 聊天消息兜底
     * 供 TpaRequestListener 和 PluginMessageHandler 共同调用
     *
     * @param target 目标玩家
     * @param requesterName 请求者名称
     * @param isTpaHere 是否为 tpahere
     */
    public static void dispatchTpaRequest(Player target, String requesterName, boolean isTpaHere) {
        TpaGui plugin = TpaGui.getInstance();

        // 检查目标玩家是否为基岩版玩家
        if (plugin.isFloodgateEnabled()) {
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                if (api != null && api.isFloodgatePlayer(target.getUniqueId())) {
                    // 发送基岩版表单
                    BedrockFormManager.sendTpaRequestForm(target, requesterName, isTpaHere);

                    // 调试信息
                    String commandType = isTpaHere
                        ? plugin.getConfig().getString("commands.tpa.here", "tpahere")
                        : plugin.getConfig().getString("commands.tpa.to-player", "tpa");
                    plugin.getLogger().info(
                        plugin.getLogMessage("send-request-form",
                            "{target}", target.getName(),
                            "{requester}", requesterName,
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

        // 如果不是基岩版玩家，尝试发送 Java 1.21.6+ 原生 Dialog
        // （版本与配置检查已收敛到 sendTpaRequestDialog 内部）
        if (sendTpaRequestDialog(target, requesterName, isTpaHere)) {
            return;
        }

        // 兜底：Dialog 不可用（版本 <1.21.6 或配置关闭）时发送聊天消息提示
        String commandType = isTpaHere
            ? plugin.getConfig().getString("commands.tpa.here", "tpahere")
            : plugin.getConfig().getString("commands.tpa.to-player", "tpa");
        target.sendMessage(plugin.getMessage("messages.tpa-request-notify",
            "{player}", requesterName,
            "{type}", commandType));
    }

    /**
     * 发送 TPA 请求对话框 (Java 1.21.6+ /dialog)
     * 注意：这需要服务器安装了预定义的 tpagui:request 对话框数据包
     *
     * @param target 目标玩家
     * @param requester 请求者名称
     * @param isTpaHere 是否为 tpahere
     * @return 是否真正发出了 dialog（版本不支持或配置关闭时返回 false）
     */
    public static boolean sendTpaRequestDialog(Player target, String requester, boolean isTpaHere) {
        TpaGui plugin = TpaGui.getInstance();
        
        // 检查配置和版本支持
        if (!plugin.isDialogSupported() || !plugin.getConfig().getBoolean("java-dialog-gui.enabled", true)) {
            return false;
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
        return true;
    }
}
