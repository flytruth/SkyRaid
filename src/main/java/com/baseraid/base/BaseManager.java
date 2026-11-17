package com.baseraid.base;

import com.baseraid.BaseRaidPlugin;
import com.baseraid.Settings;
import com.baseraid.util.Cost;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Хранение баз, покупка, удаление, топливо. */
public class BaseManager {

    private final BaseRaidPlugin plugin;
    private final File file;
    private final Map<UUID, Base> bases = new LinkedHashMap<>();
    private final Set<UUID> expiredNotified = new HashSet<>();
    private int nextSlot = 0;

    public BaseManager(BaseRaidPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bases.yml");
    }

    public Collection<Base> all() {
        return bases.values();
    }

    public Base get(UUID owner) {
        return owner == null ? null : bases.get(owner);
    }

    /** База, в чьём регионе находится точка. */
    public Base at(Location loc) {
        if (loc == null) return null;
        for (Base b : bases.values()) {
            if (b.contains(loc, plugin.settings())) return b;
        }
        return null;
    }

    public Base byName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (Base b : bases.values()) {
            if (b.ownerName != null && b.ownerName.equalsIgnoreCase(name)) return b;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Покупка / удаление
    // ------------------------------------------------------------------

    public void buy(Player player) {
        Settings s = plugin.settings();
        if (get(player.getUniqueId()) != null) {
            plugin.send(player, "have-base");
            return;
        }
        Cost cost = s.purchaseCost;
        if (cost.needsVaultOnly()) {
            plugin.send(player, "needs-vault");
            return;
        }
        if (!cost.isEmpty() && !cost.canPay(player)) {
            plugin.send(player, "not-enough", "{cost}", cost.describe());
            return;
        }
        cost.pay(player);

        World w = s.baseWorld();
        int[] pos = allocate();
        Base b = new Base(player.getUniqueId(), player.getName(), w.getName(), pos[0], s.baseY, pos[1]);
        bases.put(b.owner, b);
        IslandGenerator.generate(b, s, w);
        plugin.cores().spawnCore(b);
        b.protectionUntil = System.currentTimeMillis() + s.initialFuelHours * 3600_000L;
        expiredNotified.remove(b.owner);
        save();

        plugin.send(player, "bought", "{level}", String.valueOf(b.islandLevel));
        player.teleport(b.home(w));
    }

    public boolean delete(Base b, CommandSender sender) {
        if (plugin.raids() != null && plugin.raids().byBase(b) != null) {
            plugin.send(sender, "delete-raided");
            return false;
        }
        plugin.cores().removeCore(b);
        IslandGenerator.clear(b, plugin.settings(), plugin.settings().baseWorld());
        bases.remove(b.owner);
        expiredNotified.remove(b.owner);
        save();
        return true;
    }

    /** Выделение свободной ячейки сетки в небе. */
    private int[] allocate() {
        Settings s = plugin.settings();
        int spacing = 2 * s.maxIslandRadius() + s.gridGap;
        int slot = nextSlot++;
        int perRow = 8;
        int row = slot / perRow;
        int col = slot % perRow;
        int x = (col - 3) * spacing + (row % 2 == 0 ? 0 : spacing / 2);
        int z = (row + 1) * spacing;
        return new int[]{x, z};
    }

    // ------------------------------------------------------------------
    // Топливо
    // ------------------------------------------------------------------

    /**
     * Заправка: списывает топливо из инвентаря, продлевает защиту.
     *
     * @return добавленные часы, -1 — бак полон, 0 — нет топлива.
     */
    public int addFuel(Base b, Player player, int maxItems) {
        Settings s = plugin.settings();
        long now = System.currentTimeMillis();
        long from = Math.max(now, b.protectionUntil);
        long cap = now + s.fuelMaxHours * 3600_000L;
        long perItem = s.fuelHoursPerItem * 3600_000L;
        long spaceMs = cap - from;
        if (spaceMs < perItem) return -1;
        int spaceItems = (int) (spaceMs / perItem);
        int have = Cost.count(player, s.fuelItem);
        if (have <= 0) return 0;
        int n = Math.min(maxItems, Math.min(spaceItems, have));
        if (n <= 0) return 0;
        Cost.removeItems(player, s.fuelItem, n);
        b.protectionUntil = from + n * perItem;
        expiredNotified.remove(b.owner);
        save();
        return n * s.fuelHoursPerItem;
    }

    /** Периодическая проверка: уведомляет владельцев о кончившемся топливе. */
    public void tickFuel() {
        long now = System.currentTimeMillis();
        for (Base b : bases.values()) {
            if (b.protectionUntil <= now) {
                if (expiredNotified.add(b.owner)) {
                    Player p = Bukkit.getPlayer(b.owner);
                    if (p != null && p.isOnline()) plugin.send(p, "fuel-expired");
                }
            } else {
                expiredNotified.remove(b.owner);
            }
        }
    }

    // ------------------------------------------------------------------
    // Хранение (bases.yml)
    // ------------------------------------------------------------------

    public void load() {
        bases.clear();
        expiredNotified.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextSlot = yaml.getInt("nextSlot", 0);
        ConfigurationSection root = yaml.getConfigurationSection("bases");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            UUID owner;
            try {
                owner = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String world = s.getString("world", plugin.settings().worldName);
            Base b = new Base(owner,
                    s.getString("ownerName", "unknown"),
                    world,
                    s.getInt("x"), s.getInt("y", plugin.settings().baseY), s.getInt("z"));
            b.islandLevel = Math.max(1, s.getInt("islandLevel", 1));
            b.wallLevel = Math.max(1, s.getInt("wallLevel", 1));
            b.protectionUntil = s.getLong("protectionUntil", 0L);
            String cd = s.getString("coreDisplay");
            String ci = s.getString("coreInteraction");
            try {
                if (cd != null && !cd.isEmpty()) b.coreDisplayId = UUID.fromString(cd);
                if (ci != null && !ci.isEmpty()) b.coreInteractionId = UUID.fromString(ci);
            } catch (IllegalArgumentException ignored) {
            }
            bases.put(owner, b);
        }
        plugin.getLogger().info("Загружено баз: " + bases.size());
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("nextSlot", nextSlot);
        for (Base b : bases.values()) {
            String path = "bases." + b.owner + ".";
            yaml.set(path + "ownerName", b.ownerName);
            yaml.set(path + "islandLevel", b.islandLevel);
            yaml.set(path + "wallLevel", b.wallLevel);
            yaml.set(path + "protectionUntil", b.protectionUntil);
            yaml.set(path + "world", b.world);
            yaml.set(path + "x", b.x);
            yaml.set(path + "y", b.y);
            yaml.set(path + "z", b.z);
            yaml.set(path + "coreDisplay", b.coreDisplayId == null ? "" : b.coreDisplayId.toString());
            yaml.set(path + "coreInteraction", b.coreInteractionId == null ? "" : b.coreInteractionId.toString());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить bases.yml: " + e.getMessage());
        }
    }
}
