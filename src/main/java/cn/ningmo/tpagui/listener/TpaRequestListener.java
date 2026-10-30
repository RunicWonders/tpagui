package cn.ningmo.tpagui.listener;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.form.JavaDialogManager;
import cn.ningmo.tpagui.form.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaRequestListener implements Listener {
    public static final Map<UUID, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public static final class PendingRequest {
        public final String requesterName;
        public final boolean isTpaHere;
        private final long timestamp = System.currentTimeMillis();
        public PendingRequest(String name, boolean here) { requesterName = name; isTpaHere = here; }
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp >= Math.max(1,
                TpaGui.getInstance().getConfig().getInt("request-expire-seconds", 300)) * 1000L;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        observeCommand(event.getPlayer(), event.getMessage());
    }

    // performCommand bypasses PlayerCommandPreprocessEvent; emit it once for GUI commands.
    public static void executeAsPlayer(Player player, String command) {
        String normalized = command.trim().replaceFirst("^/", "");
        if (normalized.isEmpty()) return;
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/" + normalized);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) player.performCommand(event.getMessage().replaceFirst("^/", ""));
    }

    private static String label(String command) {
        return command.trim().replaceFirst("^/", "").split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    private static void observeCommand(Player sender, String command) {
        TpaGui plugin = TpaGui.getInstance();
        String[] args = command.trim().replaceFirst("^/", "").split("\\s+");
        String cmd = label(command);
        boolean response = java.util.stream.Stream.concat(
            plugin.getConfig().getStringList("commands.accept").stream(),
            plugin.getConfig().getStringList("commands.deny").stream()).anyMatch(c -> label(c).equals(cmd));
        if (response) {
            PendingRequest pending = pendingRequests.get(sender.getUniqueId());
            if (pending != null && (args.length == 1 || pending.requesterName.equalsIgnoreCase(args[1])))
                pendingRequests.remove(sender.getUniqueId(), pending);
            return;
        }
        if (args.length != 2 || plugin.getConfig().getStringList("commands.listen-commands").stream()
                .noneMatch(c -> label(c).equals(cmd))) return;
        boolean here = cmd.equals(label(plugin.getConfig().getString("commands.tpa.here", "tpahere")));
        String requester = sender.getName();
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null && !target.getUniqueId().equals(sender.getUniqueId())) {
            SchedulerUtil.runTask(target, () -> JavaDialogManager.dispatchTpaRequest(target, requester, here));
        } else if (target == null && plugin.getConfig().getBoolean("velocity.enabled", false)
                && cn.ningmo.tpagui.data.PlayerManager.getGlobalPlayer(args[1]) != null) {
            com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
            out.writeUTF("ShowRequest");
            out.writeUTF(args[1]);
            out.writeUTF(requester);
            out.writeBoolean(here);
            sender.sendPluginMessage(plugin, "tpagui:main", out.toByteArray());
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        String name = event.getPlayer().getName();
        pendingRequests.remove(event.getPlayer().getUniqueId());
        pendingRequests.entrySet().removeIf(e -> e.getValue().requesterName.equalsIgnoreCase(name));
    }
}
