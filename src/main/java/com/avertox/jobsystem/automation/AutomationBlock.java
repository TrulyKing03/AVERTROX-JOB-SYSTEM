package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AutomationBlock {
    private final UUID owner;
    private final JobType jobType;
    private final String locationKey;
    private int level;
    private int slotBase = 6;
    private int slotsPerLevel = 2;
    private final Map<Material, Integer> storage = new HashMap<>();

    protected AutomationBlock(UUID owner, JobType jobType, String locationKey, int level) {
        this.owner = owner;
        this.jobType = jobType;
        this.locationKey = locationKey;
        this.level = level;
    }

    public UUID getOwner() {
        return owner;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<Material, Integer> getStorage() {
        return storage;
    }

    public int getCapacity() {
        return 64 * Math.max(1, level);
    }

    public int getSlotCapacity() {
        return Math.max(1, slotBase + ((Math.max(1, level) - 1) * Math.max(0, slotsPerLevel)));
    }

    public int getUsedSlots() {
        return storage.size();
    }

    public void configureSlots(int slotBase, int slotsPerLevel) {
        this.slotBase = Math.max(1, slotBase);
        this.slotsPerLevel = Math.max(0, slotsPerLevel);
    }

    public void addResource(Material material, int amount) {
        if (amount <= 0) {
            return;
        }
        if (!storage.containsKey(material) && storage.size() >= getSlotCapacity()) {
            return;
        }
        int current = storage.values().stream().mapToInt(Integer::intValue).sum();
        if (current >= getCapacity()) {
            return;
        }
        int allowed = Math.min(amount, getCapacity() - current);
        storage.merge(material, allowed, Integer::sum);
    }

    public abstract void generateTick();
}
