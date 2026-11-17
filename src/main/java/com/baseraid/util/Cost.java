package com.baseraid.util;

import com.baseraid.econ.EconomyHook;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Стоимость действия: монеты (Vault) или предметы.
 * Если Vault подключён и coins > 0 — списываются монеты, иначе предметы.
 */
public final class Cost {

    public double coins;
    public final Map<Material, Integer> items = new LinkedHashMap<>();

    public static Cost parse(ConfigurationSection section) {
        Cost cost = new Cost();
        if (section == null) return cost;
        cost.coins = section.getDouble("coins", 0);
        ConfigurationSection is = section.getConfigurationSection("items");
        if (is != null) {
            for (String key : is.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m != null && is.isInt(key)) cost.items.put(m, is.getInt(key));
            }
        }
        return cost;
    }

    public boolean isEmpty() {
        return coins <= 0 && items.isEmpty();
    }

    /** Стоимость задана только монетами, а Vault не подключён. */
    public boolean needsVaultOnly() {
        return coins > 0 && items.isEmpty() && !EconomyHook.available();
    }

    public boolean canPay(Player player) {
        if (coins > 0 && EconomyHook.available()) return EconomyHook.has(player, coins);
        if (!items.isEmpty()) {
            for (Map.Entry<Material, Integer> e : items.entrySet()) {
                if (count(player, e.getKey()) < e.getValue()) return false;
            }
            return true;
        }
        return coins <= 0;
    }

    public void pay(Player player) {
        if (coins > 0 && EconomyHook.available()) {
            EconomyHook.withdraw(player, coins);
            return;
        }
        for (Map.Entry<Material, Integer> e : items.entrySet()) {
            removeItems(player, e.getKey(), e.getValue());
        }
    }

    public String describe() {
        if (coins > 0 && EconomyHook.available()) {
            return String.format(Locale.US, "%.0f монет", coins);
        }
        if (!items.isEmpty()) {
            return items.entrySet().stream()
                    .map(e -> e.getValue() + "x " + e.getKey())
                    .collect(Collectors.joining(", "));
        }
        if (coins > 0) return String.format(Locale.US, "%.0f монет (нужен Vault!)", coins);
        return "бесплатно";
    }

    public static int count(Player player, Material material) {
        PlayerInventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < 36; i++) {
            org.bukkit.inventory.ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == material) n += it.getAmount();
        }
        return n;
    }

    public static boolean removeItems(Player player, Material material, int amount) {
        if (amount <= 0) return true;
        if (count(player, material) < amount) return false;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36 && amount > 0; i++) {
            org.bukkit.inventory.ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != material) continue;
            int take = Math.min(amount, it.getAmount());
            it.setAmount(it.getAmount() - take);
            inv.setItem(i, it);
            amount -= take;
        }
        return true;
    }
}
