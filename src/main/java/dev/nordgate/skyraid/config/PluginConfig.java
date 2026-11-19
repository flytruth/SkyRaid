/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.config;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.economy.Price;
import dev.nordgate.skyraid.model.WallTier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Типизированный доступ к config.yml. Все ключи читаются через этот класс,
 * чтобы остальной код не работал со строковыми путями напрямую.
 */
public class PluginConfig {

    private final SkyRaidPlugin plugin;
    private FileConfiguration cfg;

    public PluginConfig(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        this.cfg = plugin.getConfig();
    }

    // ------------------------------------------------------------------ острова

    public String islandWorld() {
        return cfg.getString("island.world", "world");
    }

    public int islandHeight() {
        return cfg.getInt("island.spawn-height", 200);
    }

    public int islandMinDistance() {
        return cfg.getInt("island.min-distance", 300);
    }

    public int islandSearchRadius() {
        return cfg.getInt("island.search-radius", 3000);
    }

    public int islandMaxAttempts() {
        return cfg.getInt("island.max-attempts", 64);
    }

    public List<Integer> islandSizes() {
        List<Integer> sizes = cfg.getIntegerList("island.sizes");
        return sizes.isEmpty() ? List.of(9) : sizes;
    }

    /** Размер стороны острова для уровня, ограниченный допустимым диапазоном. */
    public int sizeOf(int level) {
        List<Integer> sizes = islandSizes();
        int index = Math.max(1, Math.min(level, sizes.size())) - 1;
        return sizes.get(index);
    }

    public Material islandTop() {
        return material("island.top-block", Material.GRASS_BLOCK);
    }

    public Material islandFill() {
        return material("island.fill-block", Material.STONE);
    }

    public Material islandCore() {
        return material("island.core-block", Material.HEAVY_CORE);
    }

    /** Может вернуть null, если подсветка отключена (AIR/ошибочный ключ). */
    public Material islandLight() {
        Material m = material("island.light-block", Material.SEA_LANTERN);
        return m.isAir() ? null : m;
    }

    // ------------------------------------------------------------------ экономика

    public List<Price> baseBuyCost() {
        return Price.parseList(cfg.getStringList("base.buy-cost"));
    }

    public List<Price> islandUpgradeCost(int toLevel) {
        return Price.parseList(cfg.getStringList("base.upgrade-cost." + toLevel));
    }

    // ------------------------------------------------------------------ стены

    public Material fuelItem() {
        return material("wall.fuel-item", Material.BLAZE_POWDER);
    }

    public int fuelHoursPerItem() {
        return Math.max(1, cfg.getInt("wall.fuel-hours-per-item", 1));
    }

    public int maxProtectionHours() {
        return Math.max(1, cfg.getInt("wall.max-protection-hours", 24));
    }

    public List<Price> wallUpgradeCost(int toLevel) {
        return Price.parseList(cfg.getStringList("wall.upgrade-cost." + toLevel));
    }

    public WallTier wallTier(int level) {
        String path = "wall.levels." + level;
        if (!cfg.isConfigurationSection(path)) {
            return null;
        }
        String name = cfg.getString(path + ".name", "Уровень " + level);
        Material mat = material(path + ".material", Material.IRON_BLOCK);
        int hp = Math.max(1, cfg.getInt(path + ".hp", 10));
        return new WallTier(level, name, mat, hp);
    }

    public int maxWallLevel() {
        ConfigurationSection section = cfg.getConfigurationSection("wall.levels");
        if (section == null) {
            return 1;
        }
        int max = 1;
        for (String key : section.getKeys(false)) {
            try {
                max = Math.max(max, Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
                // нечисловой ключ в секции уровней — пропускаем
            }
        }
        return max;
    }

    // ------------------------------------------------------------------ рейды

    public long raidDurationMillis() {
        return cfg.getLong("raid.duration-seconds", 600L) * 1000L;
    }

    public long raidUseCooldownMillis() {
        return cfg.getLong("raid.use-cooldown-millis", 750L);
    }

    public long raidCooldownMillis() {
        return cfg.getLong("raid.cooldown-seconds", 1800L) * 1000L;
    }

    public List<Price> raidEntryCost() {
        return Price.parseList(cfg.getStringList("raid.entry-cost"));
    }

    public String raidEndTeleport() {
        return cfg.getString("raid.end-teleport", "WORLD_SPAWN");
    }

    /** Урон по стене за единицу взрывчатки; null — предмет не является взрывчаткой. */
    public Integer explosiveDamage(Material item) {
        String path = "raid.explosives." + item.name();
        return cfg.isInt(path) ? cfg.getInt(path) : null;
    }

    // ------------------------------------------------------------------ сообщения

    public String message(String key) {
        return cfg.getString("messages." + key, key);
    }

    public String prefix() {
        return cfg.getString("messages.prefix", "");
    }

    // ------------------------------------------------------------------ служебное

    private Material material(String path, Material fallback) {
        String raw = cfg.getString(path);
        if (raw == null) {
            return fallback;
        }
        Material m = Material.matchMaterial(raw.trim());
        return m == null ? fallback : m;
    }
}
