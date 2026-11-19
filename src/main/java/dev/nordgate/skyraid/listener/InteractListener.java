/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.listener;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.island.IslandGenerator;
import dev.nordgate.skyraid.menu.CoreMenu;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.RaidSession;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработка кликов по ключевым блокам острова:
 * ядро открывает меню владельцу, хранилище открывается по правилам рейда,
 * стена принимает урон только от взрывчатки во время рейда.
 */
public class InteractListener implements Listener {

    private static final long HINT_COOLDOWN_MILLIS = 1500L;

    private final SkyRaidPlugin plugin;
    private final Map<UUID, Long> lastHint = new HashMap<>();

    public InteractListener(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        Location location = block.getLocation();

        RaidSession session = plugin.raidManager().getByRaider(player.getUniqueId());

        // Стена базы, которую штурмует игрок: урон только взрывчаткой.
        if (session != null && plugin.generator().wallBlocks(session.getBase()).contains(location)) {
            event.setCancelled(true);
            if (action == Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = event.getItem();
                if (item != null && plugin.config().explosiveDamage(item.getType()) != null) {
                    plugin.raidManager().useExplosive(session, player, block, item);
                } else {
                    hint(player, "wall-need-explosive");
                }
            } else {
                hint(player, "wall-need-explosive");
            }
            return;
        }

        BaseData base = plugin.storage().findAt(location);
        if (base == null) {
            return;
        }
        boolean owner = base.getOwnerId().equals(player.getUniqueId());
        IslandGenerator generator = plugin.generator();

        if (location.equals(generator.coreLocation(base))) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
            if (!owner) {
                plugin.messages().send(player, "region-protected");
                return;
            }
            if (plugin.raidManager().getByBase(base.getId()) != null) {
                plugin.messages().send(player, "core-busy");
                return;
            }
            new CoreMenu(plugin, base).open(player);
            return;
        }

        if (location.equals(generator.vaultLocation(base))) {
            RaidSession active = plugin.raidManager().getByBase(base.getId());
            if (active != null) {
                boolean raiderUnlocked = active.getRaiderId().equals(player.getUniqueId())
                        && active.isWallDestroyed();
                if (!raiderUnlocked) {
                    event.setCancelled(true);
                    if (owner) {
                        plugin.messages().send(player, "chest-raid-locked");
                    } else if (active.getRaiderId().equals(player.getUniqueId())) {
                        plugin.messages().send(player, "chest-locked");
                    } else {
                        plugin.messages().send(player, "region-protected");
                    }
                }
                return;
            }
            if (!owner) {
                event.setCancelled(true);
                plugin.messages().send(player, "region-protected");
            }
            return;
        }

        // Прочие блоки региона: чужим взаимодействие запрещено.
        if (!owner && action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    private void hint(Player player, String key) {
        long now = System.currentTimeMillis();
        Long last = lastHint.get(player.getUniqueId());
        if (last != null && now - last < HINT_COOLDOWN_MILLIS) {
            return;
        }
        lastHint.put(player.getUniqueId(), now);
        plugin.messages().send(player, key);
    }
}
