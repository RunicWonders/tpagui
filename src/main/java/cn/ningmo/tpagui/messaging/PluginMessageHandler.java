package cn.ningmo.tpagui.messaging;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.data.GlobalPlayer;
import cn.ningmo.tpagui.data.PlayerManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PluginMessageHandler implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("tpagui:main")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PlayerList")) {
            int count = in.readInt();
            List<GlobalPlayer> players = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                UUID uuid = UUID.fromString(in.readUTF());
                String server = in.readUTF();
                players.add(new GlobalPlayer(name, uuid, server));
            }
            PlayerManager.updatePlayers(players);
        } else if (subChannel.equals("ShowRequest")) {
            String targetName = in.readUTF();
            String requesterName = in.readUTF();
            boolean isTpaHere = in.readBoolean();
            
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                TpaGui plugin = TpaGui.getInstance();
                
                // 检查目标玩家是否为基岩版玩家
                if (plugin.isFloodgateEnabled()) {
                    try {
                        if (FloodgateApi.getInstance().isFloodgatePlayer(target.getUniqueId())) {
                            cn.ningmo.tpagui.form.BedrockFormManager.sendTpaRequestForm(
                                target, requesterName, isTpaHere
                            );
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                
                // 检查是否支持 Java 1.21.6+ /dialog
                if (plugin.isDialogSupported() && plugin.getConfig().getBoolean("java-dialog-gui.enabled", true)) {
                    cn.ningmo.tpagui.form.JavaDialogManager.sendTpaRequestDialog(target, requesterName, isTpaHere);
                }
            }
        }
    }
}
