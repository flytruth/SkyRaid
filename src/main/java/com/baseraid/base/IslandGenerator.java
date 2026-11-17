package com.baseraid.base;

import com.baseraid.Settings;
import org.bukkit.Material;
import org.bukkit.World;

/** Генерация круглого небесного острова под базу. */
public final class IslandGenerator {

    private IslandGenerator() {
    }

    /** Создание острова 1 уровня: платформа + пьедестал ядра. */
    public static void generate(Base b, Settings s, World w) {
        int r = b.radius(s);
        fillCircle(b, s, w, r, false);
        // Чистим пространство над островом
        clearAbove(b, s, w, r);
        // Пьедестал ядра в центре
        w.getBlockAt(b.x, b.y + 1, b.z).setType(s.pedestal, false);
    }

    /** Расширение острова при улучшении (не трогает постройки игрока). */
    public static void expand(Base b, Settings s, World w) {
        int r = b.radius(s);
        fillCircle(b, s, w, r, true);
        clearAbove(b, s, w, r);
        // Пьедестал мог "съесться" — восстанавливаем на всякий случай
        if (w.getBlockAt(b.x, b.y + 1, b.z).getType().isAir()) {
            w.getBlockAt(b.x, b.y + 1, b.z).setType(s.pedestal, false);
        }
    }

    /** Удаление острова (только блоки платформы и пьедестал). */
    public static void clear(Base b, Settings s, World w) {
        int r = b.radius(s) + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int i = 0; i <= 1; i++) {
                    org.bukkit.block.Block bl = w.getBlockAt(b.x + dx, b.y - i, b.z + dz);
                    Material expected = i == 0 ? s.islandTop : s.islandFiller;
                    if (bl.getType() == expected) bl.setType(Material.AIR, false);
                }
                for (int i = 1; i <= s.fillerLayers; i++) {
                    org.bukkit.block.Block bl = w.getBlockAt(b.x + dx, b.y - i, b.z + dz);
                    if (bl.getType() == s.islandFiller) bl.setType(Material.AIR, false);
                }
                if (dx == 0 && dz == 0) {
                    org.bukkit.block.Block bl = w.getBlockAt(b.x, b.y + 1, b.z);
                    if (bl.getType() == s.pedestal) bl.setType(Material.AIR, false);
                }
            }
        }
    }

    private static void fillCircle(Base b, Settings s, World w, int r, boolean onlyAir) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                org.bukkit.block.Block top = w.getBlockAt(b.x + dx, b.y, b.z + dz);
                if (!onlyAir || top.getType().isAir()) top.setType(s.islandTop, false);
                for (int i = 1; i <= s.fillerLayers; i++) {
                    org.bukkit.block.Block f = w.getBlockAt(b.x + dx, b.y - i, b.z + dz);
                    if (!onlyAir || f.getType().isAir()) f.setType(s.islandFiller, false);
                }
            }
        }
    }

    private static void clearAbove(Base b, Settings s, World w, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int i = 1; i <= 3; i++) {
                    org.bukkit.block.Block bl = w.getBlockAt(b.x + dx, b.y + i, b.z + dz);
                    if (!bl.getType().isAir()) bl.setType(Material.AIR, false);
                }
            }
        }
    }
}
