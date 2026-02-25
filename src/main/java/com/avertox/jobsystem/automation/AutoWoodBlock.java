package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.UUID;

public class AutoWoodBlock extends AutomationBlock {
    public AutoWoodBlock(UUID owner, String locationKey, int level) {
        super(owner, JobType.WOODCUTTER, locationKey, level);
    }

    @Override
    public void generateTick() {
        addResource(Material.OAK_LOG, Math.max(1, getLevel()));
    }
}
