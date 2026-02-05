package cn.ningmo.tpagui.data;

import cn.ningmo.tpagui.TpaGui;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Map<String, GlobalPlayer> globalPlayers = new ConcurrentHashMap<>();
    private static long lastUpdate = 0;

    public static void updatePlayers(List<GlobalPlayer> players) {
        globalPlayers.clear();
        for (GlobalPlayer player : players) {
            globalPlayers.put(player.getName().toLowerCase(), player);
        }
        lastUpdate = System.currentTimeMillis();
    }

    public static Collection<GlobalPlayer> getGlobalPlayers() {
        return globalPlayers.values();
    }

    public static GlobalPlayer getGlobalPlayer(String name) {
        return globalPlayers.get(name.toLowerCase());
    }

    public static void requestUpdate(Player requester) {
        if (requester == null) return;
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetPlayers");
        
        requester.sendPluginMessage(TpaGui.getInstance(), "tpagui:main", out.toByteArray());
    }

    public static boolean shouldUpdate() {
        long interval = TpaGui.getInstance().getConfig().getLong("velocity.sync-interval", 0) * 1000;
        if (interval <= 0) return true; // Always update if interval is 0 (on demand)
        return System.currentTimeMillis() - lastUpdate > interval;
    }
}
