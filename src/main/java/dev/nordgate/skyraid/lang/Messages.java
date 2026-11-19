/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.lang;

import dev.nordgate.skyraid.config.PluginConfig;
import dev.nordgate.skyraid.util.TextUtil;
import org.bukkit.entity.Player;

/**
 * Отправка локализованных сообщений из секции messages в config.yml.
 * Плейсхолдеры задаются парами: send(p, "key", "name", value, "count", "3").
 */
public class Messages {

    private final PluginConfig config;

    public Messages(PluginConfig config) {
        this.config = config;
    }

    public String raw(String key, String... replacements) {
        String text = config.message(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return text;
    }

    /** Сообщение с префиксом плагина. */
    public void send(Player player, String key, String... replacements) {
        player.sendMessage(TextUtil.color(config.prefix() + raw(key, replacements)));
    }

    /** Сообщение без префикса — для справок и многострочных блоков. */
    public void sendPlain(Player player, String key, String... replacements) {
        player.sendMessage(TextUtil.color(raw(key, replacements)));
    }
}
