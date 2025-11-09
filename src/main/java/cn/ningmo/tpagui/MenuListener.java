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
        // 只处理GUI库存的点击，忽略玩家自己的库存点击
        // 检查点击的库存是否为顶部库存（GUI）
        if (event.getClickedInventory() == null || 
            !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        
        String titleTemplate = TpaGui.getInstance().getMessage("gui.title");
        int pagePlaceholderIndex = titleTemplate.indexOf("{page}");
        if (pagePlaceholderIndex == -1) {
            return;
        }
        String titlePrefix = titleTemplate.substring(0, pagePlaceholderIndex);

        if (!event.getView().getTitle().startsWith(titlePrefix)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String title = event.getView().getTitle();
        int currentPage = extractPageFromTitle(title);
        if (currentPage <= 0) {
            return;
        }

        // 将从标题中提取的页码（从1开始）转换为从0开始的页码
        int pageIndex = currentPage - 1;

        if (clicked.getType() == Material.ARROW) {
            ItemMeta itemMeta = clicked.getItemMeta();
            if (itemMeta == null || !itemMeta.hasDisplayName()) {
                return;
            }
            
            String itemName = itemMeta.getDisplayName();
            String nextPageName = TpaGui.getInstance().getMessage("gui.navigation.next-page");
            String prevPageName = TpaGui.getInstance().getMessage("gui.navigation.previous-page");

            if (itemName.equals(nextPageName)) {
                player.openInventory(GuiManager.createTpaMenu(player, pageIndex + 1));
            } else if (itemName.equals(prevPageName)) {
                if (pageIndex > 0) {
                    player.openInventory(GuiManager.createTpaMenu(player, pageIndex - 1));
                }
            }
        } else if (clicked.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !(meta instanceof SkullMeta)) {
                return;
            }

            SkullMeta skullMeta = (SkullMeta) meta;
            if (skullMeta.getOwningPlayer() == null) {
                return;
            }
            
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
            TpaGui.getInstance().getLogger().info(
                TpaGui.getInstance().getLogMessage("gui-command-executed", 
                    "{player}", player.getName(), 
                    "{command}", command)
            );
            
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
    
    /**
     * 从标题中提取页码
     * @param title GUI标题
     * @return 页码（1-based），如果无法提取则返回0
     */
    private int extractPageFromTitle(String title) {
        try {
            // 获取配置中的标题模板
            String titleTemplate = TpaGui.getInstance().getMessage("gui.title");
            
            // 找到占位符{page}在模板中的位置
            int placeholderIndex = titleTemplate.indexOf("{page}");
            if (placeholderIndex == -1) {
                return 0;
            }
            
            // 获取占位符前后的文本
            String prefix = titleTemplate.substring(0, placeholderIndex);
            String suffix = titleTemplate.substring(placeholderIndex + 6); // 6是"{page}"的长度
            
            // 从实际标题中提取页码
            if (title.startsWith(prefix) && title.endsWith(suffix)) {
                String pageStr = title.substring(prefix.length(), title.length() - suffix.length());
                int page = Integer.parseInt(pageStr.trim());
                return page > 0 ? page : 0;
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            String errorMsg = TpaGui.getInstance().getMessage("gui.error.extract-page-failed", title);
            TpaGui.getInstance().getLogger().warning(errorMsg);
        }
        return 0;
    }
}