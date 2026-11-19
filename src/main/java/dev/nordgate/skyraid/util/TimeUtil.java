/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Форматирование длительностей и меток времени для сообщений. */
public final class TimeUtil {

    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private TimeUtil() {
    }

    /** "1ч 30м", "12м 05с", "45с". */
    public static String duration(long millis) {
        long totalSec = Math.max(0L, millis / 1000L);
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        if (h > 0) {
            return h + "ч " + m + "м";
        }
        if (m > 0) {
            return m + "м " + String.format("%02dс", s);
        }
        return s + "с";
    }

    /** Таймер рейда: "09:58". */
    public static String timer(long millis) {
        long totalSec = Math.max(0L, millis / 1000L);
        return String.format("%02d:%02d", totalSec / 60L, totalSec % 60L);
    }

    /** Календарная метка: "25.08.2025 14:30". */
    public static synchronized String clock(long epochMillis) {
        return CLOCK.format(new Date(epochMillis));
    }
}
