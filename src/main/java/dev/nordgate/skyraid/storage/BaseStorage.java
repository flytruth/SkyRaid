/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.storage;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.model.BaseData;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Хранилище баз в bases.yml. Файл читабелен и правится вручную при
 * необходимости; запись идёт атомарно через YamlConfiguration.
 */
public class BaseStorage {

    private final SkyRaidPlugin plugin;
    private final File file;
    private final Map<UUID, BaseData> byId = new LinkedHashMap<>();
    private final Map<UUID, UUID> ownerIndex = new LinkedHashMap<>();

    public BaseStorage(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        File dir = plugin.getDataFolder();
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку плагина: " + dir);
        }
        this.file = new File(dir, "bases.yml");
    }

    public void load() {
        byId.clear();
        ownerIndex.clear();
        if (!file.exists()) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
            ConfigurationSection section = yaml.getConfigurationSection("bases");
            if (section == null) {
                return;
            }
            for (String key : section.getKeys(false)) {
                try {
                    BaseData base = read(section, key);
                    byId.put(base.getId(), base);
                    ownerIndex.put(base.getOwnerId(), base.getId());
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Пропущена повреждённая запись базы: " + key);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось прочитать bases.yml", ex);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (BaseData base : byId.values()) {
            String path = "bases." + base.getId();
            yaml.set(path + ".owner", base.getOwnerId().toString());
            yaml.set(path + ".owner-name", base.getOwnerName());
            yaml.set(path + ".world", base.getWorldName());
            yaml.set(path + ".x", base.getX());
            yaml.set(path + ".y", base.getY());
            yaml.set(path + ".z", base.getZ());
            yaml.set(path + ".level", base.getLevel());
            yaml.set(path + ".wall-level", base.getWallLevel());
            yaml.set(path + ".protected-until", base.getProtectedUntil());
            yaml.set(path + ".created-at", base.getCreatedAt());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить bases.yml", ex);
        }
    }

    private BaseData read(ConfigurationSection section, String key) {
        String path = "bases." + key;
        return new BaseData(
                UUID.fromString(key),
                UUID.fromString(section.getString(path + ".owner")),
                section.getString(path + ".owner-name", "?"),
                section.getString(path + ".world", "world"),
                section.getInt(path + ".x"),
                section.getInt(path + ".y"),
                section.getInt(path + ".z"),
                Math.max(1, section.getInt(path + ".level", 1)),
                Math.max(1, section.getInt(path + ".wall-level", 1)),
                section.getLong(path + ".protected-until", 0L),
                section.getLong(path + ".created-at", System.currentTimeMillis()));
    }

    public void add(BaseData base) {
        byId.put(base.getId(), base);
        ownerIndex.put(base.getOwnerId(), base.getId());
    }

    public BaseData getBase(UUID id) {
        return byId.get(id);
    }

    public BaseData getByOwner(UUID ownerId) {
        UUID id = ownerIndex.get(ownerId);
        return id == null ? null : byId.get(id);
    }

    public Collection<BaseData> all() {
        return byId.values();
    }

    /** Базы с истёкшей защитой ядра — кандидаты на рейд. */
    public List<BaseData> unprotectedBases() {
        return byId.values().stream()
                .filter(base -> !base.isProtected())
                .sorted(Comparator.comparingLong(BaseData::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** База, в защищённый регион которой попадает точка (или null). */
    public BaseData findAt(Location location) {
        for (BaseData base : byId.values()) {
            if (inRegion(base, location)) {
                return base;
            }
        }
        return null;
    }

    public BaseData findByCore(Location location) {
        for (BaseData base : byId.values()) {
            if (location.equals(plugin.generator().coreLocation(base))) {
                return base;
            }
        }
        return null;
    }

    public BaseData findByVault(Location location) {
        for (BaseData base : byId.values()) {
            if (location.equals(plugin.generator().vaultLocation(base))) {
                return base;
            }
        }
        return null;
    }

    private boolean inRegion(BaseData base, Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(base.getWorldName())) {
            return false;
        }
        int half = plugin.config().sizeOf(base.getLevel()) / 2 + 3;
        return Math.abs(location.getBlockX() - base.getX()) <= half
                && Math.abs(location.getBlockZ() - base.getZ()) <= half
                && location.getBlockY() >= base.getY() - 8
                && location.getBlockY() <= base.getY() + 48;
    }
}
