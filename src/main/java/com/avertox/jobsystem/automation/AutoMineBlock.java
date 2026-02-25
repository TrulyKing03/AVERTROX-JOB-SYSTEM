package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.UUID;

public class AutoMineBlock extends AutomationBlock {
    public AutoMineBlock(UUID owner, String locationKey, int level) {
        super(owner, JobType.MINER, locationKey, level);
    }

    @Override
    public void generateTick() {
        addResource(Material.IRON_ORE, Math.max(1, getLevel()));
    }
}
