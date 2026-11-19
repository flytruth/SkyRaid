/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Данные одной небесной базы. Центр (x, y, z) — это верхний блок платформы;
 * все ключевые точки (ядро, хранилище, стена) вычисляются от него.
 */
public class BaseData {

    private final UUID id;
    private final UUID ownerId;
    private String ownerName;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private int level;
    private int wallLevel;
    private long protectedUntil;
    private final long createdAt;

    public BaseData(UUID id, UUID ownerId, String ownerName, String worldName,
                    int x, int y, int z, int level, int wallLevel,
                    long protectedUntil, long createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.level = level;
        this.wallLevel = wallLevel;
        this.protectedUntil = protectedUntil;
        this.createdAt = createdAt;
    }

    public Location center() {
        World world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z);
    }

    /** Защита активна, пока в ядре осталось записанное топливо. */
    public boolean isProtected() {
        return protectedUntil > System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getWallLevel() {
        return wallLevel;
    }

    public void setWallLevel(int wallLevel) {
        this.wallLevel = wallLevel;
    }

    public long getProtectedUntil() {
        return protectedUntil;
    }

    public void setProtectedUntil(long protectedUntil) {
        this.protectedUntil = protectedUntil;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
