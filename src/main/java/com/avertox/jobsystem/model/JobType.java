package com.avertox.jobsystem.model;

public enum JobType {
    FARMER,
    FISHER,
    WOODCUTTER,
    MINER;

    public String key() {
        return name().toLowerCase();
    }
}
