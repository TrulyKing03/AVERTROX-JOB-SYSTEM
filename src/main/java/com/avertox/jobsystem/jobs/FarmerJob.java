package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;

public class FarmerJob extends Job {
    public FarmerJob(ConfigManager configManager) {
        super(JobType.FARMER, configManager);
    }

    public boolean hasSpeedBoost(int level) {
        return level >= 4;
    }

    public boolean hasTntAutoHarvest(int level) {
        return level >= 5;
    }

    public boolean hasImprovedRegrowth(int level) {
        return level >= 7;
    }

    public boolean canUseAutomation(int level) {
        return level >= 10;
    }
}
