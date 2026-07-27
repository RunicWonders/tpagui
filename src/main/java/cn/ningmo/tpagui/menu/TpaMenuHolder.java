package cn.ningmo.tpagui.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * TPA GUI 菜单的 InventoryHolder，携带当前页码（从 0 开始）。
 * 用于在不依赖标题文本的情况下识别菜单并获取页码。
 */
public class TpaMenuHolder implements InventoryHolder {
    private final int page;

    public TpaMenuHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        // 由 Bukkit 在 createInventory 时绑定，此处无需返回实例
        return null;
    }
}
