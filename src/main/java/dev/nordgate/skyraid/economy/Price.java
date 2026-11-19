/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.economy;

import dev.nordgate.skyraid.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Цена в виде "материал + количество". Парсится из строк конфига
 * формата "DIAMOND:16".
 */
public final class Price {

    private final Material material;
    private final int amount;

    private Price(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public static Price parse(String raw) {
        String[] parts = raw.trim().split(":");
        Material material = Material.matchMaterial(parts[0].trim());
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Некорректный предмет цены: " + raw);
        }
        int amount = 1;
        if (parts.length > 1) {
            amount = Integer.parseInt(parts[1].trim());
        }
        return new Price(material, Math.max(1, amount));
    }

    /** Разбирает список цен; битые записи пропускаются с предупреждением в лог. */
    public static List<Price> parseList(List<String> raw) {
        List<Price> prices = new ArrayList<>(raw.size());
        for (String entry : raw) {
            try {
                prices.add(parse(entry));
            } catch (RuntimeException ex) {
                Bukkit.getLogger().warning("[SkyRaid] " + ex.getMessage());
            }
        }
        return prices;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String describe() {
        return amount + "x " + Items.pretty(material);
    }
}
