/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.event;

import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.RaidSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Вызывается в момент старта рейда. Внешние плагины (например, PvP-режим)
 * подписываются на это событие, чтобы включить свои механики в регионе базы.
 */
public class RaidStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final RaidSession session;

    public RaidStartEvent(RaidSession session) {
        this.session = session;
    }

    public RaidSession getSession() {
        return session;
    }

    public BaseData getBase() {
        return session.getBase();
    }

    public UUID getRaiderId() {
        return session.getRaiderId();
    }

    public Player getRaider() {
        return Bukkit.getPlayer(session.getRaiderId());
    }

    public UUID getOwnerId() {
        return session.getBase().getOwnerId();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
