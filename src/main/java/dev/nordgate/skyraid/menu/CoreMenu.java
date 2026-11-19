/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.menu;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.economy.ItemCurrencyService;
import dev.nordgate.skyraid.economy.Price;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.WallTier;
import dev.nordgate.skyraid.util.ItemBuilder;
import dev.nordgate.skyraid.util.Items;
import dev.nordgate.skyraid.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню ядра базы: запись защиты топливом, улучшение стены,
 * расширение острова и сводка по базе.
 */
public class CoreMenu extends Menu {

    private static final int SLOT_FUEL = 10;
    private static final int SLOT_WALL = 12;
    private static final int SLOT_ISLAND = 14;
    private static final int SLOT_INFO = 16;
    private static final int SLOT_SPAWN = 22;

    private final SkyRaidPlugin plugin;
    private final BaseData base;

    public CoreMenu(SkyRaidPlugin plugin, BaseData base) {
        super(plugin.messages().raw("core-menu-title"), 27);
        this.plugin = plugin;
        this.base = base;
        build();
    }

    private void build() {
        fill(pane());
        set(SLOT_FUEL, fuelItem());
        set(SLOT_WALL, wallItem());
        set(SLOT_ISLAND, islandItem());
        set(SLOT_INFO, infoItem());
        set(SLOT_SPAWN, new ItemBuilder(Material.ENDER_PEARL)
                .name("&dВернуться на спавн")
                .lore("&7Телепортирует на спавн мира.")
                .build());
    }

    @Override
    public void onClick(Player player, int slot) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
        switch (slot) {
            case SLOT_FUEL -> addFuel(player);
            case SLOT_WALL -> upgradeWall(player);
            case SLOT_ISLAND -> upgradeIsland(player);
            case SLOT_SPAWN -> {
                player.closeInventory();
                World world = Bukkit.getWorld(base.getWorldName());
                if (world == null) {
                    world = Bukkit.getWorlds().get(0);
                }
                player.teleport(world.getSpawnLocation());
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ действия

    private void addFuel(Player player) {
        PluginConfig cfg = plugin.config();
        Material fuel = cfg.fuelItem();

        int have = ItemCurrencyService.count(player.getInventory(), fuel);
        if (have == 0) {
            plugin.messages().send(player, "fuel-no-items", "item", Items.pretty(fuel));
            return;
        }

        long now = System.currentTimeMillis();
        long perItemMillis = cfg.fuelHoursPerItem() * 3_600_000L;
        long maxMillis = cfg.maxProtectionHours() * 3_600_000L;
        long baseline = Math.max(base.getProtectedUntil(), now);
        long roomMillis = maxMillis - (baseline - now);
        int use = (int) Math.min(have, roomMillis / perItemMillis);

        if (use <= 0) {
            plugin.messages().send(player, "fuel-full",
                    "max", String.valueOf(cfg.maxProtectionHours()));
            return;
        }

        ItemCurrencyService.remove(player.getInventory(), fuel, use);
        base.setProtectedUntil(baseline + use * perItemMillis);
        plugin.storage().save();

        plugin.messages().send(player, "fuel-added",
                "hours", String.valueOf(use * cfg.fuelHoursPerItem()),
                "until", TimeUtil.clock(base.getProtectedUntil()));
        reopen(player);
    }

    private void upgradeWall(Player player) {
        PluginConfig cfg = plugin.config();
        int nextLevel = base.getWallLevel() + 1;
        WallTier next = cfg.wallTier(nextLevel);
        if (next == null || nextLevel > cfg.maxWallLevel()) {
            plugin.messages().send(player, "wall-max");
            return;
        }
        List<Price> cost = cfg.wallUpgradeCost(nextLevel);
        if (!plugin.currency().canAfford(player, cost)) {
            plugin.messages().send(player, "not-enough-resources",
                    "missing", plugin.currency().describeMissing(player, cost));
            return;
        }
        plugin.currency().pay(player, cost);
        base.setWallLevel(nextLevel);
        plugin.generator().replaceWall(base);
        plugin.storage().save();

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.1f);
        plugin.messages().send(player, "wall-upgraded", "tier", next.getName());
        reopen(player);
    }

    private void upgradeIsland(Player player) {
        PluginConfig cfg = plugin.config();
        int nextLevel = base.getLevel() + 1;
        if (nextLevel > cfg.islandSizes().size()) {
            plugin.messages().send(player, "base-level-max");
            return;
        }
        List<Price> cost = cfg.islandUpgradeCost(nextLevel);
        if (!plugin.currency().canAfford(player, cost)) {
            plugin.messages().send(player, "not-enough-resources",
                    "missing", plugin.currency().describeMissing(player, cost));
            return;
        }
        plugin.currency().pay(player, cost);
        base.setLevel(nextLevel);
        plugin.generator().generate(base);
        plugin.storage().save();

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1f);
        plugin.messages().send(player, "base-upgraded",
                "level", String.valueOf(nextLevel),
                "size", String.valueOf(cfg.sizeOf(nextLevel)));
        reopen(player);
    }

