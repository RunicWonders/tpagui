package cn.ningmo.tpagui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;
import cn.ningmo.tpagui.menu.GuiManager;
import cn.ningmo.tpagui.menu.TpaMenuHolder;

public class MenuListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 通过自定义 InventoryHolder 识别 TPA GUI，忽略其他界面
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof TpaMenuHolder)) {
            return;
        }

        // 只处理GUI库存的点击，忽略玩家自己的库存点击
        if (event.getClickedInventory() == null || 
            !event.getClickedInventory().equals(topInventory)) {
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

        // 页码直接从 holder 中读取（从 0 开始）
        int pageIndex = ((TpaMenuHolder) topInventory.getHolder()).getPage();

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
        } else if (isBackButton(clicked)) {
            // 返回按钮：以玩家身份执行配置的命令（如 /cd 返回主菜单）
            String command = TpaGui.getInstance().getConfig().getString("back-button.java-command", "");
            if (command != null && !command.trim().isEmpty()) {
                player.performCommand(command.trim().startsWith("/")
                    ? command.trim().substring(1) : command.trim());
            }
            player.closeInventory();
        } else if (clicked.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !(meta instanceof SkullMeta)) {
                return;
            }

            SkullMeta skullMeta = (SkullMeta) meta;
            String targetName = null;
            
            if (skullMeta.getOwningPlayer() != null) {
                targetName = skullMeta.getOwningPlayer().getName();
            } else if (skullMeta.hasDisplayName()) {
                // 回退逻辑：尝试从显示名称解析 (去除颜色代码和前缀)
                String displayName = ChatColor.stripColor(skullMeta.getDisplayName());
                String namePrefix = ChatColor.stripColor(TpaGui.getInstance().getMessage("gui.skull.name", "{player}", ""));
                targetName = displayName.replace(namePrefix, "").trim();
            }
            
            if (targetName == null || targetName.isEmpty()) {
                player.sendMessage(TpaGui.getInstance().getMessage("player-offline"));
                return;
            }
            
            // 从配置文件获取命令
            String tpaCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.to-player", "tpa");
            String tpaHereCommand = TpaGui.getInstance().getConfig().getString("commands.tpa.here", "tpahere");
            
            // 构建命令
            String command = event.isLeftClick() ? 
                tpaCommand + " " + targetName : 
                tpaHereCommand + " " + targetName;
            
            // 记录到控制台
            TpaGui.getInstance().getLogger().info(
                TpaGui.getInstance().getLogMessage("gui-command-executed", 
                    "{player}", player.getName(), 
                    "{command}", "/" + command)
            );
            
            // 执行命令（performCommand 替代已废弃的 player.chat）
            player.performCommand(command);
            player.closeInventory();
        }
    }
    
    /**
     * 判断点击的物品是否为返回按钮（材质与显示名均需与配置匹配）
     */
    private boolean isBackButton(ItemStack item) {
        TpaGui plugin = TpaGui.getInstance();
        if (!plugin.getConfig().getBoolean("back-button.enabled", false)) {
            return false;
        }
        Material material = Material.matchMaterial(
            plugin.getConfig().getString("back-button.material", "BARRIER"));
        if (material == null) {
            material = Material.BARRIER;
        }
        if (item.getType() != material) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
            && meta.getDisplayName().equals(plugin.getMessage("gui.navigation.back"));
    }

    /**
     * 取消对 TPA GUI 的拖拽操作，防止物品被拖入菜单导致丢失
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TpaMenuHolder) {
            event.setCancelled(true);
        }
    }
}