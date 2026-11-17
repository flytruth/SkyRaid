package com.baseraid.base;

import com.baseraid.Settings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.UUID;

/** База игрока: остров в небе, ядро, уровни, защита топливом. */
public class Base {

    public final UUID owner;
    public volatile String ownerName;

    public int islandLevel = 1;
    public int wallLevel = 1;

    /** До какого момента (epoch millis) база защищена топливом. */
    public long protectionUntil = 0L;

    public final String world;
    /** Координаты центрального блока; y — уровень поверхности острова. */
    public final int x, y, z;

    public UUID coreDisplayId;
    public UUID coreInteractionId;

    public Base(UUID owner, String ownerName, String world, int x, int y, int z) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Settings.LevelSpec level(Settings s) {
        Settings.LevelSpec l = s.islandLevels.get(islandLevel);
        return l != null ? l : s.islandLevels.firstEntry().getValue();
    }

    public int radius(Settings s) {
        return level(s).radius;
    }

    public Settings.WallSpec wall(Settings s) {
        Settings.WallSpec w = s.wallLevels.get(wallLevel);
        return w != null ? w : s.wallLevels.firstEntry().getValue();
    }

    /** Радиус кольца стены (внешний край). */
    public int wallRadius(Settings s) {
        return Math.max(4, radius(s) - s.wallInset);
    }

    /** Находится ли точка в защищаемом регионе базы. */
    public boolean contains(Location loc, Settings s) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) return false;
        int dx = loc.getBlockX() - x;
        int dz = loc.getBlockZ() - z;
        int dy = loc.getBlockY() - y;
        if (dy < -s.regionBelow || dy > s.regionAbove) return false;
        int r = radius(s) + 2;
        return dx * dx + dz * dz <= r * r;
    }

    /** Точка над пьедесталом. */
    public Location center(World w) {
        return new Location(w, x + 0.5, y + 1, z + 0.5);
    }

    /** Точка появления владельца (/base home). */
    public Location home(World w) {
        return new Location(w, x + 2.5, y + 1, z + 0.5);
    }

    /** Точка высадки рейдера — снаружи стены, лицом к центру. */
    public Location raidSpawn(World w, Settings s) {
        Location l = new Location(w, x + 0.5, y + 1.0, z - wallRadius(s) - 2 + 0.5);
        Vector dir = new Vector(x + 0.5 - l.getX(), 0.2, z + 0.5 - l.getZ());
        if (dir.lengthSquared() > 0.001) l.setDirection(dir);
        return l;
    }
}
