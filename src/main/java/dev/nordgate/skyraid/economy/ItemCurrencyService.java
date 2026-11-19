/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.economy;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Оплата предметами из инвентаря игрока (основные 36 слотов). */
public class ItemCurrencyService implements CurrencyService {

    private final SkyRaidPlugin plugin;

    public ItemCurrencyService(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canAfford(Player player, List<Price> prices) {
        Inventory inv = player.getInventory();
        for (Price price : prices) {
            if (count(inv, price.getMaterial()) < price.getAmount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void pay(Player player, List<Price> prices) {
        Inventory inv = player.getInventory();
        for (Price price : prices) {
            remove(inv, price.getMaterial(), price.getAmount());
        }
    }

    @Override
    public String describeMissing(Player player, List<Price> prices) {
        Inventory inv = player.getInventory();
        List<String> missing = new ArrayList<>();
        for (Price price : prices) {
            int have = count(inv, price.getMaterial());
            if (have < price.getAmount()) {
                missing.add(plugin.messages().raw("missing-item-format",
                        "amount", String.valueOf(price.getAmount() - have),
                        "material", Items.pretty(price.getMaterial())));
            }
        }
        return String.join("&7, ", missing);
    }

    public static int count(Inventory inventory, Material material) {
        int total = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static void remove(Inventory inventory, Material material, int amount) {
        int left = amount;
        for (int slot = 0; slot < 36 && left > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != material) {
                continue;
            }
            int take = Math.min(left, item.getAmount());
            left -= take;
            if (take >= item.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - take);
                inventory.setItem(slot, item);
            }
        }
    }
}
