/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.command;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.event.RaidEndEvent;
import dev.nordgate.skyraid.lang.Messages;
import dev.nordgate.skyraid.menu.RaidListMenu;
import dev.nordgate.skyraid.model.RaidSession;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /raid — меню доступных рейдов; /raid leave — выход из текущего рейда. */
public class RaidCommand implements TabExecutor {

    private final SkyRaidPlugin plugin;

    public RaidCommand(SkyRaidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }
        Messages m = plugin.messages();
        if (!player.hasPermission("skyraid.command.raid")) {
            m.send(player, "no-permission");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            RaidSession session = plugin.raidManager().getByRaider(player.getUniqueId());
            if (session == null) {
                m.send(player, "raid-leave-none");
            } else {
                plugin.raidManager().endRaid(session, RaidEndEvent.Reason.LEAVE);
            }
            return true;
        }
        new RaidListMenu(plugin, 0).open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "leave".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("leave");
        }
        return List.of();
    }
}
