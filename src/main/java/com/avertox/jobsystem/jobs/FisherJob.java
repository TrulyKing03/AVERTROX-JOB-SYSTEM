package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;

public class FisherJob extends Job {
    public FisherJob(ConfigManager configManager) {
        super(JobType.FISHER, configManager);
    }

    public boolean hasImprovedRod(int level) {
        return level >= 4;
    }

    public boolean unlocksNewFishTypes(int level) {
        return level >= 6;
    }

    public boolean fasterReeling(int level) {
        return level >= 8;
    }

    public boolean canUseAutomation(int level) {
        return level >= 10;
    }
}
