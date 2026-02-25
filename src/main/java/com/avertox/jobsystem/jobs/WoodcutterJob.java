package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;

public class WoodcutterJob extends Job {
    public WoodcutterJob(ConfigManager configManager) {
        super(JobType.WOODCUTTER, configManager);
    }

    public boolean hasTreeFelling(int level) {
        return level >= 5;
    }

    public boolean improvedChoppingAndDurability(int level) {
        return level >= 6;
    }

    public boolean canUseAutomation(int level) {
        return level >= 10;
    }
}
