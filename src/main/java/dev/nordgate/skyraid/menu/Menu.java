/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.menu;

import dev.nordgate.skyraid.util.ItemBuilder;
import dev.nordgate.skyraid.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Базовый класс GUI: инвентарь сам является holder'ом, поэтому клики
 * маршрутизируются в Menu#onClick без костылей с заголовками.
 */
public abstract class Menu implements InventoryHolder {

    protected final Inventory inventory;

    protected Menu(String title, int size) {
        this.inventory = Bukkit.createInventory(this, size, TextUtil.color(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Клик по верхнему инвентарю; событие уже отменено. */
    public abstract void onClick(Player player, int slot);

    public void open(Player player) {
        player.openInventory(inventory);
    }

    protected void fill(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected static ItemStack pane() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    }
}
