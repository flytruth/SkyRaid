/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.raid;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.economy.Price;
import dev.nordgate.skyraid.event.RaidEndEvent;
import dev.nordgate.skyraid.event.RaidStartEvent;
import dev.nordgate.skyraid.island.IslandGenerator;
import dev.nordgate.skyraid.lang.Messages;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.RaidSession;
import dev.nordgate.skyraid.model.WallTier;
import dev.nordgate.skyraid.util.TextUtil;
import dev.nordgate.skyraid.util.TimeUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Жизненный цикл рейда: старт из меню, тик таймера, урон по стене
 * и завершение по одной из причин.
 */
public class RaidManager {

    private final SkyRaidPlugin plugin;
    private final Map<UUID, RaidSession> byRaider = new HashMap<>();
    private final Map<UUID, RaidSession> byBase = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> lastExplosiveUse = new HashMap<>();
    private BukkitTask tickTask;

    public RaidManager(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (RaidSession session : List.copyOf(byRaider.values())) {
            endRaid(session, RaidEndEvent.Reason.PLUGIN_DISABLE);
        }
    }

    public RaidSession getByRaider(UUID playerId) {
        return byRaider.get(playerId);
    }

    public RaidSession getByBase(UUID baseId) {
        return byBase.get(baseId);
    }

    public boolean isRaiding(UUID playerId) {
        return byRaider.containsKey(playerId);
    }

    // ------------------------------------------------------------------ старт

