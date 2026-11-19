/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.menu;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Универсальное подтверждение: старт рейда и другие необратимые действия. */
public class ConfirmMenu extends Menu {

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;

    private final Runnable onConfirm;

    public ConfirmMenu(SkyRaidPlugin plugin, ItemStack info, Runnable onConfirm) {
        super(plugin.messages().raw("confirm-title"), 27);
        this.onConfirm = onConfirm;
        fill(pane());
        set(SLOT_CONFIRM, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name("&a&lПодтвердить").build());
        set(SLOT_INFO, info);
        set(SLOT_CANCEL, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("&c&lОтмена").build());
    }

    @Override
    public void onClick(Player player, int slot) {
        if (slot == SLOT_CONFIRM) {
            player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
            player.closeInventory();
            onConfirm.run();
        } else if (slot == SLOT_CANCEL) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
            player.closeInventory();
        }
    }
}
