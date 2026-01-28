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
        
        // 检查是否为基岩版玩家 (如果 Floodgate 在 Velocity 上)
        try {
            if (isFloodgatePlayer(player)) {
                // TODO: 实现 Velocity 端的 Floodgate 表单
                // 目前先回退到文本列表
            }
        } catch (NoClassDefFoundError ignored) {}

        // 发送在线玩家列表
        Collection<Player> allPlayers = server.getAllPlayers();
        if (allPlayers.size() <= 1) {
            player.sendMessage(Component.text(plugin.getLogMessage("no-players-online"), NamedTextColor.RED));
            return;
        }

        TextComponent.Builder builder = Component.text()
            .append(Component.text("--- ", NamedTextColor.GRAY))
            .append(Component.text("传送请求菜单 (Proxy)", NamedTextColor.AQUA))
            .append(Component.text(" ---", NamedTextColor.GRAY))
            .append(Component.newline());

        for (Player p : allPlayers) {
            if (p.equals(player)) continue;

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
