/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.command;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.economy.Price;
import dev.nordgate.skyraid.event.BaseCreateEvent;
import dev.nordgate.skyraid.lang.Messages;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.WallTier;
import dev.nordgate.skyraid.util.TextUtil;
import dev.nordgate.skyraid.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /base — покупка, телепорт, расширение и справка по базе. */
public class BaseCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("buy", "tp", "upgrade", "info", "reload");

    private final SkyRaidPlugin plugin;

    public BaseCommand(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }
        Messages m = plugin.messages();
        if (!player.hasPermission("skyraid.command.base")) {
            m.send(player, "no-permission");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "buy", "create" -> buy(player);
            case "tp", "home" -> teleportHome(player);
            case "upgrade" -> upgrade(player);
            case "info" -> info(player);
            case "reload" -> reload(player);
            default -> help(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.equals("reload") && !sender.hasPermission("skyraid.admin")) {
                continue;
            }
            if (sub.startsWith(prefix)) {
                out.add(sub);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ buy

    private void buy(Player player) {
        Messages m = plugin.messages();
        if (plugin.storage().getByOwner(player.getUniqueId()) != null) {
            m.send(player, "already-has-base");
            return;
        }
        List<Price> cost = plugin.config().baseBuyCost();
        if (!plugin.currency().canAfford(player, cost)) {
            m.send(player, "not-enough-resources",
                    "missing", plugin.currency().describeMissing(player, cost));
            return;
        }
        Location spot = plugin.generator().findFreeLocation(plugin.storage());
        if (spot == null) {
            m.send(player, "no-space");
            return;
        }
        plugin.currency().pay(player, cost);

        BaseData base = new BaseData(
                UUID.randomUUID(),
                player.getUniqueId(),
                player.getName(),
                spot.getWorld().getName(),
                spot.getBlockX(), spot.getBlockY(), spot.getBlockZ(),
                1, 1, 0L, System.currentTimeMillis());

        plugin.generator().generate(base);
        plugin.generator().placeCoreAndVault(base);
        plugin.storage().add(base);
        plugin.storage().save();

        player.teleport(plugin.generator().homeSpawn(base));
        m.send(player, "base-created");
        Bukkit.getPluginManager().callEvent(new BaseCreateEvent(base, player));
    }

    // ------------------------------------------------------------------ tp / upgrade / info

    private void teleportHome(Player player) {
        BaseData base = requireBase(player);
        if (base == null) {
            return;
        }
        player.teleport(plugin.generator().homeSpawn(base));
        plugin.messages().send(player, "base-teleport");
    }

    private void upgrade(Player player) {
        BaseData base = requireBase(player);
        if (base == null) {
            return;
        }
        Messages m = plugin.messages();
        PluginConfig cfg = plugin.config();
        int nextLevel = base.getLevel() + 1;
        if (nextLevel > cfg.islandSizes().size()) {
            m.send(player, "base-level-max");
            return;
        }
        List<Price> cost = cfg.islandUpgradeCost(nextLevel);
        if (!plugin.currency().canAfford(player, cost)) {
            m.send(player, "not-enough-resources",
                    "missing", plugin.currency().describeMissing(player, cost));
            return;
        }
        plugin.currency().pay(player, cost);
        base.setLevel(nextLevel);
        plugin.generator().generate(base);
        plugin.storage().save();
        m.send(player, "base-upgraded",
                "level", String.valueOf(nextLevel),
                "size", String.valueOf(cfg.sizeOf(nextLevel)));
    }

    private void info(Player player) {
        BaseData base = requireBase(player);
        if (base == null) {
            return;
        }
        PluginConfig cfg = plugin.config();
        WallTier tier = cfg.wallTier(base.getWallLevel());
        int size = cfg.sizeOf(base.getLevel());

        player.sendMessage(TextUtil.color("&6=== Ваша база ==="));
        player.sendMessage(TextUtil.color("&7Уровень острова: &f" + base.getLevel()
                + " &8(&f" + size + "x" + size + "&8)"));
        player.sendMessage(TextUtil.color("&7Стена: " + (tier != null ? tier.getName() : "&f?")));
        player.sendMessage(TextUtil.color(base.isProtected()
                ? "&7Защита ядра: &aдо " + TimeUtil.clock(base.getProtectedUntil())
                : "&7Защита ядра: &cне записана"));
    }

    private void reload(Player player) {
        Messages m = plugin.messages();
        if (!player.hasPermission("skyraid.admin")) {
            m.send(player, "no-permission");
            return;
        }
        plugin.reloadPlugin();
        m.send(player, "reloaded");
    }

    private void help(Player player) {
        Messages m = plugin.messages();
        m.sendPlain(player, "help-header");
        m.sendPlain(player, "help-buy");
        m.sendPlain(player, "help-tp");
        m.sendPlain(player, "help-upgrade");
        m.sendPlain(player, "help-info");
        m.sendPlain(player, "help-raid");
        m.sendPlain(player, "help-leave");
        if (player.hasPermission("skyraid.admin")) {
            m.sendPlain(player, "help-reload");
        }
    }

    private BaseData requireBase(Player player) {
        BaseData base = plugin.storage().getByOwner(player.getUniqueId());
        if (base == null) {
            plugin.messages().send(player, "no-base");
        }
        return base;
    }
}
