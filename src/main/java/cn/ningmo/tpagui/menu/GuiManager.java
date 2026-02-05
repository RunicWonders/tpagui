package cn.ningmo.tpagui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.ChatColor;
import cn.ningmo.tpagui.TpaGui;

import java.util.List;
import java.util.ArrayList;

public class GuiManager {
    private static final int ROWS = 6;
    
    public static Inventory createTpaMenu(Player player, int page) {
        TpaGui plugin = TpaGui.getInstance();
        int playersPerPage = plugin.getConfig().getInt("java-dialog-gui.players-per-page", 45);
        if (playersPerPage > 45) playersPerPage = 45; // 最多 5 行

        Inventory inv = Bukkit.createInventory(null, ROWS * 9, 
            plugin.getMessage("gui.title", "{page}", String.valueOf(page + 1)));
        
        // 获取所有在线玩家（排除当前玩家）
        List<Player> availablePlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != player) {
                availablePlayers.add(p);
            }
        }
        
        // 计算总页数
        int totalPlayers = availablePlayers.size();
        int totalPages = totalPlayers > 0 ? ((totalPlayers - 1) / playersPerPage + 1) : 1;
        
        // 添加玩家头颅
        int startIndex = page * playersPerPage;
        int slotIndex = 0;
        for (int i = startIndex; i < availablePlayers.size() && slotIndex < playersPerPage; i++) {
            Player target = availablePlayers.get(i);
            // 再次验证玩家是否在线（可能在获取列表后离线）
            if (target != null && target.isOnline()) {
                ItemStack skull = createPlayerSkull(target);
                // 只添加有效的头颅（不是空的ItemStack）
                if (skull != null && skull.getItemMeta() != null) {
                    inv.setItem(slotIndex, skull);
                    slotIndex++;
                }
            }
        }
        
        // 在底部（第6行）添加翻页按钮
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, 
                plugin.getMessage("gui.navigation.previous-page")));
        }
        if (totalPlayers > 0 && page < totalPages - 1) {
            inv.setItem(53, createNavigationItem(Material.ARROW, 
                plugin.getMessage("gui.navigation.next-page")));
        }
        
        return inv;
    }
    
    private static ItemStack createPlayerSkull(Player player) {
        TpaGui plugin = TpaGui.getInstance();
        
        // 验证玩家有效性
        if (player == null || !player.isOnline()) {
            plugin.getLogger().fine(
                plugin.getLogMessage("skull-create-offline", 
                    "{player}", player != null ? player.getName() : "null")
            );
            return null;
        }
        
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning(plugin.getLogMessage("skull-itemmeta-error"));
            return null;
        }
        
        try {
            // 设置玩家头颅的所有者
            meta.setOwningPlayer(player);
        } catch (Exception e) {
            plugin.getLogger().warning(
                plugin.getLogMessage("skull-owner-error", 
                    "{player}", player.getName(), 
                    "{error}", e.getMessage())
            );
            return null;
        }
        
        // 设置显示名称和描述
        meta.setDisplayName(plugin.getMessage("gui.skull.name", "{player}", player.getName()));
        
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("messages.gui.skull.lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        
        skull.setItemMeta(meta);
        return skull;
    }
    
    private static ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
} 