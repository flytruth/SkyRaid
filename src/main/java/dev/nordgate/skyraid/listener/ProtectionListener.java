/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.listener;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.island.IslandGenerator;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.RaidSession;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Регион острова принадлежит владельцу. Исключение — активный рейд,
 * во время которого рейдер взаимодействует со стеной через взрывчатку.
 */
public class ProtectionListener implements Listener {

    private static final long WARN_COOLDOWN_MILLIS = 2500L;

    private final SkyRaidPlugin plugin;
    private final Map<UUID, Long> lastWarning = new HashMap<>();

    public ProtectionListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        BaseData base = plugin.storage().findAt(event.getBlock().getLocation());
        if (base == null) {
            return;
        }
        if (!base.getOwnerId().equals(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            warn(event.getPlayer(), "region-protected");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        BaseData base = plugin.storage().findAt(location);
        if (base == null) {
            return;
        }
        Player player = event.getPlayer();
        IslandGenerator generator = plugin.generator();
        List<Location> wall = generator.wallBlocks(base);
        boolean keyBlock = location.equals(generator.coreLocation(base))
                || location.equals(generator.vaultLocation(base))
                || wall.contains(location);

        if (keyBlock) {
            event.setCancelled(true);
            RaidSession session = plugin.raidManager().getByRaider(player.getUniqueId());
            if (wall.contains(location) && session != null
                    && session.getBase().getId().equals(base.getId())) {
                warn(player, "wall-need-explosive");
            } else {
                warn(player, "special-block");
            }
            return;
        }
        if (base.getOwnerId().equals(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        warn(player, "region-protected");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (plugin.storage().findAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        BaseData base = plugin.storage().findAt(event.getBlock().getLocation());
        if (base == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player != null && base.getOwnerId().equals(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.storage().findAt(block.getLocation()) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.storage().findAt(block.getLocation()) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (touchesRegion(event.getBlocks())
                || plugin.storage().findAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (touchesRegion(event.getBlocks())
                || plugin.storage().findAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Запрещает чужим телепортироваться на остров (эндер-жемчуг, хорус и т.п.).
     * Служебные телепорты плагина (cause = PLUGIN) проходят свободно.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        BaseData base = plugin.storage().findAt(event.getTo());
        if (base == null) {
            return;
        }
        Player player = event.getPlayer();
        if (base.getOwnerId().equals(player.getUniqueId())) {
            return;
        }
        RaidSession session = plugin.raidManager().getByBase(base.getId());
        if (session != null && session.getRaiderId().equals(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        warn(player, "region-protected");
    }

    private boolean touchesRegion(List<Block> blocks) {
        for (Block block : blocks) {
            if (plugin.storage().findAt(block.getLocation()) != null) {
                return true;
            }
        }
        return false;
    }

    private void warn(Player player, String key) {
        long now = System.currentTimeMillis();
        Long last = lastWarning.get(player.getUniqueId());
        if (last != null && now - last < WARN_COOLDOWN_MILLIS) {
            return;
        }
        lastWarning.put(player.getUniqueId(), now);
        plugin.messages().send(player, key);
    }
}
