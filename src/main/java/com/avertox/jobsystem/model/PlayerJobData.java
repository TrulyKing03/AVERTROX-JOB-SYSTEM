package com.avertox.jobsystem.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerJobData {
    private int level;
    private double xp;
    private double moneyEarned;
    private final Set<String> unlockedRecipes;
    private final Map<String, Integer> upgrades;

    public PlayerJobData() {
        this.level = 1;
        this.xp = 0.0D;
        this.moneyEarned = 0.0D;
        this.unlockedRecipes = new HashSet<>();
        this.upgrades = new HashMap<>();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
    }

    public double getMoneyEarned() {
        return moneyEarned;
    }

    public void addMoneyEarned(double amount) {
        this.moneyEarned += amount;
    }

    public Set<String> getUnlockedRecipes() {
        return unlockedRecipes;
    }

    public Map<String, Integer> getUpgrades() {
        return upgrades;
    }
}