    // ------------------------------------------------------------------ предметы

    private ItemStack fuelItem() {
        PluginConfig cfg = plugin.config();
        List<String> lore = new ArrayList<>();
        if (base.isProtected()) {
            lore.add("&7Защита: &aактивна до " + TimeUtil.clock(base.getProtectedUntil()));
            lore.add("&7Осталось: &f" + TimeUtil.duration(base.getProtectedUntil() - System.currentTimeMillis()));
        } else {
            lore.add("&7Защита: &cядро не записано — база уязвима");
        }
        lore.add("");
        lore.add("&7Топливо: &f" + Items.pretty(cfg.fuelItem())
                + " &8(1 шт = " + cfg.fuelHoursPerItem() + "ч)");
        lore.add("&7Лимит записи: &f" + cfg.maxProtectionHours() + "ч");
        lore.add("");
        lore.add("&eНажмите, чтобы записать ядро");
        return new ItemBuilder(cfg.fuelItem()).name("&6Топливо ядра").lore(lore.toArray(new String[0])).build();
    }

    private ItemStack wallItem() {
        PluginConfig cfg = plugin.config();
        WallTier current = cfg.wallTier(base.getWallLevel());
        Material icon = current != null ? current.getMaterial() : Material.IRON_BLOCK;

        List<String> lore = new ArrayList<>();
        lore.add("&7Текущий уровень: " + (current != null ? current.getName() : "?")
                + " &8(&7" + (current != null ? current.getMaxHp() : 0) + " HP&8)");
        WallTier next = cfg.wallTier(base.getWallLevel() + 1);
        if (next != null && base.getWallLevel() + 1 <= cfg.maxWallLevel()) {
            lore.add("&7Следующий: " + next.getName() + " &8(&7" + next.getMaxHp() + " HP&8)");
            for (Price price : cfg.wallUpgradeCost(base.getWallLevel() + 1)) {
                lore.add("&8— &f" + price.describe());
            }
            lore.add("");
            lore.add("&eНажмите, чтобы усилить стену");
        } else {
            lore.add("&cДостигнут максимальный уровень");
        }
        return new ItemBuilder(icon).name("&eСтена хранилища").lore(lore.toArray(new String[0])).build();
    }

    private ItemStack islandItem() {
        PluginConfig cfg = plugin.config();
        List<String> lore = new ArrayList<>();
        int size = cfg.sizeOf(base.getLevel());
        lore.add("&7Уровень: &f" + base.getLevel() + " &8(&f" + size + "x" + size + "&8)");
        if (base.getLevel() < cfg.islandSizes().size()) {
            int nextSize = cfg.sizeOf(base.getLevel() + 1);
            lore.add("&7Следующий: &f" + nextSize + "x" + nextSize);
            for (Price price : cfg.islandUpgradeCost(base.getLevel() + 1)) {
                lore.add("&8— &f" + price.describe());
            }
            lore.add("");
            lore.add("&eНажмите, чтобы расширить остров");
        } else {
            lore.add("&cОстров максимального размера");
        }
        return new ItemBuilder(cfg.islandTop()).name("&aОстров").lore(lore.toArray(new String[0])).build();
    }

    private ItemStack infoItem() {
        PluginConfig cfg = plugin.config();
        WallTier tier = cfg.wallTier(base.getWallLevel());
        List<String> lore = new ArrayList<>();
        lore.add("&7Владелец: &f" + base.getOwnerName());
        lore.add("&7Создана: &f" + TimeUtil.clock(base.getCreatedAt()));
        lore.add("&7Стена: " + (tier != null ? tier.getName() : "&f?"));
        lore.add(base.isProtected()
                ? "&7Защита: &aдо " + TimeUtil.clock(base.getProtectedUntil())
                : "&7Защита: &cне записана");
        return new ItemBuilder(Material.PAPER).name("&fПаспорт базы").lore(lore.toArray(new String[0])).build();
    }

    private void reopen(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> new CoreMenu(plugin, base).open(player));
    }
}
