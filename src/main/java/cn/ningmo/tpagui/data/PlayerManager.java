package cn.ningmo.tpagui.data;

import cn.ningmo.tpagui.TpaGui;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Map<String, GlobalPlayer> globalPlayers = new ConcurrentHashMap<>();
    private static volatile long lastUpdate = 0;

    // 已发送 GetPlayers、等待 PlayerList 到达后刷新菜单的请求者
    private static final Set<String> pendingRequesters = ConcurrentHashMap.newKeySet();

    public static void updatePlayers(List<GlobalPlayer> players) {
        globalPlayers.clear();
        for (GlobalPlayer player : players) {
            globalPlayers.put(player.getName().toLowerCase(Locale.ROOT), player);
        }
        lastUpdate = System.currentTimeMillis();
    }

    public static Collection<GlobalPlayer> getGlobalPlayers() {
        return globalPlayers.values();
    }

    public static GlobalPlayer getGlobalPlayer(String name) {
        return globalPlayers.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 标记一个请求者正在等待 PlayerList 更新（TpaGuiCommand 在发送 GetPlayers 前调用）
     * @param playerName 请求者名称
     */
    public static void markPending(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        pendingRequesters.add(playerName);
    }

    /**
     * 取出并清除所有等待 PlayerList 更新的请求者
     * @return 等待中的请求者名称集合
     */
    public static Set<String> drainPending() {
        Set<String> pending = new HashSet<>(pendingRequesters);
        pendingRequesters.clear();
        return pending;
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
