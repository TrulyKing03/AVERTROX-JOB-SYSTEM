package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.UUID;

public class AutoHunterBlock extends AutomationBlock {
    public AutoHunterBlock(UUID owner, String locationKey, int level) {
        super(owner, JobType.HUNTER, locationKey, level);
    }

    @Override
    public void generateTick() {
        addResource(Material.BONE, Math.max(1, getLevel()));
        if (getLevel() >= 4) {
            addResource(Material.ARROW, Math.max(1, getLevel() / 2));
        }
    }
}
