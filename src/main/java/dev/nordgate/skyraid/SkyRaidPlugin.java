/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid;

import dev.nordgate.skyraid.command.BaseCommand;
import dev.nordgate.skyraid.command.RaidCommand;
import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.economy.ItemCurrencyService;
import dev.nordgate.skyraid.island.IslandGenerator;
import dev.nordgate.skyraid.lang.Messages;
import dev.nordgate.skyraid.listener.InteractListener;
import dev.nordgate.skyraid.listener.ProtectionListener;
import dev.nordgate.skyraid.listener.RaidFlowListener;
import dev.nordgate.skyraid.menu.MenuListener;
import dev.nordgate.skyraid.raid.RaidManager;
import dev.nordgate.skyraid.storage.BaseStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Точка входа плагина: собирает сервисы, регистрирует команды и слушателей.
 */
public final class SkyRaidPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private Messages messages;
    private BaseStorage storage;
    private IslandGenerator generator;
    private ItemCurrencyService currency;
    private RaidManager raidManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pluginConfig = new PluginConfig(this);
        this.messages = new Messages(pluginConfig);
        this.storage = new BaseStorage(this);
        this.generator = new IslandGenerator(this);
        this.currency = new ItemCurrencyService(this);
        this.raidManager = new RaidManager(this);

        storage.load();
        registerCommands();
        registerListeners();

        raidManager.start();
        getServer().getScheduler().runTaskTimer(this, storage::save, 6000L, 6000L);

        getLogger().info("SkyRaid включен. Загружено баз: " + storage.all().size());
    }

    @Override
    public void onDisable() {
        if (raidManager != null) {
            raidManager.shutdown();
        }
        if (storage != null) {
            storage.save();
        }
        getLogger().info("SkyRaid выключен.");
    }

    private void registerCommands() {
        PluginCommand base = Objects.requireNonNull(getCommand("base"), "Команда base не описана в plugin.yml");
        PluginCommand raid = Objects.requireNonNull(getCommand("raid"), "Команда raid не описана в plugin.yml");
        base.setExecutor(new BaseCommand(this));
        raid.setExecutor(new RaidCommand(this));
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new ProtectionListener(this), this);
        pm.registerEvents(new InteractListener(this), this);
        pm.registerEvents(new RaidFlowListener(this), this);
    }

    /** Перечитывает config.yml и хранилище баз без перезапуска сервера. */
    public void reloadPlugin() {
        reloadConfig();
        pluginConfig.reload();
        storage.load();
    }

    public PluginConfig config() {
        return pluginConfig;
    }

    public Messages messages() {
        return messages;
    }

    public BaseStorage storage() {
        return storage;
    }

    public IslandGenerator generator() {
        return generator;
    }

    public ItemCurrencyService currency() {
        return currency;
    }

    public RaidManager raidManager() {
        return raidManager;
    }
}
