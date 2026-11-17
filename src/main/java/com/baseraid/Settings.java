package com.baseraid;

import com.baseraid.util.Cost;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Загруженный и разобранный config.yml. */
public final class Settings {

    private final BaseRaidPlugin plugin;

    public String worldName;
    public int baseY;
    public int gridGap;

    public String spawnWorld;
    public double spawnX, spawnY, spawnZ;
    public float spawnYaw, spawnPitch;

    public int regionAbove, regionBelow;

    public int initialFuelHours;
    public Cost purchaseCost;

    public Material coreItem;
    public Material pedestal;

    public Material islandTop, islandFiller;
    public int fillerLayers;
    public final TreeMap<Integer, LevelSpec> islandLevels = new TreeMap<>();

    public int wallHeight, wallThickness, wallInset;
    public final TreeMap<Integer, WallSpec> wallLevels = new TreeMap<>();

    public Material fuelItem;
    public int fuelHoursPerItem, fuelMaxHours;

    public int raidDurationMin, raidCooldownMin, tntDamage, fireChargeDamage;
    public boolean allowBreakAfterBreach;

    public final Map<String, String> messages = new LinkedHashMap<>();

    public Settings(BaseRaidPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class LevelSpec {
        public final int radius;
        public final Cost cost;

        LevelSpec(int radius, Cost cost) {
            this.radius = radius;
            this.cost = cost;
        }
    }

    public static final class WallSpec {
        public final Material material;
        public final String name;
        public final int hp;
        public final Cost cost;

        WallSpec(Material material, String name, int hp, Cost cost) {
            this.material = material;
            this.name = name;
            this.hp = hp;
            this.cost = cost;
        }
    }

    public void load() {
        var cfg = plugin.getConfig();
        worldName = cfg.getString("world", "world");
        baseY = cfg.getInt("base-y", 200);
        gridGap = cfg.getInt("grid-gap", 32);

        spawnWorld = cfg.getString("spawn.world", worldName);
        spawnX = cfg.getDouble("spawn.x", 0.5);
        spawnY = cfg.getDouble("spawn.y", 100.0);
        spawnZ = cfg.getDouble("spawn.z", 0.5);
        spawnYaw = (float) cfg.getDouble("spawn.yaw", 0.0);
        spawnPitch = (float) cfg.getDouble("spawn.pitch", 0.0);

        regionAbove = cfg.getInt("region.height-above", 48);
        regionBelow = cfg.getInt("region.depth-below", 6);

        initialFuelHours = cfg.getInt("purchase.initial-fuel-hours", 24);
        purchaseCost = Cost.parse(cfg.getConfigurationSection("purchase.cost"));

        coreItem = mat("core.display-item", Material.MACE);
        pedestal = mat("core.pedestal", Material.OBSIDIAN);

        islandTop = mat("island.top", Material.GRASS_BLOCK);
        islandFiller = mat("island.filler", Material.DIRT);
        fillerLayers = Math.max(1, cfg.getInt("island.filler-layers", 2));
        islandLevels.clear();
        ConfigurationSection ls = cfg.getConfigurationSection("island.levels");
        if (ls != null) {
            for (String key : ls.getKeys(false)) {
                Integer lvl = parseInt(key);
                ConfigurationSection s = ls.getConfigurationSection(key);
                if (lvl == null || s == null) continue;
                islandLevels.put(lvl, new LevelSpec(
                        Math.max(4, s.getInt("radius", 8)),
                        Cost.parse(s.getConfigurationSection("cost"))));
            }
        }
        if (islandLevels.isEmpty()) {
            islandLevels.put(1, new LevelSpec(8, new Cost()));
        }

        wallHeight = Math.max(1, cfg.getInt("wall.height", 2));
        wallThickness = Math.max(1, cfg.getInt("wall.thickness", 2));
        wallInset = Math.max(1, cfg.getInt("wall.inset", 3));
        wallLevels.clear();
        ConfigurationSection ws = cfg.getConfigurationSection("wall.levels");
        if (ws != null) {
            for (String key : ws.getKeys(false)) {
                Integer lvl = parseInt(key);
                ConfigurationSection s = ws.getConfigurationSection(key);
                if (lvl == null || s == null) continue;
                wallLevels.put(lvl, new WallSpec(
                        mat(s, "material", Material.IRON_BLOCK),
                        s.getString("name", "Стена"),
                        Math.max(1, s.getInt("hp", 10)),
                        Cost.parse(s.getConfigurationSection("cost"))));
            }
        }
        if (wallLevels.isEmpty()) {
            wallLevels.put(1, new WallSpec(Material.IRON_BLOCK, "Железная", 10, new Cost()));
        }

        fuelItem = mat("fuel.item", Material.COAL);
        fuelHoursPerItem = Math.max(1, cfg.getInt("fuel.hours-per-item", 1));
        fuelMaxHours = Math.max(1, cfg.getInt("fuel.max-hours", 24));

        raidDurationMin = Math.max(1, cfg.getInt("raid.duration-minutes", 10));
        raidCooldownMin = Math.max(0, cfg.getInt("raid.cooldown-minutes", 30));
        tntDamage = Math.max(1, cfg.getInt("raid.tnt-damage", 2));
        fireChargeDamage = Math.max(1, cfg.getInt("raid.fire-charge-damage", 1));
        allowBreakAfterBreach = cfg.getBoolean("raid.allow-break-after-breach", true);

        messages.clear();
        ConfigurationSection ms = cfg.getConfigurationSection("messages");
        if (ms != null) {
            for (String key : ms.getKeys(true)) {
                if (ms.isString(key)) messages.put(key, ms.getString(key, key));
            }
        }
    }

    public World baseWorld() {
        World w = Bukkit.getWorld(worldName);
        return w != null ? w : Bukkit.getWorlds().get(0);
    }

    public Location spawnLoc() {
        World w = Bukkit.getWorld(spawnWorld);
        if (w == null) w = Bukkit.getWorlds().get(0);
        return new Location(w, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public int islandMaxLevel() {
        return islandLevels.isEmpty() ? 1 : islandLevels.lastKey();
    }

    public int wallMaxLevel() {
        return wallLevels.isEmpty() ? 1 : wallLevels.lastKey();
    }

    public int maxIslandRadius() {
        int r = 8;
        for (LevelSpec l : islandLevels.values()) r = Math.max(r, l.radius);
        return r;
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Material mat(String path, Material def) {
        String name = plugin.getConfig().getString(path, def.name());
        Material m = Material.matchMaterial(name == null ? "" : name);
        if (m == null) {
            plugin.getLogger().warning("Неизвестный материал в конфиге: " + path + " = " + name + " (использую " + def + ")");
            return def;
        }
        return m;
    }

    private static Material mat(ConfigurationSection s, String path, Material def) {
        String name = s.getString(path, def.name());
        Material m = Material.matchMaterial(name == null ? "" : name);
        return m != null ? m : def;
    }
}
