package com.baseraid.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/** Утилиты текста: цвета (&-коды), формат времени. */
public final class Text {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    /** '&'-строка -> Adventure Component (для названий/описаний предметов). */
    public static Component c(String s) {
        return AMPERSAND.deserialize(s);
    }

    /** '&'-строка -> legacy '§'-строка (для sendMessage, BossBar, названий инвентарей). */
    public static String legacy(String s) {
        return s.replace('&', '\u00a7');
    }

    public static List<Component> lore(String... lines) {
        List<Component> out = new ArrayList<>(lines.length);
        for (String line : lines) out.add(c(line));
        return out;
    }

    /** Формат оставшегося времени: "5 ч 30 мин". */
    public static String fmtTime(long ms) {
        if (ms <= 0) return "0 мин";
        long h = ms / 3600_000L;
        long m = (ms % 3600_000L) / 60_000L;
        return h > 0 ? h + " ч " + m + " мин" : m + " мин";
    }

    /** Формат "м:сс" для таймера рейда. */
    public static String mmss(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
