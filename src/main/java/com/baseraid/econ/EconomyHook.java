package com.baseraid.econ;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Подключение Vault-экономики (опционально). */
public final class EconomyHook {

    private static Economy economy;

    private EconomyHook() {
    }

    /** true, если Vault и провайдер экономики найдены. */
    public static boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static boolean available() {
        return economy != null;
    }

    public static boolean has(Player player, double amount) {
        return economy != null && economy.getBalance(player) >= amount;
    }

    public static void withdraw(Player player, double amount) {
        if (economy != null) economy.withdrawPlayer(player, amount);
    }
}
