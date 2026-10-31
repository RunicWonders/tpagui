package cn.ningmo.tpagui;

import cn.ningmo.tpagui.form.SchedulerUtil;
import cn.ningmo.tpagui.listener.TpaRequestListener;
import cn.ningmo.tpagui.menu.GuiManager;
import cn.ningmo.tpagui.menu.JavaConfirmMenu.ConfirmMenuHolder;
import cn.ningmo.tpagui.menu.TpaMenuHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class MenuListener implements Listener {
    private boolean isMenu(InventoryHolder holder) {
        return holder instanceof TpaMenuHolder || holder instanceof ConfirmMenuHolder;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!isMenu(holder)) return;
        // Cancel bottom-inventory transfers too, before any slot or item checks.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        boolean left = event.getClick() == ClickType.LEFT;
        // Inventory transactions must finish before opening/closing menus or running commands.
        SchedulerUtil.runTask(player, () -> {
            if (player.getOpenInventory().getTopInventory() != top) return;
            if (holder instanceof ConfirmMenuHolder confirm) {
                if (slot != 11 && slot != 15) return;
                if (!confirm.getTargetUuid().equals(player.getUniqueId())) return;
                TpaRequestListener.PendingRequest request = confirm.getRequest();
                if (request.isExpired() || !TpaRequestListener.pendingRequests.remove(player.getUniqueId(), request)) {
                    TpaRequestListener.pendingRequests.remove(player.getUniqueId(), request);
                    player.closeInventory();
                    player.sendMessage(TpaGui.getInstance().getMessage("request-expired"));
                    return;
                }
                player.closeInventory();
                TpaGui plugin = TpaGui.getInstance();
                TpaRequestListener.executeConfiguredCommands(player,
                    slot == 11 ? "commands.accept" : "commands.deny", request.requesterName);
                player.sendMessage(plugin.getMessage(slot == 11 ? "form.request.accepted" : "form.request.denied",
                    "{player}", request.requesterName));
                return;
            }
            int page = ((TpaMenuHolder) holder).getPage();
            if (slot == 45 && item.getType() == Material.ARROW) {
                player.openInventory(GuiManager.createTpaMenu(player, Math.max(0, page - 1)));
            } else if (slot == 53 && item.getType() == Material.ARROW) {
                player.openInventory(GuiManager.createTpaMenu(player, page + 1));
            } else if (slot == 49 && TpaGui.getInstance().getConfig().getBoolean("back-button.enabled", false)) {
                player.closeInventory();
                TpaRequestListener.executeAsPlayer(player, TpaGui.getInstance().getConfig().getString("back-button.java-command", ""));
            } else if (slot < 45 && item.getItemMeta() instanceof SkullMeta skull) {
                String name = ((TpaMenuHolder) holder).getTarget(slot);
                if (name == null) name = skull.getOwningPlayer() == null ? null : skull.getOwningPlayer().getName();
                if (name == null) {
                    player.sendMessage(TpaGui.getInstance().getMessage("player-offline"));
                    return;
                }
                String command = TpaGui.getInstance().getConfig().getString(left ? "commands.tpa.to-player" : "commands.tpa.here",
                    left ? "tpa" : "tpahere");
                player.closeInventory();
                TpaRequestListener.executeAsPlayer(player, command + " " + name);
            }
        });
    }

    @EventHandler public void onInventoryDrag(InventoryDragEvent event) {
        if (isMenu(event.getView().getTopInventory().getHolder())) event.setCancelled(true);
    }

    @EventHandler public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ConfirmMenuHolder holder)
                || !(event.getPlayer() instanceof Player player)) return;
        SchedulerUtil.runTask(player, () -> {
            if (TpaRequestListener.pendingRequests.get(player.getUniqueId()) == holder.getRequest()
                    && !holder.getRequest().isExpired()
                    && !(player.getOpenInventory().getTopInventory().getHolder() instanceof ConfirmMenuHolder)
                    && holder.getRequest().markReopenHintSent())
                player.sendMessage(TpaGui.getInstance().getMessage("request-reopen-hint"));
        });
    }
}
