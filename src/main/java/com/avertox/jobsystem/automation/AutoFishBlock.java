package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.UUID;

public class AutoFishBlock extends AutomationBlock {
    public AutoFishBlock(UUID owner, String locationKey, int level) {
        super(owner, JobType.FISHER, locationKey, level);
    }

    @Override
    public void generateTick() {
        addResource(Material.COD, Math.max(1, getLevel()));
    }
}
