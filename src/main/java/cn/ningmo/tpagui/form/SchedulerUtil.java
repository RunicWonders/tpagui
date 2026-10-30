package cn.ningmo.tpagui.form;

import cn.ningmo.tpagui.TpaGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SchedulerUtil {
    private SchedulerUtil() {}
    public static void runTask(Player player, Runnable action) {
        TpaGui plugin = TpaGui.getInstance();
        Runnable guarded = () -> {
            if (player.isOnline() && plugin.isEnabled()) action.run();
        };
        if (plugin.isFolia()) player.getScheduler().run(plugin, task -> guarded.run(), null);
        else Bukkit.getScheduler().runTask(plugin, guarded);
    }
}
