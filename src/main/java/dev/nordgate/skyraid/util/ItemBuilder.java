/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Компактный сборщик ItemStack для пунктов GUI. */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, amount));
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String legacyName) {
        meta.displayName(TextUtil.color(legacyName));
        return this;
    }

    public ItemBuilder lore(String... legacyLines) {
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(legacyLines.length);
        for (String line : legacyLines) {
            lore.add(TextUtil.color(line));
        }
        meta.lore(lore);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
