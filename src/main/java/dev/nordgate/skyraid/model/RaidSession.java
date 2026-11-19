/*
 * SkyRaid — небесные базы с топливной защитой ядра и рейдами.
 * Copyright (c) 2025 Nordgate Studio.
 */
package dev.nordgate.skyraid.model;

import net.kyori.adventure.bossbar.BossBar;

import java.util.UUID;

/** Активный рейд: участник, цель, таймер и состояние стены. */
public class RaidSession {

    private final BaseData base;
    private final UUID raiderId;
    private final String raiderName;
    private final long startedAt;
    private final long durationMillis;
    private final int maxWallHp;
    private int wallHp;
    private boolean wallDestroyed;
    private BossBar raiderBar;
    private BossBar ownerBar;

    public RaidSession(BaseData base, UUID raiderId, String raiderName,
                       long durationMillis, int maxWallHp) {
        this.base = base;
        this.raiderId = raiderId;
        this.raiderName = raiderName;
        this.startedAt = System.currentTimeMillis();
        this.durationMillis = durationMillis;
        this.maxWallHp = maxWallHp;
        this.wallHp = maxWallHp;
    }

    public long getEndsAt() {
        return startedAt + durationMillis;
    }

    /** Наносит урон стене и возвращает остаток прочности. */
    public int damageWall(int amount) {
        wallHp = Math.max(0, wallHp - amount);
        return wallHp;
    }

    public BaseData getBase() {
        return base;
    }

    public UUID getRaiderId() {
        return raiderId;
    }

    public String getRaiderName() {
        return raiderName;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public int getMaxWallHp() {
        return maxWallHp;
    }

    public int getWallHp() {
        return wallHp;
    }

    public boolean isWallDestroyed() {
        return wallDestroyed;
    }

    public void setWallDestroyed(boolean wallDestroyed) {
        this.wallDestroyed = wallDestroyed;
    }

    public BossBar getRaiderBar() {
        return raiderBar;
    }

    public void setRaiderBar(BossBar raiderBar) {
        this.raiderBar = raiderBar;
    }

    public BossBar getOwnerBar() {
        return ownerBar;
    }

    public void setOwnerBar(BossBar ownerBar) {
        this.ownerBar = ownerBar;
    }
}
