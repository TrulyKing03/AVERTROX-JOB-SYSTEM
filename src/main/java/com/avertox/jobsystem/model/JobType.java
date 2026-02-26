package com.avertox.jobsystem.model;

public enum JobType {
    FARMER,
    FISHER,
    WOODCUTTER,
    MINER,
    HUNTER;

    public String key() {
        return name().toLowerCase();
    }
}
