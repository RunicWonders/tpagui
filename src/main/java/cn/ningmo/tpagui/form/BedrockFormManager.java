package cn.ningmo.tpagui.form;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.data.GlobalPlayer;
import cn.ningmo.tpagui.data.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BedrockFormManager {
    
    // 内部类，用于统一处理本地和跨服玩家
    private static class TargetInfo {
        final String name;
        final UUID uuid;
        final String server;

        TargetInfo(String name, UUID uuid, String server) {
            this.name = name;
            this.uuid = uuid;
            this.server = server;
        }
    }
    
    public static void openTpaForm(Player player) {
        openTpaForm(player, 0);
    }

    public static void openTpaForm(Player player, int page) {
        TpaGui plugin = TpaGui.getInstance();
        int playersPerPage = plugin.getConfig().getInt("java-dialog-gui.players-per-page", 20);
        boolean showAvatars = plugin.getConfig().getBoolean("java-dialog-gui.show-avatars", true);
        String avatarApi = plugin.getConfig().getString("java-dialog-gui.avatar-api", "https://mc-heads.net/avatar/{uuid}/64");

        // 获取在线玩家列表（排除自己）
        List<TargetInfo> availablePlayers = new ArrayList<>();
        
        if (plugin.getConfig().getBoolean("velocity.enabled", false)) {
            // Velocity 模式：获取全局玩家
            for (GlobalPlayer gp : PlayerManager.getGlobalPlayers()) {
                if (!gp.getUuid().equals(player.getUniqueId())) {
                    availablePlayers.add(new TargetInfo(gp.getName(), gp.getUuid(), gp.getServer()));
                }
            }
        } else {
            // 普通模式：获取本地在线玩家
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player) {
                    availablePlayers.add(new TargetInfo(p.getName(), p.getUniqueId(), "local"));
                }
            }
        }
        
        if (availablePlayers.isEmpty()) {
            player.sendMessage(plugin.getMessage("no-players-online"));
            return;
        }

        // 计算分页
        int totalPlayers = availablePlayers.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / playersPerPage);
        int start = page * playersPerPage;
        int end = Math.min(start + playersPerPage, totalPlayers);

        // 创建表单
        SimpleForm.Builder formBuilder = SimpleForm.builder()
            .title(plugin.getMessage("form.title") + (totalPages > 1 ? " (" + (page + 1) + "/" + totalPages + ")" : ""));

        // 添加玩家按钮
        for (int i = start; i < end; i++) {
            TargetInfo target = availablePlayers.get(i);
            String buttonText = target.name;
            if (!target.server.equals("local") && !target.server.equals("unknown")) {
                buttonText += " (" + target.server + ")";
            }
            
            if (showAvatars) {
                String imageUrl = avatarApi.replace("{uuid}", target.uuid.toString()).replace("{name}", target.name);
                formBuilder.button(buttonText, FormImage.Type.URL, imageUrl);
            } else {
                formBuilder.button(buttonText);
            }
        }

        // 添加导航按钮
        if (page > 0) {
            formBuilder.button(plugin.getMessage("gui.navigation.previous-page"), FormImage.Type.PATH, "textures/ui/left_arrow_custom");
        }
        if (page < totalPages - 1) {
            formBuilder.button(plugin.getMessage("gui.navigation.next-page"), FormImage.Type.PATH, "textures/ui/right_arrow_custom");
        }

        formBuilder.responseHandler((form, response) -> {
            if (response == null) {
                plugin.getLogger().info(plugin.getLogMessage("form-closed", "{player}", player.getName()));
                return;
            }

            try {
                int buttonId = Integer.parseInt(response.trim());
                int playerCountOnPage = end - start;

                if (buttonId < playerCountOnPage) {
                    // 点击了玩家按钮
                    TargetInfo target = availablePlayers.get(start + buttonId);
                    openActionSelectForm(player, target);
                } else {
                    // 点击了导航按钮
                    int navIndex = buttonId - playerCountOnPage;
                    boolean hasPrev = page > 0;
                    
                    if (hasPrev && navIndex == 0) {
                        // 上一页
                        openTpaForm(player, page - 1);
                    } else {
                        // 下一页
                        openTpaForm(player, page + 1);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getLogMessage("form-response-error", "{error}", e.getMessage()));
            }
        });

        sendForm(player, formBuilder.build());
    }

    private static void openActionSelectForm(Player player, TargetInfo target) {
        TpaGui plugin = TpaGui.getInstance();
        SimpleForm form = SimpleForm.builder()
            .title(plugin.getMessage("form.player-select") + ": " + target.name)
            .button(plugin.getMessage("form.action.tpa"), FormImage.Type.PATH, "textures/ui/multiplayer_glyph_color")
            .button(plugin.getMessage("form.action.tpahere"), FormImage.Type.PATH, "textures/ui/world_glyph_color")
            .button(plugin.getMessage("form.action.back"), FormImage.Type.PATH, "textures/ui/cancel")
            .responseHandler((form1, response) -> {
                if (response == null) return;
                
                int id = Integer.parseInt(response.trim());
                if (id == 0) {
                    // TPA: 传送到目标玩家
                    String cmdName = plugin.getConfig().getString("commands.tpa.to-player", "tpa");
                    String fullCommand = "/" + cmdName + " " + target.name;
                    runTask(player, () -> {
                        player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(plugin, true));
                        try {
                            player.chat(fullCommand);
                        } finally {
                            player.removeMetadata("TPAGUI_COMMAND", plugin);
                        }
                    });
                } else if (id == 1) {
                    // TPAHERE: 请求目标玩家传送到自己
                    String cmdName = plugin.getConfig().getString("commands.tpa.here", "tpahere");
                    String fullCommand = "/" + cmdName + " " + target.name;
                    runTask(player, () -> {
                        player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(plugin, true));
                        try {
                            player.chat(fullCommand);
                        } finally {
                            player.removeMetadata("TPAGUI_COMMAND", plugin);
                        }
                    });
                } else if (id == 2) {
                    // 返回主菜单
                    openTpaForm(player, 0);
                }
            })
            .build();
        
        sendForm(player, form);
    }

    private static void sendForm(Player player, Object form) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null) return;
            FloodgatePlayer fp = api.getPlayer(player.getUniqueId());
            if (fp != null) {
                if (form instanceof SimpleForm) {
                    fp.sendForm((SimpleForm) form);
                } else if (form instanceof CustomForm) {
                    fp.sendForm((CustomForm) form);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void executeDenyCommands(Player player) {
        runTask(player, () -> {
            List<String> denyCommands = TpaGui.getInstance().getConfig().getStringList("commands.deny");
            player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(TpaGui.getInstance(), true));
            try {
                for (String cmd : denyCommands) {
                    player.chat("/" + cmd);
                }
            } finally {
                player.removeMetadata("TPAGUI_COMMAND", TpaGui.getInstance());
            }
        });
    }

    private static void executeAcceptCommands(Player player, String requester) {
        runTask(player, () -> {
            List<String> acceptCommands = TpaGui.getInstance().getConfig().getStringList("commands.accept");
            player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(TpaGui.getInstance(), true));
            try {
                for (String cmd : acceptCommands) {
                    player.chat("/" + cmd + " " + requester);
                }
            } finally {
                player.removeMetadata("TPAGUI_COMMAND", TpaGui.getInstance());
            }
        });
    }

    /**
     * 在玩家所在线程执行任务（兼容Folia）
     * @param player 玩家
     * @param runnable 任务
     */
    private static void runTask(Player player, Runnable runnable) {
        if (TpaGui.getInstance().isFolia()) {
            player.getScheduler().run(TpaGui.getInstance(), (task) -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(TpaGui.getInstance(), runnable);
        }
    }

    public static void sendTpaRequestForm(Player target, String requester, boolean isTpaHere) {
        // 添加调试日志
        TpaGui.getInstance().getLogger().info(
            TpaGui.getInstance().getLogMessage("preparing-send-form",
                "{player}", target.getName())
        );
        
        try {
            String title = TpaGui.getInstance().getMessage("form.request.title");
            String content = TpaGui.getInstance().getMessage(
                isTpaHere ? "form.request.content-here" : "form.request.content-to",
                "{player}", requester
            );
            
            // 添加调试日志
            TpaGui.getInstance().getLogger().info(
                TpaGui.getInstance().getLogMessage("form-content",
                    "{title}", title,
                    "{content}", content)
            );
            
            SimpleForm form = SimpleForm.builder()
                .title(title)
                .content(content)
                .button(TpaGui.getInstance().getMessage("form.request.accept"))
                .button(TpaGui.getInstance().getMessage("form.request.deny"))
                .responseHandler((form1, response) -> {
                    if (response == null || response.trim().isEmpty()) {
                        // 玩家关闭表单，记录到控制台并发送消息
                        TpaGui.getInstance().getLogger().info(
                            TpaGui.getInstance().getLogMessage("request-form-closed",
                                "{player}", target.getName(),
                                "{requester}", requester)
                        );
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
                            TpaGui.getInstance().getLogger().info(
                                TpaGui.getInstance().getLogMessage("request-accepted",
                                    "{player}", target.getName(),
                                    "{requester}", requester)
                            );
                            
                            // 执行接受命令
                            executeAcceptCommands(target, requester);
                            
                            // 发送确认消息
                            target.sendMessage(TpaGui.getInstance().getMessage("form.request.accepted", "{player}", requester));
                        } else {
                            // 记录拒绝到控制台
                            TpaGui.getInstance().getLogger().info(
                                TpaGui.getInstance().getLogMessage("request-denied",
                                    "{player}", target.getName(),
                                    "{requester}", requester)
                            );
                            // 发送拒绝消息
                            target.sendMessage(TpaGui.getInstance().getMessage("form.request.denied", "{player}", requester));
                            // 执行拒绝命令
                            executeDenyCommands(target);
                        }
                    } catch (NumberFormatException e) {
                        // 记录错误，但不显示给玩家，因为可能是关闭表单导致的
                        TpaGui.getInstance().getLogger().fine(
                            TpaGui.getInstance().getLogMessage("form-response-parse",
                                "{error}", e.getMessage())
                        );
                        // 当作关闭表单处理
                        TpaGui.getInstance().getLogger().info(
                            TpaGui.getInstance().getLogMessage("request-form-closed",
                                "{player}", target.getName(),
                                "{requester}", requester)
                        );
                        target.sendMessage(TpaGui.getInstance().getMessage("form.request.closed", "{player}", requester));
                        executeDenyCommands(target);
                    }
                })
                .build();
            
            // 获取 FloodgatePlayer 并发送表单
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                if (api == null) {
                    TpaGui.getInstance().getLogger().warning(
                        TpaGui.getInstance().getLogMessage("floodgate-api-unavailable")
                    );
                    return;
                }
                
                FloodgatePlayer floodgatePlayer = api.getPlayer(target.getUniqueId());
                if (floodgatePlayer != null) {
                    floodgatePlayer.sendForm(form);
                    // 添加调试日志
                    TpaGui.getInstance().getLogger().info(
                        TpaGui.getInstance().getLogMessage("form-sent",
                            "{player}", target.getName())
                    );
                } else {
                    TpaGui.getInstance().getLogger().warning(
                        TpaGui.getInstance().getLogMessage("floodgate-player-error",
                            "{player}", target.getName())
                    );
                }
            } catch (Exception e) {
                TpaGui.getInstance().getLogger().severe(
                    TpaGui.getInstance().getLogMessage("floodgate-send-form-error",
                        "{error}", e.getMessage())
                );
                e.printStackTrace();
            }
        } catch (Exception e) {
            TpaGui.getInstance().getLogger().severe(
                TpaGui.getInstance().getLogMessage("floodgate-send-form-error",
                    "{error}", e.getMessage())
            );
            e.printStackTrace();
        }
    }
}