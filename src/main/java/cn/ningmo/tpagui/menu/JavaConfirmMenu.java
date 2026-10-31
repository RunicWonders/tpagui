package cn.ningmo.tpagui.menu;

import cn.ningmo.tpagui.TpaGui;
import cn.ningmo.tpagui.listener.TpaRequestListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.UUID;

public final class JavaConfirmMenu {
    public static void open(Player target, TpaRequestListener.PendingRequest request) {
        TpaGui plugin = TpaGui.getInstance();
        ConfirmMenuHolder holder = new ConfirmMenuHolder(target.getUniqueId(), request);
        holder.inventory = Bukkit.createInventory(holder, 27, plugin.getMessage("confirm.title"));
        ItemStack info = item(Material.PLAYER_HEAD,
            plugin.getMessage("confirm.skull-name", "{player}", request.requesterName));
        ItemMeta meta = info.getItemMeta();
        meta.setLore(List.of(plugin.getMessage(request.isTpaHere ? "confirm.content-here" : "confirm.content-to")));
        info.setItemMeta(meta);
        holder.inventory.setItem(4, info);
        holder.inventory.setItem(11, item(Material.LIME_STAINED_GLASS_PANE, plugin.getMessage("confirm.accept")));
        holder.inventory.setItem(15, item(Material.RED_STAINED_GLASS_PANE, plugin.getMessage("confirm.deny")));
        target.openInventory(holder.inventory);
    }

    private static ItemStack item(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static final class ConfirmMenuHolder implements InventoryHolder {
        private final UUID target;
        private final TpaRequestListener.PendingRequest request;
        private Inventory inventory;
        public ConfirmMenuHolder(UUID target, TpaRequestListener.PendingRequest request) {
            this.target = target;
            this.request = request;
        }
        public UUID getTargetUuid() { return target; }
        public TpaRequestListener.PendingRequest getRequest() { return request; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
