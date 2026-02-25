package com.avertox.jobsystem.tracker;

import org.bukkit.Location;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlacedBlockTracker {
    private final Set<String> placed = ConcurrentHashMap.newKeySet();

    public void markPlaced(Location location) {
        placed.add(key(location));
    }

    public boolean consumeIfPlaced(Location location) {
        return placed.remove(key(location));
    }

    private String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
