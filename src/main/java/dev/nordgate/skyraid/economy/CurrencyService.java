/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.economy;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Абстракция оплаты. Поставляется ресурсная реализация (ItemCurrencyService);
 * при необходимости сюда добавляется реализация поверх Vault без правок
 * остального кода.
 */
public interface CurrencyService {

    boolean canAfford(Player player, List<Price> prices);

    void pay(Player player, List<Price> prices);

    /** Список недостающих позиций для сообщения об ошибке. */
    String describeMissing(Player player, List<Price> prices);
}
