package cn.ningmo.tpagui.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VelocityFormManager {

    public static void openTpaForm(VelocityTpaGui plugin, Player player, int page) {
        int playersPerPage = plugin.getConfig().node("java-dialog-gui", "players-per-page").getInt(20);
        boolean showAvatars = plugin.getConfig().node("java-dialog-gui", "show-avatars").getBoolean(true);
        String avatarApi = plugin.getConfig().node("java-dialog-gui", "avatar-api").getString("https://mc-heads.net/avatar/{uuid}/64");

        List<Player> allPlayers = plugin.getServer().getAllPlayers().stream()
                .filter(p -> !p.equals(player))
                .collect(Collectors.toList());

        if (allPlayers.isEmpty()) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getLogMessage("no-players-online"), net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        int totalPages = (int) Math.ceil((double) allPlayers.size() / playersPerPage);
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int start = (page - 1) * playersPerPage;
        int end = Math.min(start + playersPerPage, allPlayers.size());

        SimpleForm.Builder form = SimpleForm.builder()
                .title(plugin.getLogMessage("form-title") + " (" + page + "/" + totalPages + ")");

        for (int i = start; i < end; i++) {
            Player target = allPlayers.get(i);
            String name = target.getUsername();
            UUID uuid = target.getUniqueId();
            
            if (showAvatars) {
                String imageUrl = avatarApi.replace("{uuid}", uuid.toString()).replace("{name}", name);
                form.button(name, FormImage.Type.URL, imageUrl);
            } else {
                form.button(name);
            }
        }

        // 分页按钮
        if (page > 1) {
            form.button(plugin.getLogMessage("gui.navigation.previous-page"), FormImage.Type.PATH, "textures/ui/left_arrow_hover");
        }
        if (page < totalPages) {
            form.button(plugin.getLogMessage("gui.navigation.next-page"), FormImage.Type.PATH, "textures/ui/right_arrow_hover");
        }

        final int currentPage = page;
        final int finalStart = start;
        final int finalEnd = end;

        form.validResultHandler(response -> {
            int buttonId = response.clickedButtonId();
            int playerCount = finalEnd - finalStart;

            if (buttonId < playerCount) {
                // 点击了玩家
                Player target = allPlayers.get(finalStart + buttonId);
                openActionSelectForm(plugin, player, target);
            } else {
                // 点击了分页按钮
                int navId = buttonId - playerCount;
                if (currentPage > 1 && navId == 0) {
                    // 上一页
                    openTpaForm(plugin, player, currentPage - 1);
                } else {
                    // 下一页 (可能是 navId 0 或 1)
                    openTpaForm(plugin, player, currentPage + 1);
                }
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private static void openActionSelectForm(VelocityTpaGui plugin, Player player, Player target) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title(target.getUsername())
                .content(plugin.getLogMessage("form.player-select"))
                .button("TPA (传送到他)", FormImage.Type.PATH, "textures/ui/multiplayer_glyph_color")
                .button("TPAHERE (拉他过来)", FormImage.Type.PATH, "textures/ui/world_glyph_color")
                .button("返回", FormImage.Type.PATH, "textures/ui/cancel");

        form.validResultHandler(response -> {
            int buttonId = response.clickedButtonId();
            String targetName = target.getUsername();
            
            if (buttonId == 0) {
                plugin.getServer().getCommandManager().executeAsync(player, "tpa " + targetName);
            } else if (buttonId == 1) {
                plugin.getServer().getCommandManager().executeAsync(player, "tpahere " + targetName);
            } else if (buttonId == 2) {
                openTpaForm(plugin, player, 1);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }
}
