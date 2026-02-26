package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;

public class HunterJob extends Job {
    public HunterJob(ConfigManager configManager) {
        super(JobType.HUNTER, configManager);
    }

    public boolean hasTrackerInstinct(int level) {
        return level >= 4;
    }

    public boolean hasStreakBonus(int level) {
        return level >= 6;
    }

    public boolean hasLootSense(int level) {
        return level >= 8;
    }

    public boolean canUseAutomation(int level) {
        return level >= 10;
    }
}
