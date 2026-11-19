/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.listener;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.event.RaidEndEvent;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.RaidSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Служебные завершения рейда: смерть, выход с сервера,
 * опустошение хранилища и защита сундука от воронок во время штурма.
 */
public class RaidFlowListener implements Listener {

    private final SkyRaidPlugin plugin;

    public RaidFlowListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RaidSession session = plugin.raidManager().getByRaider(event.getPlayer().getUniqueId());
        if (session != null) {
            plugin.raidManager().endRaid(session, RaidEndEvent.Reason.DISCONNECT);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        RaidSession session = plugin.raidManager().getByRaider(event.getEntity().getUniqueId());
        if (session != null) {
            plugin.raidManager().endRaid(session, RaidEndEvent.Reason.DEATH);
        }
    }

    /** Рейд успешен, когда рейдер закрывает полностью опустошённое хранилище. */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.CHEST) {
            return;
        }
        Location location = inventory.getLocation();
        if (location == null) {
            return;
        }
        BaseData base = plugin.storage().findByVault(location);
        if (base == null) {
            return;
        }
        RaidSession session = plugin.raidManager().getByBase(base.getId());
        if (session == null || !session.isWallDestroyed()) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!session.getRaiderId().equals(player.getUniqueId())) {
            return;
        }
        if (isEmpty(inventory)) {
            plugin.raidManager().endRaid(session, RaidEndEvent.Reason.SUCCESS);
        }
    }

    /** Во время рейда хранилище нельзя растащить воронками. */
    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (isRaidedVault(event.getSource().getLocation())
                || isRaidedVault(event.getDestination().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean isRaidedVault(Location location) {
        if (location == null) {
            return false;
        }
        BaseData base = plugin.storage().findByVault(location);
        return base != null && plugin.raidManager().getByBase(base.getId()) != null;
    }

    private boolean isEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }
}
