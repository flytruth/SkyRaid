/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.util;

import org.bukkit.Material;

import java.util.Locale;

/** Мелкие утилиты над предметами. */
public final class Items {

    private Items() {
    }

    /** Человекочитаемое имя материала: "netherite ingot". */
    public static String pretty(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
