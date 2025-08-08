package cn.ningmo.tpagui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;
import cn.ningmo.tpagui.menu.GuiManager;
import org.bukkit.metadata.FixedMetadataValue;

public class MenuListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("传送请求菜单")) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // 处理翻页
        if (clicked.getType() == Material.ARROW) {
            String title = event.getView().getTitle();
            String[] titleParts = title.split(" ");
            if (titleParts.length >= 4) {
                int currentPage = Integer.parseInt(titleParts[3]) - 1;
                if (meta.getDisplayName().contains("上一页")) {
                    player.openInventory(GuiManager.createTpaMenu(player, currentPage - 1));
                } else if (meta.getDisplayName().contains("下一页")) {
                    player.openInventory(GuiManager.createTpaMenu(player, currentPage + 1));
                }
            }
            return;
        }
        
        // 处理玩家头颅点击
        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) meta;
            if (skullMeta.getOwningPlayer() != null) {
                Player target = skullMeta.getOwningPlayer().getPlayer();
                
                if (target == null || !target.isOnline()) {
                    player.sendMessage(TpaGui.getInstance().getMessage("player-offline"));
                    return;
                }
                
                // 从配置文件获取命令
                String tpaCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.to-player", "tpa");
                String tpaHereCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.here", "tpahere");
                
                // 构建命令
                String command = event.isLeftClick() ? 
                    "/" + tpaCommand + " " + target.getName() : 
                    "/" + tpaHereCommand + " " + target.getName();
                
                // 记录到控制台
                TpaGui.getInstance().getLogger().info(player.getName() + " 通过GUI执行命令: " + command);
                
                // 执行命令
                player.setMetadata("TPAGUI_COMMAND", new FixedMetadataValue(TpaGui.getInstance(), true));
                try {
                    player.chat(command);
                } finally {
                    player.removeMetadata("TPAGUI_COMMAND", TpaGui.getInstance());
                }
                
                player.closeInventory();
            }
        }
    }
}