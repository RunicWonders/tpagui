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
        // 获取配置中的GUI标题模板，去掉占位符部分进行匹配
        String titleTemplate = TpaGui.getInstance().getMessage("gui.title");
        String titlePrefix = titleTemplate.split(" - ")[0]; // 获取标题前缀部分
        
        if (!event.getView().getTitle().startsWith(titlePrefix)) {
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
            // 从标题中提取页码，支持不同语言的标题格式
            int currentPage = extractPageFromTitle(title);
            if (currentPage > 0) {
                String previousPageText = TpaGui.getInstance().getMessage("gui.navigation.previous-page");
                String nextPageText = TpaGui.getInstance().getMessage("gui.navigation.next-page");
                
                if (meta.getDisplayName().equals(previousPageText)) {
                    player.openInventory(GuiManager.createTpaMenu(player, currentPage - 2)); // currentPage是1-based，需要转换为0-based
                } else if (meta.getDisplayName().equals(nextPageText)) {
                    player.openInventory(GuiManager.createTpaMenu(player, currentPage)); // currentPage已经是下一页的0-based索引
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
                return Integer.parseInt(pageStr.trim());
            }
        } catch (Exception e) {
            String errorMsg = TpaGui.getInstance().getMessage("gui.error.extract-page-failed", title);
            TpaGui.getInstance().getLogger().warning(errorMsg);
        }
        return 0;
    }
}