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
        
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        // 处理翻页
        if (clicked.getType() == Material.ARROW) {
            int currentPage = Integer.parseInt(event.getView().getTitle().split(" ")[3]) - 1;
            if (clicked.getItemMeta().getDisplayName().contains("上一页")) {
                player.openInventory(GuiManager.createTpaMenu(player, currentPage - 1));
            } else if (clicked.getItemMeta().getDisplayName().contains("下一页")) {
                player.openInventory(GuiManager.createTpaMenu(player, currentPage + 1));
            }
            return;
        }
        
        // 处理玩家头颅点击
        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            Player target = meta.getOwningPlayer().getPlayer();
            
            if (target == null || !target.isOnline()) {
                player.sendMessage(TpaGui.getInstance().getMessage("player-offline"));
                return;
            }
            
            // 构建命令
            String command = event.isLeftClick() ? 
                "/tpa " + target.getName() : 
                "/tpahere " + target.getName();
            
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