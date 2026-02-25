package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.data.MySqlManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JobManager {
    private final MySqlManager mySqlManager;
    private final EconomyService economyService;
    private final Map<JobType, Job> jobs = new EnumMap<>(JobType.class);
    private final Map<UUID, Map<JobType, PlayerJobData>> cache = new HashMap<>();
    private final Map<UUID, JobType> activeJobs = new HashMap<>();
    private final Map<UUID, Long> lastSwitchMillis = new HashMap<>();

    public JobManager(MySqlManager mySqlManager, EconomyService economyService) {
        this.mySqlManager = mySqlManager;
        this.economyService = economyService;
    }

    public void registerJob(Job job) {
        jobs.put(job.getType(), job);
    }

    public Job getJob(JobType type) {
        return jobs.get(type);
    }

    public Map<JobType, Job> getRegisteredJobs() {
        return jobs;
    }

    public PlayerJobData getOrCreate(UUID uuid, JobType type) {
        Map<JobType, PlayerJobData> playerData = cache.computeIfAbsent(uuid, id -> new EnumMap<>(JobType.class));
        return playerData.computeIfAbsent(type, k -> new PlayerJobData());
    }

    public void loadPlayer(UUID uuid) {
        Map<JobType, PlayerJobData> loaded = mySqlManager.loadJobs(uuid);
        if (loaded.isEmpty()) {
            loaded = new EnumMap<>(JobType.class);
        }
        cache.put(uuid, loaded);
    }

    public void savePlayer(UUID uuid) {
        Map<JobType, PlayerJobData> playerData = cache.get(uuid);
        if (playerData != null) {
            mySqlManager.saveJobs(uuid, playerData);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
        activeJobs.remove(uuid);
        lastSwitchMillis.remove(uuid);
    }

    public void addProgress(Player player, JobType type, double xp, double baseMoney) {
        Job job = jobs.get(type);
        if (job == null) {
            return;
        }
        PlayerJobData data = getOrCreate(player.getUniqueId(), type);
        job.addXp(player, data, xp);
        double pay = job.scaledMoney(data.getLevel(), baseMoney);
        economyService.deposit(player, pay);
        data.addMoneyEarned(pay);
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            savePlayer(uuid);
        }
    }

    public JobType getActiveJob(UUID uuid) {
        return activeJobs.get(uuid);
    }

    public boolean isActiveJob(UUID uuid, JobType type) {
        JobType active = activeJobs.get(uuid);
        return active != null && active == type;
    }

    public SwitchResult activateJob(UUID uuid, JobType target) {
        JobType active = activeJobs.get(uuid);
        if (active == target) {
            return new SwitchResult(true, 0L, false);
        }
        long now = System.currentTimeMillis();
        if (active != null) {
            long last = lastSwitchMillis.getOrDefault(uuid, 0L);
            long cooldown = TimeUnit.DAYS.toMillis(1);
            long elapsed = now - last;
            if (elapsed < cooldown) {
                return new SwitchResult(false, cooldown - elapsed, true);
            }
        }
        activeJobs.put(uuid, target);
        if (active != null && active != target) {
            lastSwitchMillis.put(uuid, now);
        }
        return new SwitchResult(true, 0L, false);
    }

    public void forceActivateJob(UUID uuid, JobType target) {
        activeJobs.put(uuid, target);
        lastSwitchMillis.put(uuid, System.currentTimeMillis());
    }

    public void clearSwitchCooldown(UUID uuid) {
        lastSwitchMillis.remove(uuid);
    }

    public long getRemainingSwitchCooldownMillis(UUID uuid) {
        JobType active = activeJobs.get(uuid);
        if (active == null) {
            return 0L;
        }
        long last = lastSwitchMillis.getOrDefault(uuid, 0L);
        long cooldown = TimeUnit.DAYS.toMillis(1);
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0L, cooldown - elapsed);
    }

    public void modifyXp(UUID uuid, JobType type, double delta) {
        PlayerJobData data = getOrCreate(uuid, type);
        double next = Math.max(0.0D, data.getXp() + delta);
        data.setXp(next);
        Job job = jobs.get(type);
        if (job != null) {
            data.setLevel(job.computeLevel(next));
        }
    }

    public void modifyLevel(UUID uuid, JobType type, int delta) {
        PlayerJobData data = getOrCreate(uuid, type);
        int next = Math.max(1, data.getLevel() + delta);
        data.setLevel(next);
    }

    public void modifyMoneyEarned(UUID uuid, JobType type, double delta) {
        PlayerJobData data = getOrCreate(uuid, type);
        data.setMoneyEarned(data.getMoneyEarned() + delta);
    }

    public void resetJobProgress(UUID uuid, JobType type) {
        PlayerJobData data = getOrCreate(uuid, type);
        data.setLevel(1);
        data.setXp(0.0D);
        data.setMoneyEarned(0.0D);
        data.getUpgrades().clear();
        data.getUnlockedRecipes().clear();
    }

    public record SwitchResult(boolean success, long remainingMillis, boolean onCooldown) {
    }
}
