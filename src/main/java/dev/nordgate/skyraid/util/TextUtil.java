/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Преобразование строк с '&' кодами в Adventure-компоненты. */
public final class TextUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {
    }

    public static Component color(String input) {
        return LEGACY.deserialize(input == null ? "" : input);
    }
}
