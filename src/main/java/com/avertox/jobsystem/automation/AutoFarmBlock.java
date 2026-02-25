package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.UUID;

public class AutoFarmBlock extends AutomationBlock {
    public AutoFarmBlock(UUID owner, String locationKey, int level) {
        super(owner, JobType.FARMER, locationKey, level);
    }

    @Override
    public void generateTick() {
        addResource(Material.WHEAT, Math.max(1, getLevel()));
    }
}
