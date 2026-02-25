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

public class JobManager {
    private final MySqlManager mySqlManager;
    private final EconomyService economyService;
    private final Map<JobType, Job> jobs = new EnumMap<>(JobType.class);
    private final Map<UUID, Map<JobType, PlayerJobData>> cache = new HashMap<>();

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
}
