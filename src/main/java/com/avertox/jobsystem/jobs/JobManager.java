package com.avertox.jobsystem.jobs;

import com.avertox.jobsystem.data.MySqlManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
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
        sendProgressActionBar(player, type, job.getProgress(data), xp, pay);
    }

    public void addMoneyEarned(UUID uuid, JobType type, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        PlayerJobData data = getOrCreate(uuid, type);
        data.addMoneyEarned(amount);
    }

    public Job.ProgressSnapshot getProgress(UUID uuid, JobType type) {
        Job job = jobs.get(type);
        if (job == null) {
            return new Job.ProgressSnapshot(1, 1, 0.0D, 0.0D, 0.0D, 1.0D, true);
        }
        PlayerJobData data = getOrCreate(uuid, type);
        return job.getProgress(data);
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

    private void sendProgressActionBar(Player player, JobType type, Job.ProgressSnapshot progress, double gainedXp, double gainedMoney) {
        StringBuilder action = new StringBuilder();
        action.append("\u00A7e").append(type.name()).append("\u00A77 ");
        action.append(progressBar(progress.progressPercent(), 12));
        action.append("\u00A7f ").append((int) Math.round(progress.progressPercent() * 100.0D)).append("%");

        if (progress.maxed()) {
            action.append(" \u00A76MAX LEVEL");
        } else {
            action.append(" \u00A77Next: \u00A7b").append(formatOneDecimal(progress.xpToNext())).append(" XP");
        }

        action.append(" \u00A78|\u00A7a +").append(formatOneDecimal(gainedXp)).append(" XP");
        if (gainedMoney > 0.0D) {
            action.append(" \u00A78|\u00A7a $").append(formatMoney(gainedMoney));
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(action.toString()));
    }

    private String progressBar(double percent, int segments) {
        int filled = (int) Math.round(Math.max(0.0D, Math.min(1.0D, percent)) * segments);
        StringBuilder bar = new StringBuilder("\u00A78[");
        for (int i = 0; i < segments; i++) {
            bar.append(i < filled ? "\u00A7a|" : "\u00A77|");
        }
        bar.append("\u00A78]");
        return bar.toString();
    }

    private String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
