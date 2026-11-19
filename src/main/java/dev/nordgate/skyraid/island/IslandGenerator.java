/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.island;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.WallTier;
import dev.nordgate.skyraid.storage.BaseStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Генерация и геометрия острова. Компоновка фиксированная и вычисляется
 * от центра платформы (x, y, z):
 * <pre>
 *   (x,   y+1, z)   — ядро базы (HEAVY_CORE)
 *   (x,   y+1, z-3) — хранилище (сундук, повёрнут к ядру)
 *   (x-1/x, y+1..y+2, z-1) — стена 2x2, опечатывающая хранилище
 * </pre>
 */
public class IslandGenerator {

    private final SkyRaidPlugin plugin;

    public IslandGenerator(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    public int sizeOf(BaseData base) {
        return plugin.config().sizeOf(base.getLevel());
    }

    public int halfOf(BaseData base) {
        return sizeOf(base) / 2;
    }

    /** Случайная свободная позиция для нового острова или null, если место не найдено. */
    public Location findFreeLocation(BaseStorage storage) {
        PluginConfig cfg = plugin.config();
        World world = Bukkit.getWorld(cfg.islandWorld());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return null;
        }
        int radius = cfg.islandSearchRadius();
        long minDistanceSquared = (long) cfg.islandMinDistance() * cfg.islandMinDistance();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < cfg.islandMaxAttempts(); attempt++) {
            int x = random.nextInt(-radius, radius + 1);
            int z = random.nextInt(-radius, radius + 1);
            boolean free = true;
            for (BaseData other : storage.all()) {
                if (!other.getWorldName().equals(world.getName())) {
                    continue;
                }
                long dx = (long) other.getX() - x;
                long dz = (long) other.getZ() - z;
                if (dx * dx + dz * dz < minDistanceSquared) {
                    free = false;
                    break;
                }
            }
            if (free) {
                return new Location(world, x, cfg.islandHeight(), z);
            }
        }
        return null;
    }

    /**
     * Достраивает платформу острова до текущего уровня. Заполняются только
     * воздушные блоки, поэтому постройки владельца не затираются.
     */
    public void generate(BaseData base) {
        World world = Bukkit.getWorld(base.getWorldName());
        if (world == null) {
            return;
        }
        PluginConfig cfg = plugin.config();
        int half = halfOf(base);
        int cx = base.getX();
        int cy = base.getY();
        int cz = base.getZ();

        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                setIfAir(world, cx + dx, cy, cz + dz, cfg.islandTop());
                setIfAir(world, cx + dx, cy - 1, cz + dz, cfg.islandFill());
                setIfAir(world, cx + dx, cy - 2, cz + dz, cfg.islandFill());
                if (Math.abs(dx) <= half - 1 && Math.abs(dz) <= half - 1) {
                    setIfAir(world, cx + dx, cy - 3, cz + dz, cfg.islandFill());
                }
            }
        }

        Material light = cfg.islandLight();
        if (light != null) {
            for (int sx : new int[]{-half, half}) {
                for (int sz : new int[]{-half, half}) {
                    world.getBlockAt(cx + sx, cy, cz + sz).setType(light, false);
                }
            }
        }
    }

    /** Ставит ядро, хранилище и стену. Вызывается один раз при создании базы. */
    public void placeCoreAndVault(BaseData base) {
        World world = Bukkit.getWorld(base.getWorldName());
        if (world == null) {
            return;
        }
        world.getBlockAt(base.getX(), base.getY() + 1, base.getZ())
                .setType(plugin.config().islandCore(), false);

        Block chestBlock = world.getBlockAt(base.getX(), base.getY() + 1, base.getZ() - 3);
        chestBlock.setType(Material.CHEST, false);
        if (chestBlock.getState() instanceof Chest chest) {
            chest.setFacing(BlockFace.SOUTH);
            chest.update(true, false);
        }
        replaceWall(base);
    }

    /** Переставляет блоки стены в материал текущего уровня. */
    public void replaceWall(BaseData base) {
        World world = Bukkit.getWorld(base.getWorldName());
        if (world == null) {
            return;
        }
        WallTier tier = plugin.config().wallTier(base.getWallLevel());
        Material material = tier != null ? tier.getMaterial() : Material.IRON_BLOCK;
        for (Location location : wallBlocks(base)) {
            world.getBlockAt(location).setType(material, false);
        }
    }

    /** Четыре блока стены 2x2 между ядром и хранилищем. */
    public List<Location> wallBlocks(BaseData base) {
        World world = Bukkit.getWorld(base.getWorldName());
        int cx = base.getX();
        int cy = base.getY();
        int cz = base.getZ();
        return List.of(
                new Location(world, cx - 1, cy + 1, cz - 1),
                new Location(world, cx, cy + 1, cz - 1),
                new Location(world, cx - 1, cy + 2, cz - 1),
                new Location(world, cx, cy + 2, cz - 1));
    }

    public Location coreLocation(BaseData base) {
        return new Location(Bukkit.getWorld(base.getWorldName()), base.getX(), base.getY() + 1, base.getZ());
    }

    public Location vaultLocation(BaseData base) {
        return new Location(Bukkit.getWorld(base.getWorldName()), base.getX(), base.getY() + 1, base.getZ() - 3);
    }

    /** Точка появления владельца: рядом с ядром, лицом к нему. */
    public Location homeSpawn(BaseData base) {
        return new Location(Bukkit.getWorld(base.getWorldName()),
                base.getX() + 0.5, base.getY() + 1, base.getZ() + 1.5, 180f, 10f);
    }

    /** Точка появления рейдера: южнее ядра, лицом к острову. */
    public Location raidSpawn(BaseData base) {
        return new Location(Bukkit.getWorld(base.getWorldName()),
                base.getX() + 0.5, base.getY() + 1, base.getZ() + 3.5, 180f, 5f);
    }

    private void setIfAir(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType().isAir()) {
            block.setType(material, false);
        }
    }
}
