package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;

public class MinerJob extends Job {
    public MinerJob(ConfigManager configManager) {
        super(JobType.MINER, configManager);
    }

    public boolean hasSpeedBoost(int level) {
        return level >= 4;
    }

    public boolean hasPickaxeUpgrades(int level) {
        return level >= 5;
    }

    public boolean hasVeinMining(int level) {
        return level >= 8;
    }

    public boolean canUseAutomation(int level) {
        return level >= 10;
    }
}
