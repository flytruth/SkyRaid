/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.menu;

import dev.nordgate.skyraid.SkyRaidPlugin;
import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.WallTier;
import dev.nordgate.skyraid.util.ItemBuilder;
import dev.nordgate.skyraid.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Список незащищённых баз, доступных для рейда. */
public class RaidListMenu extends Menu {

    private static final int PER_PAGE = 21;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};
    private static final int SLOT_PREV = 45;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 51;

    private final SkyRaidPlugin plugin;
    private final int page;
    private final List<BaseData> bases;
    private final int pages;
    private final Map<Integer, BaseData> mapped = new HashMap<>();

    public RaidListMenu(SkyRaidPlugin plugin, int page) {
        super(plugin.messages().raw("raid-menu-title"), 54);
        this.plugin = plugin;
        this.page = page;
        this.bases = plugin.storage().unprotectedBases();
        this.pages = Math.max(1, (bases.size() + PER_PAGE - 1) / PER_PAGE);
        build();
    }

    private void build() {
        fill(pane());

        set(4, new ItemBuilder(Material.BOOK)
                .name("&fНезащищённые базы: &c" + bases.size())
                .lore("&7Ядра этих баз не записаны —", "&7их можно атаковать прямо сейчас.")
                .build());

        if (bases.isEmpty()) {
            set(22, new ItemBuilder(Material.BARRIER)
                    .name("&7Нет доступных баз")
                    .lore("&7Все ядра записаны. Возвращайтесь позже.")
                    .build());
            return;
        }

        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < bases.size(); i++) {
            BaseData base = bases.get(from + i);
            int slot = ITEM_SLOTS[i];
            mapped.put(slot, base);
            set(slot, baseItem(base));
        }

        if (page > 0) {
            set(SLOT_PREV, new ItemBuilder(Material.ARROW).name("&fНазад").build());
        }
        set(SLOT_PAGE, new ItemBuilder(Material.BOOK)
                .name("&fСтраница " + (page + 1) + "/" + pages).build());
        if (page + 1 < pages) {
            set(SLOT_NEXT, new ItemBuilder(Material.ARROW).name("&fВперёд").build());
        }
    }

    @Override
    public void onClick(Player player, int slot) {
        if (slot == SLOT_PREV && page > 0) {
            new RaidListMenu(plugin, page - 1).open(player);
            return;
        }
        if (slot == SLOT_NEXT && page + 1 < pages) {
            new RaidListMenu(plugin, page + 1).open(player);
            return;
        }
        BaseData target = mapped.get(slot);
        if (target == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.8f, 1.2f);
        ItemStack info = baseItem(target);
        new ConfirmMenu(plugin, info, () -> plugin.raidManager().startRaid(player, target)).open(player);
    }

    /** Голова владельца с уровнем стены и острова; координаты намеренно скрыты. */
    private ItemStack baseItem(BaseData base) {
        PluginConfig cfg = plugin.config();
        WallTier tier = cfg.wallTier(base.getWallLevel());
        int size = cfg.sizeOf(base.getLevel());

        List<String> lore = new ArrayList<>();
        lore.add("&7Владелец: &f" + base.getOwnerName());
        lore.add("&7Стена: " + (tier != null ? tier.getName() : "&f?")
                + " &8(&7" + (tier != null ? tier.getMaxHp() : 0) + " HP&8)");
        lore.add("&7Остров: &fуровень " + base.getLevel() + " &8(&f" + size + "x" + size + "&8)");
        lore.add("");
        lore.add("&eЛКМ — начать рейд");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(base.getOwnerId()));
        meta.displayName(TextUtil.color("&cБаза &f" + base.getOwnerName()));
        List<net.kyori.adventure.text.Component> components = new ArrayList<>(lore.size());
        for (String line : lore) {
            components.add(TextUtil.color(line));
        }
        meta.lore(components);
        head.setItemMeta(meta);
        return head;
    }
}
