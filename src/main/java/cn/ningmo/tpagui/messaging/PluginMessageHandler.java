package cn.ningmo.tpagui.messaging;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.data.GlobalPlayer;
import cn.ningmo.tpagui.data.PlayerManager;
import cn.ningmo.tpagui.form.JavaDialogManager;
import cn.ningmo.tpagui.menu.GuiManager;
import cn.ningmo.tpagui.menu.TpaMenuHolder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PluginMessageHandler implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("tpagui:main")) return;

        TpaGui plugin = TpaGui.getInstance();
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PlayerList")) {
            // 解析玩家列表，畸形数据记警告而不炸堆栈
            try {
                int count = in.readInt();
                List<GlobalPlayer> players = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String name = in.readUTF();
                    UUID uuid = UUID.fromString(in.readUTF());
                    String server = in.readUTF();
                    players.add(new GlobalPlayer(name, uuid, server));
                }
                PlayerManager.updatePlayers(players);
                // 玩家列表更新后，刷新等待中的 TPA 菜单（修复首次打开空列表）
                refreshPendingMenus();
            } catch (Exception e) {
                plugin.getLogger().warning("解析 PlayerList 插件消息失败: " + e.getMessage());
            }
        } else if (subChannel.equals("ShowRequest")) {
            String targetName = in.readUTF();
            String requesterName = in.readUTF();
            boolean isTpaHere = in.readBoolean();

            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                // 切到目标玩家所在线程再分发（兼容 Folia）
                runTask(target, () -> JavaDialogManager.dispatchTpaRequest(target, requesterName, isTpaHere));
            }
        }
    }

    /**
     * 刷新 pending 请求者当前打开的 TPA 菜单
     */
    private void refreshPendingMenus() {
        for (String name : PlayerManager.drainPending()) {
            Player p = Bukkit.getPlayer(name);
            if (p == null || !p.isOnline()) continue;
            runTask(p, () -> {
                InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof TpaMenuHolder) {
                    int page = ((TpaMenuHolder) holder).getPage();
                    p.openInventory(GuiManager.createTpaMenu(p, page));
                }
            });
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
}
