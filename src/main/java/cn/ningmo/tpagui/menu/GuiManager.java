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

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class GuiManager {
    private static final int ROWS = 6;
    private static final int SLOTS_PER_PAGE = 45; // 前5行用于显示玩家头颅
    
    public static Inventory createTpaMenu(Player player, int page) {
        TpaGui plugin = TpaGui.getInstance();
        Inventory inv = Bukkit.createInventory(null, ROWS * 9, 
            plugin.getMessage("gui.title", "{page}", String.valueOf(page + 1)));
        
        // 获取所有在线玩家
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        
        // 计算总页数
        int totalPages = (players.length - 1) / SLOTS_PER_PAGE + 1;
        
        // 添加玩家头颅
        int startIndex = page * SLOTS_PER_PAGE;
        for (int i = 0; i < SLOTS_PER_PAGE && startIndex + i < players.length; i++) {
            Player target = players[startIndex + i];
            if (target != player) {
                ItemStack skull = createPlayerSkull(target);
                inv.setItem(i, skull);
            }
        }
        
        // 添加翻页按钮
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, 
                plugin.getMessage("gui.navigation.previous-page")));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createNavigationItem(Material.ARROW, 
                plugin.getMessage("gui.navigation.next-page")));
        }
        
        return inv;
    }
    
    private static ItemStack createPlayerSkull(Player player) {
        TpaGui plugin = TpaGui.getInstance();
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
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
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
} 