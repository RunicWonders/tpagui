package cn.ningmo.tpagui.form;

import cn.ningmo.tpagui.TpaGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.geysermc.cumulus.SimpleForm;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;

public class BedrockFormManager {
    
    public static void openTpaForm(Player player) {
        // 获取在线玩家列表（排除自己）
        List<String> playerNames = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != player) {
                playerNames.add(p.getName());
            }
        }
        
        if (playerNames.isEmpty()) {
            player.sendMessage(TpaGui.getInstance().getMessage("no-players-online"));
            return;
        }

        // 创建表单
        CustomForm form = CustomForm.builder()
            .title(TpaGui.getInstance().getMessage("form.title"))
            .dropdown(
                TpaGui.getInstance().getMessage("form.player-select"),
                playerNames.toArray(new String[0])
            )
            .toggle(
                TpaGui.getInstance().getMessage("form.tpahere-toggle")
            )
            .responseHandler((form1, response) -> {
                if (response == null) {
                    // 玩家关闭表单
                    TpaGui.getInstance().getLogger().info(player.getName() + " 关闭了传送请求菜单");
                    return;
                }
                
                try {
                    // 解析JSON响应
                    JsonArray jsonArray = new JsonParser().parse(response).getAsJsonArray();
                    int selectedIndex = jsonArray.get(0).getAsInt();
                    boolean isTpaHere = jsonArray.get(1).getAsBoolean();

                    String targetName = playerNames.get(selectedIndex);
                    Player target = Bukkit.getPlayer(targetName);
                    if (target == null || !target.isOnline()) {
                        player.sendMessage(TpaGui.getInstance().getMessage("player-offline"));
                        return;
                    }

                    // 从配置文件获取命令
                    String tpaCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.to-player", "tpa");
                    String tpaHereCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.here", "tpahere");
                    
                    // 构建命令
                    String command = isTpaHere ? "/" + tpaHereCommand + " " + targetName : "/" + tpaCommand + " " + targetName;
                    
                    // 记录到控制台
                    TpaGui.getInstance().getLogger().info(player.getName() + " 通过GUI执行命令: " + command);
                    
                    // 执行命令
                    player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(TpaGui.getInstance(), true));
                    try {
                        player.chat(command);
                    } finally {
                        player.removeMetadata("TPAGUI_COMMAND", TpaGui.getInstance());
                    }
                } catch (Exception e) {
                    if (response != null) {  // 只在非关闭表单时显示错误
                        player.sendMessage(TpaGui.getInstance().getMessage("form-error"));
                        TpaGui.getInstance().getLogger().warning("处理表单响应时出错: " + e.getMessage());
                    }
                }
            })
            .build();

        // 发送表单给玩家
        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(form);
    }

    private static void executeDenyCommands(Player player) {
        List<String> denyCommands = TpaGui.getInstance().getConfig().getStringList("commands.deny");
        player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(TpaGui.getInstance(), true));
        try {
            for (String cmd : denyCommands) {
                player.chat("/" + cmd);
            }
        } finally {
            player.removeMetadata("TPAGUI_COMMAND", TpaGui.getInstance());
        }
    }

    private static void executeAcceptCommands(Player player, String requester) {
        List<String> acceptCommands = TpaGui.getInstance().getConfig().getStringList("commands.accept");
        player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(TpaGui.getInstance(), true));
        try {
            for (String cmd : acceptCommands) {
                player.chat("/" + cmd + " " + requester);
            }
        } finally {
            player.removeMetadata("TPAGUI_COMMAND", TpaGui.getInstance());
        }
    }

    public static void sendTpaRequestForm(Player target, String requester, boolean isTpaHere) {
        // 添加调试日志
        TpaGui.getInstance().getLogger().info("准备发送表单给 " + target.getName());
        
        try {
            String title = TpaGui.getInstance().getMessage("form.request.title");
            String content = TpaGui.getInstance().getMessage(
                isTpaHere ? "form.request.content-here" : "form.request.content-to",
                "{player}", requester
            );
            
            // 添加调试日志
            TpaGui.getInstance().getLogger().info("表单内容: " + title + " | " + content);
            
            SimpleForm form = SimpleForm.builder()
                .title(title)
                .content(content)
                .button(TpaGui.getInstance().getMessage("form.request.accept"))
                .button(TpaGui.getInstance().getMessage("form.request.deny"))
                .responseHandler((form1, response) -> {
                    if (response == null || response.trim().isEmpty()) {
                        // 玩家关闭表单，记录到控制台并发送消息
                        TpaGui.getInstance().getLogger().info(target.getName() + " 关闭了来自 " + requester + " 的传送请求表单");
                        target.sendMessage(TpaGui.getInstance().getMessage("form.request.closed", "{player}", requester));
                        // 执行拒绝命令
                        executeDenyCommands(target);
                        return;
                    }
                    
                    try {
                        // 去除可能的空白字符
                        int buttonId = Integer.parseInt(response.trim());
                        if (buttonId == 0) {
                            // 记录到控制台
                            TpaGui.getInstance().getLogger().info(target.getName() + " 接受了来自 " + requester + " 的传送请求");
                            
                            // 执行接受命令
                            executeAcceptCommands(target, requester);
                            
                            // 发送确认消息
                            target.sendMessage(TpaGui.getInstance().getMessage("form.request.accepted", "{player}", requester));
                        } else {
                            // 记录拒绝到控制台
                            TpaGui.getInstance().getLogger().info(target.getName() + " 拒绝了来自 " + requester + " 的传送请求");
                            // 发送拒绝消息
                            target.sendMessage(TpaGui.getInstance().getMessage("form.request.denied", "{player}", requester));
                            // 执行拒绝命令
                            executeDenyCommands(target);
                        }
                    } catch (NumberFormatException e) {
                        // 记录错误，但不显示给玩家，因为可能是关闭表单导致的
                        TpaGui.getInstance().getLogger().fine("表单响应解析: " + e.getMessage());
                        // 当作关闭表单处理
                        TpaGui.getInstance().getLogger().info(target.getName() + " 关闭了来自 " + requester + " 的传送请求表单");
                        target.sendMessage(TpaGui.getInstance().getMessage("form.request.closed", "{player}", requester));
                        executeDenyCommands(target);
                    }
                })
                .build();
            
            // 获取 FloodgatePlayer 并发送表单
            var floodgatePlayer = FloodgateApi.getInstance().getPlayer(target.getUniqueId());
            if (floodgatePlayer != null) {
                floodgatePlayer.sendForm(form);
                // 添加调试日志
                TpaGui.getInstance().getLogger().info("表单已发送给 " + target.getName());
            } else {
                TpaGui.getInstance().getLogger().warning("无法获取 " + target.getName() + " 的 FloodgatePlayer");
            }
        } catch (Exception e) {
            TpaGui.getInstance().getLogger().severe("发送表单时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}