    public void startRaid(Player player, BaseData base) {
        Messages m = plugin.messages();
        PluginConfig cfg = plugin.config();
        UUID playerId = player.getUniqueId();

        if (!player.hasPermission("skyraid.command.raid")) {
            m.send(player, "no-permission");
            return;
        }
        if (base.getOwnerId().equals(playerId)) {
            m.send(player, "raid-own-base");
            return;
        }
        if (byRaider.containsKey(playerId)) {
            m.send(player, "raid-already");
            return;
        }
        if (byBase.containsKey(base.getId())) {
            m.send(player, "raid-base-busy");
            return;
        }
        if (base.isProtected()) {
            m.send(player, "raid-protected");
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(playerId, 0L);
        if (readyAt > now) {
            m.send(player, "raid-cooldown", "time", TimeUtil.duration(readyAt - now));
            return;
        }
        List<Price> entryCost = cfg.raidEntryCost();
        if (!plugin.currency().canAfford(player, entryCost)) {
            m.send(player, "not-enough-resources",
                    "missing", plugin.currency().describeMissing(player, entryCost));
            return;
        }
        plugin.currency().pay(player, entryCost);

        WallTier tier = cfg.wallTier(base.getWallLevel());
        int wallHp = tier != null ? tier.getMaxHp() : 8;

        RaidSession session = new RaidSession(base, playerId, player.getName(),
                cfg.raidDurationMillis(), wallHp);

        BossBar raiderBar = BossBar.bossBar(
                TextUtil.color("&c&lРейд базы &f" + base.getOwnerName()),
                1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        BossBar ownerBar = BossBar.bossBar(
                TextUtil.color("&c&lВашу базу рейдят"),
                1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

        player.showBossBar(raiderBar);
        Player owner = Bukkit.getPlayer(base.getOwnerId());
        if (owner != null) {
            owner.showBossBar(ownerBar);
        }
        session.setRaiderBar(raiderBar);
        session.setOwnerBar(ownerBar);

        byRaider.put(playerId, session);
        byBase.put(base.getId(), session);

        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 0.8f);
        player.teleport(plugin.generator().raidSpawn(base));
        m.send(player, "raid-started", "time", TimeUtil.duration(cfg.raidDurationMillis()));
        if (owner != null) {
            owner.playSound(owner.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 0.6f);
            m.send(owner, "raid-owner-alert", "raider", player.getName());
        }

        Bukkit.getPluginManager().callEvent(new RaidStartEvent(session));
    }

    // ------------------------------------------------------------------ взрывчатка

    /** Применение динамита или огненного заряда по стене. */
    public void useExplosive(RaidSession session, Player player, Block wall, ItemStack item) {
        PluginConfig cfg = plugin.config();
        Integer damage = cfg.explosiveDamage(item.getType());
        if (damage == null || session.isWallDestroyed()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastExplosiveUse.get(player.getUniqueId());
        if (last != null && now - last < cfg.raidUseCooldownMillis()) {
            return;
        }
        lastExplosiveUse.put(player.getUniqueId(), now);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        Location center = wall.getLocation().add(0.5, 0.5, 0.5);
        World world = wall.getWorld();
        world.playSound(center, Sound.ENTITY_TNT_PRIMED, 1f, 1.2f);
        world.spawnParticle(Particle.SMOKE, center, 24, 0.35, 0.35, 0.35, 0.02);
        world.spawnParticle(Particle.FLAME, center, 10, 0.3, 0.3, 0.3, 0.02);

        int hp = session.damageWall(damage);
        player.sendActionBar(TextUtil.color(plugin.messages().raw("wall-hit",
                "damage", String.valueOf(damage),
                "hp", String.valueOf(hp),
                "max", String.valueOf(session.getMaxWallHp()))));

        if (hp <= 0) {
            destroyWall(session);
        }
    }

    private void destroyWall(RaidSession session) {
        session.setWallDestroyed(true);
        IslandGenerator generator = plugin.generator();
        BaseData base = session.getBase();

        for (Location location : generator.wallBlocks(base)) {
            location.getBlock().setType(Material.AIR, false);
            location.getWorld().spawnParticle(Particle.EXPLOSION, location.add(0.5, 0.5, 0.5), 1);
        }
        Location center = generator.coreLocation(base).add(0, 1, -1);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        Player raider = Bukkit.getPlayer(session.getRaiderId());
        if (raider != null) {
            plugin.messages().send(raider, "wall-destroyed");
        }
        Player owner = Bukkit.getPlayer(base.getOwnerId());
        if (owner != null) {
            plugin.messages().send(owner, "wall-destroyed");
        }
    }

    // ------------------------------------------------------------------ тик

    private void tick() {
        long now = System.currentTimeMillis();
        for (RaidSession session : List.copyOf(byRaider.values())) {
            long remaining = session.getEndsAt() - now;
            if (remaining <= 0) {
                endRaid(session, RaidEndEvent.Reason.TIMEOUT);
                continue;
            }
            Player raider = Bukkit.getPlayer(session.getRaiderId());
            if (raider == null) {
                endRaid(session, RaidEndEvent.Reason.DISCONNECT);
                continue;
            }
            if (isTooFar(session, raider)) {
                endRaid(session, RaidEndEvent.Reason.ABANDONED);
                continue;
            }

            float progress = (float) Math.max(0.0, Math.min(1.0, (double) remaining / session.getDurationMillis()));
            String time = TimeUtil.timer(remaining);
            if (session.getRaiderBar() != null) {
                session.getRaiderBar().progress(progress);
                session.getRaiderBar().name(TextUtil.color(
                        "&c&lРейд &f" + session.getBase().getOwnerName() + " &8— &f" + time));
            }
            if (session.getOwnerBar() != null) {
                session.getOwnerBar().progress(progress);
                session.getOwnerBar().name(TextUtil.color(
                        "&c&lРейд на вашу базу &8— &f" + time));
            }
        }
    }

    private boolean isTooFar(RaidSession session, Player raider) {
        BaseData base = session.getBase();
        Location location = raider.getLocation();
        double limit = plugin.generator().halfOf(base) + 12;
        if (!location.getWorld().getName().equals(base.getWorldName())) {
            return true;
        }
        double dx = location.getX() - (base.getX() + 0.5);
        double dz = location.getZ() - (base.getZ() + 0.5);
        return dx * dx + dz * dz > limit * limit || location.getY() < base.getY() - 15;
    }

    // ------------------------------------------------------------------ завершение

    public void endRaid(RaidSession session, RaidEndEvent.Reason reason) {
        if (byRaider.get(session.getRaiderId()) != session) {
            return; // сессия уже завершена
        }
        byRaider.remove(session.getRaiderId());
        byBase.remove(session.getBase().getId());
        lastExplosiveUse.remove(session.getRaiderId());

        Player raider = Bukkit.getPlayer(session.getRaiderId());
        Player owner = Bukkit.getPlayer(session.getBase().getOwnerId());
        hideBar(raider, session.getRaiderBar());
        hideBar(owner, session.getOwnerBar());

        BaseData base = session.getBase();
        if (session.isWallDestroyed()) {
            plugin.generator().replaceWall(base);
        }

        if (reason != RaidEndEvent.Reason.PLUGIN_DISABLE) {
            cooldowns.put(session.getRaiderId(),
                    System.currentTimeMillis() + plugin.config().raidCooldownMillis());
        }

        Messages m = plugin.messages();
        switch (reason) {
            case TIMEOUT -> {
                m.send(raider, "raid-ended-timeout");
                teleportOut(raider, base);
            }
            case LEAVE -> {
                m.send(raider, "raid-ended-leave");
                teleportOut(raider, base);
            }
            case ABANDONED -> {
                m.send(raider, "raid-ended-abandoned");
                teleportOut(raider, base);
            }
            case DEATH -> m.send(raider, "raid-ended-death");
            case SUCCESS -> {
                m.send(raider, "raid-success");
                if (owner != null) {
                    m.send(owner, "raid-success-owner", "raider", session.getRaiderName());
                }
                teleportOut(raider, base);
            }
            default -> {
                // DISCONNECT и PLUGIN_DISABLE не требуют сообщений
            }
        }

        Bukkit.getPluginManager().callEvent(new RaidEndEvent(session, reason));
    }

    private void teleportOut(Player raider, BaseData base) {
        if (raider == null || raider.isDead()) {
            return;
        }
        raider.teleport(endLocation(base));
    }

    private void hideBar(Player player, BossBar bar) {
        if (player != null && bar != null) {
            player.hideBossBar(bar);
        }
    }

    /** Точка, куда рейдер возвращается после рейда (спавн мира или координаты из конфига). */
    private Location endLocation(BaseData base) {
        String value = plugin.config().raidEndTeleport();
        if (!"WORLD_SPAWN".equalsIgnoreCase(value)) {
            String[] parts = value.split(";");
            if (parts.length == 4) {
                World world = Bukkit.getWorld(parts[0].trim());
                if (world != null) {
                    try {
                        return new Location(world,
                                Double.parseDouble(parts[1].trim()),
                                Double.parseDouble(parts[2].trim()),
                                Double.parseDouble(parts[3].trim()));
                    } catch (NumberFormatException ignored) {
                        // некорректные координаты — уходим на спавн мира
                    }
                }
            }
        }
        World world = Bukkit.getWorld(base.getWorldName());
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        return world.getSpawnLocation();
    }
}
