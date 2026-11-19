/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.model;

import org.bukkit.Material;

/** Уровень стены: материал блоков, прочность и отображаемое имя. */
public class WallTier {

    private final int level;
    private final String name;
    private final Material material;
    private final int maxHp;

    public WallTier(int level, String name, Material material, int maxHp) {
        this.level = level;
        this.name = name;
        this.material = material;
        this.maxHp = maxHp;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getMaxHp() {
        return maxHp;
    }
}
