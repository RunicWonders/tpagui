package cn.ningmo.tpagui.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VelocityTpaGuiCommand implements SimpleCommand {

    private final VelocityTpaGui plugin;
    private final ProxyServer server;

    public VelocityTpaGuiCommand(VelocityTpaGui plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Online players: ")
                .append(Component.text(server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .collect(Collectors.joining(", ")), NamedTextColor.GREEN)));
            return;
        }

        Player player = (Player) invocation.source();
        
        // 解析页码
        int page = 1;
        String[] args = invocation.arguments();
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        // 检查是否为基岩版玩家 (如果 Floodgate 在 Velocity 上)
        try {
            if (isFloodgatePlayer(player)) {
                VelocityFormManager.openTpaForm(plugin, player, page);
                return;
            }
        } catch (NoClassDefFoundError ignored) {
        } catch (Exception e) {
            player.sendMessage(Component.text(plugin.getLogMessage("form-error"), NamedTextColor.RED));
        }

        // 发送在线玩家列表 (分页文本)
        List<Player> allPlayers = server.getAllPlayers().stream()
                .filter(p -> !p.equals(player))
                .collect(Collectors.toList());

        if (allPlayers.isEmpty()) {
            player.sendMessage(Component.text(plugin.getLogMessage("no-players-online"), NamedTextColor.RED));
            return;
        }

        int playersPerPage = plugin.getConfig().node("java-dialog-gui", "players-per-page").getInt(20);
        int totalPages = (int) Math.ceil((double) allPlayers.size() / playersPerPage);
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int start = (page - 1) * playersPerPage;
        int end = Math.min(start + playersPerPage, allPlayers.size());

        TextComponent.Builder builder = Component.text()
            .append(Component.text("--- ", NamedTextColor.GRAY))
            .append(Component.text(plugin.getLogMessage("form-title") + " (Proxy) [" + page + "/" + totalPages + "]", NamedTextColor.AQUA))
            .append(Component.text(" ---", NamedTextColor.GRAY))
            .append(Component.newline());

        for (int i = start; i < end; i++) {
            Player p = allPlayers.get(i);
            builder.append(Component.text("- ", NamedTextColor.GRAY))
                .append(Component.text(p.getUsername(), NamedTextColor.GOLD))
                .append(Component.text(" [", NamedTextColor.GRAY))
                .append(Component.text("TPA", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/tpa " + p.getUsername())))
                .append(Component.text("] [", NamedTextColor.GRAY))
                .append(Component.text("TPAHERE", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/tpahere " + p.getUsername())))
                .append(Component.text("]", NamedTextColor.GRAY))
                .append(Component.newline());
        }

        // 分页导航
        if (totalPages > 1) {
            builder.append(Component.newline());
            if (page > 1) {
                builder.append(Component.text("[◀ 上一页]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/tpagui " + (page - 1))))
                    .append(Component.text("  ", NamedTextColor.GRAY));
            }
            builder.append(Component.text("第 " + page + "/" + totalPages + " 页", NamedTextColor.WHITE));
            if (page < totalPages) {
                builder.append(Component.text("  ", NamedTextColor.GRAY))
                    .append(Component.text("[下一页 ▶]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/tpagui " + (page + 1))));
            }
        }

        player.sendMessage(builder.build());
    }

    private boolean isFloodgatePlayer(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable e) {
            return false;
        }
    }
}
