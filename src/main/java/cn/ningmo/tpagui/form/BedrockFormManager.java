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
        int playersPerPage = Math.max(1, plugin.getConfig().getInt("java-dialog-gui.players-per-page", 20));
        boolean showAvatars = plugin.getConfig().getBoolean("java-dialog-gui.show-avatars", true);
        String avatarApi = plugin.getConfig().getString("java-dialog-gui.avatar-api", "https://mc-heads.net/avatar/{uuid}/64");

        // 获取在线玩家列表（排除自己）
        List<TargetInfo> availablePlayers = new ArrayList<>();
        
        if (plugin.getConfig().getBoolean("velocity.enabled", false)) {
            // Velocity 模式：获取全局玩家
            boolean showCrossServer = plugin.getConfig().getBoolean("velocity.show-cross-server-players", true);
            String selfServer = plugin.getConfig().getString("velocity.server-name", "");
            for (GlobalPlayer gp : PlayerManager.getGlobalPlayers()) {
                if (gp.getUuid().equals(player.getUniqueId())) continue;
                // 配置关闭跨服显示时，跳过其他服务器的玩家
                if (!showCrossServer && !gp.getServer().equals(selfServer)) continue;
                // 同服隐身玩家（SuperVanish 等）对无权限玩家隐藏
                Player localPlayer = Bukkit.getPlayer(gp.getUuid());
                if (localPlayer != null && !player.canSee(localPlayer)) continue;
                availablePlayers.add(new TargetInfo(gp.getName(), gp.getUuid(), gp.getServer()));
            }
        } else {
            // 普通模式：获取本地在线玩家（隐藏隐身玩家）
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player && player.canSee(p)) {
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
        // 页码越界时回退到最后一页（表单打开期间可能有玩家下线）
        if (page * playersPerPage >= totalPlayers) {
            page = Math.max(0, (totalPlayers - 1) / playersPerPage);
        }
        final int currentPage = page;
        int start = currentPage * playersPerPage;
        int end = Math.min(start + playersPerPage, totalPlayers);

        // 创建表单
        SimpleForm.Builder formBuilder = SimpleForm.builder()
            .title(plugin.getMessage("form.title") + (totalPages > 1 ? " (" + (currentPage + 1) + "/" + totalPages + ")" : ""));

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
        if (currentPage > 0) {
            formBuilder.button(plugin.getMessage("gui.navigation.previous-page"), FormImage.Type.PATH, "textures/ui/left_arrow_custom");
        }
        if (currentPage < totalPages - 1) {
            formBuilder.button(plugin.getMessage("gui.navigation.next-page"), FormImage.Type.PATH, "textures/ui/right_arrow_custom");
        }
        // 返回按钮（点击后执行配置的命令，如 /cd 返回主菜单）
        final boolean showBackButton = plugin.getConfig().getBoolean("back-button.enabled", false);
        if (showBackButton) {
            formBuilder.button(plugin.getMessage("gui.navigation.back"), FormImage.Type.PATH, "textures/ui/cancel");
        }

        formBuilder.responseHandler((form, response) -> {
            // Cumulus 回调不在服务器主线程触发，先切回玩家所在线程再访问 Bukkit API
            runTask(player, () -> {
                if (response == null) {
                    plugin.getLogger().fine(plugin.getLogMessage("form-closed", "{player}", player.getName()));
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
                        // 导航/返回按钮，按添加顺序计算索引
                        int navIndex = buttonId - playerCountOnPage;
                        boolean hasPrev = currentPage > 0;
                        boolean hasNext = currentPage < totalPages - 1;
                        int nextIndex = hasPrev ? 1 : 0;
                        int backIndex = (hasPrev ? 1 : 0) + (hasNext ? 1 : 0);
                        
                        if (hasPrev && navIndex == 0) {
                            // 上一页
                            openTpaForm(player, currentPage - 1);
                        } else if (hasNext && navIndex == nextIndex) {
                            // 下一页
                            openTpaForm(player, currentPage + 1);
                        } else if (showBackButton && navIndex == backIndex) {
                            // 返回按钮：以玩家身份执行配置的命令（基岩版独立命令）
                            String command = plugin.getConfig().getString("back-button.bedrock-command", "");
                            if (command != null && !command.trim().isEmpty()) {
                                executeAsPlayer(player, command.trim());
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(plugin.getLogMessage("form-response-error", "{error}", e.getMessage()));
                }
            });
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
                // Cumulus 回调不在服务器主线程触发，先切回玩家所在线程再访问 Bukkit API
                runTask(player, () -> {
                    if (response == null) return;
                    
                    int id;
                    try {
                        id = Integer.parseInt(response.trim());
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning(plugin.getLogMessage("form-response-error", "{error}", e.getMessage()));
                        return;
                    }
                    if (id == 0) {
                        // TPA: 传送到目标玩家
                        String cmdName = plugin.getConfig().getString("commands.tpa.to-player", "tpa");
                        executeAsPlayer(player, cmdName + " " + target.name);
                    } else if (id == 1) {
                        // TPAHERE: 请求目标玩家传送到自己
                        String cmdName = plugin.getConfig().getString("commands.tpa.here", "tpahere");
                        executeAsPlayer(player, cmdName + " " + target.name);
                    } else if (id == 2) {
                        // 返回主菜单
                        openTpaForm(player, 0);
                    }
                });
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

    /**
     * 以玩家身份执行命令（在玩家所在线程调度，兼容Folia）
     * @param player 玩家
     * @param command 不带斜杠的命令
     */
    private static void executeAsPlayer(Player player, String command) {
        // performCommand 不接受前导斜杠，统一剥离
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        runTask(player, () -> player.performCommand(cmd));
    }

    private static void executeDenyCommands(Player player, String requester) {
        for (String cmd : TpaGui.getInstance().getConfig().getStringList("commands.deny")) {
            executeAsPlayer(player, cmd + " " + requester);
        }
    }

    private static void executeAcceptCommands(Player player, String requester) {
        for (String cmd : TpaGui.getInstance().getConfig().getStringList("commands.accept")) {
            executeAsPlayer(player, cmd + " " + requester);
        }
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
        try {
            String title = TpaGui.getInstance().getMessage("form.request.title");
            String content = TpaGui.getInstance().getMessage(
                isTpaHere ? "form.request.content-here" : "form.request.content-to",
                "{player}", requester
            );
            
            SimpleForm form = SimpleForm.builder()
                .title(title)
                .content(content)
                .button(TpaGui.getInstance().getMessage("form.request.accept"))
                .button(TpaGui.getInstance().getMessage("form.request.deny"))
                .responseHandler((form1, response) -> {
                    // Cumulus 回调不在服务器主线程触发，先切回玩家所在线程再访问 Bukkit API
                    runTask(target, () -> {
                        if (response == null || response.trim().isEmpty()) {
                            // 玩家关闭表单：仅记录日志，不执行任何命令
                            TpaGui.getInstance().getLogger().info(
                                TpaGui.getInstance().getLogMessage("request-form-closed",
                                    "{player}", target.getName(),
                                    "{requester}", requester)
                            );
                            return;
                        }
                        
                        int buttonId;
                        try {
                            // 去除可能的空白字符
                            buttonId = Integer.parseInt(response.trim());
                        } catch (NumberFormatException e) {
                            // 无效响应：仅记录日志，不执行任何命令
                            TpaGui.getInstance().getLogger().fine(
                                TpaGui.getInstance().getLogMessage("form-response-parse",
                                    "{error}", e.getMessage())
                            );
                            return;
                        }
                        
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
                            executeDenyCommands(target, requester);
                        }
                    });
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
                    TpaGui.getInstance().getLogger().fine(
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
