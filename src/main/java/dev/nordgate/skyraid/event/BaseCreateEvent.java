/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.event;

import dev.nordgate.skyraid.model.BaseData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Вызывается после покупки и генерации новой базы. */
public class BaseCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BaseData base;
    private final Player creator;

    public BaseCreateEvent(BaseData base, Player creator) {
        this.base = base;
        this.creator = creator;
    }

    public BaseData getBase() {
        return base;
    }

    public Player getCreator() {
        return creator;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
