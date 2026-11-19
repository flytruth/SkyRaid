/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.event;

import dev.nordgate.skyraid.model.BaseData;
import dev.nordgate.skyraid.model.RaidSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Вызывается при завершении рейда по любой причине. */
public class RaidEndEvent extends Event {

    public enum Reason {
        /** Хранилище полностью опустошено рейдером. */
        SUCCESS,
        /** Истекли отведённые 10 минут. */
        TIMEOUT,
        /** Рейдер погиб. */
        DEATH,
        /** Рейдер вышел с сервера. */
        DISCONNECT,
        /** Рейдер ушёл слишком далеко от острова. */
        ABANDONED,
        /** Рейдер сам вышел из рейда (/raid leave). */
        LEAVE,
        /** Сервер останавливается или плагин выгружается. */
        PLUGIN_DISABLE
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final RaidSession session;
    private final Reason reason;

    public RaidEndEvent(RaidSession session, Reason reason) {
        this.session = session;
        this.reason = reason;
    }

    public RaidSession getSession() {
        return session;
    }

    public BaseData getBase() {
        return session.getBase();
    }

    public Reason getReason() {
        return reason;
    }

    public boolean isSuccess() {
        return reason == Reason.SUCCESS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
