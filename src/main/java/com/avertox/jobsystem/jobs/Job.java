package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class Job {
    private final JobType type;
    protected final ConfigManager configManager;

    protected Job(JobType type, ConfigManager configManager) {
        this.type = type;
        this.configManager = configManager;
    }

    public JobType getType() {
        return type;
    }

    public int computeLevel(double xp) {
        List<Integer> thresholds = configManager.getLevelThresholds(type);
        int level = 1;
        for (int i = 0; i < thresholds.size(); i++) {
            if (xp >= thresholds.get(i)) {
                level = i + 1;
            } else {
                break;
            }
        }
        return Math.max(1, Math.min(level, thresholds.size()));
    }

    public void addXp(Player player, PlayerJobData data, double amount) {
        int oldLevel = data.getLevel();
        data.setXp(data.getXp() + amount);
        int newLevel = computeLevel(data.getXp());
        data.setLevel(newLevel);
        if (newLevel > oldLevel) {
            onLevelUp(player, oldLevel, newLevel);
        }
    }

    public double scaledMoney(int level, double base) {
        int threshold = configManager.getEconomyHighLevelThreshold();
        double multiplier = level >= threshold
                ? configManager.getEconomyMultiplierHighLevel()
                : configManager.getEconomyMultiplierDefault();
        return base * multiplier;
    }

    protected void onLevelUp(Player player, int oldLevel, int newLevel) {
        player.sendMessage("§a" + type.name() + " leveled up: " + oldLevel + " -> " + newLevel);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.15f);

        Location loc = player.getLocation().add(0.0, 1.0, 0.0);
        Particle.DustOptions dust = new Particle.DustOptions(colorForJob(type), 1.8f);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 45, 0.8, 0.5, 0.8, 0.02);
        player.getWorld().spawnParticle(Particle.REDSTONE, loc, 30, 0.7, 0.5, 0.7, dust);
    }

    private Color colorForJob(JobType jobType) {
        return switch (jobType) {
            case FARMER -> Color.LIME;
            case FISHER -> Color.AQUA;
            case WOODCUTTER -> Color.ORANGE;
            case MINER -> Color.SILVER;
        };
    }
}
