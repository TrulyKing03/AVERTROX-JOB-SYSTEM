package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.HunterJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HunterListener implements Listener {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final HunterJob hunterJob;
    private final JobToolService toolService;
    private final Map<UUID, Long> lastKillMillis = new HashMap<>();
    private final Map<UUID, Integer> streaks = new HashMap<>();

    public HunterListener(JobManager jobManager, ConfigManager configManager, HunterJob hunterJob, JobToolService toolService) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.hunterJob = hunterJob;
        this.toolService = toolService;
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.HUNTER)) {
            return;
        }

        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.HUNTER);
        if (!toolService.hasUsableTool(player, JobType.HUNTER)) {
            if (toolService.hasOwnedToolInInventory(player, JobType.HUNTER)) {
                player.sendMessage("\u00A7eHold your HUNTER bound tool in main hand to gain XP.");
            } else {
                toolService.grantCurrentTool(player, data, JobType.HUNTER);
                player.sendMessage("\u00A7aYou received your HUNTER bound tool.");
            }
            return;
        }

        int toolTier = toolService.getHeldTier(player, JobType.HUNTER);
        int level = data.getLevel();
        boolean hostile = event.getEntity() instanceof Monster;

        double xp = configManager.getReward(JobType.HUNTER, "kill_xp") * (1.0D + toolTier * 0.10D);
        if (hostile) {
            xp += 2.0D;
        }

        if (hunterJob.hasTrackerInstinct(level)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 0, true, false, false));
        }

        if (hunterJob.hasStreakBonus(level)) {
            applyStreakBonus(player.getUniqueId());
            int streak = streaks.getOrDefault(player.getUniqueId(), 1);
            if (streak >= 3) {
                xp += Math.min(8.0D, streak * 0.75D);
            }
        }

        if (hunterJob.hasLootSense(level)) {
            double dropChance = Math.min(0.35D, 0.10D + (toolTier * 0.02D));
            if (Math.random() < dropChance) {
                event.getDrops().add(new ItemStack(Material.ARROW, Math.max(1, 1 + toolTier / 3)));
            }
        }

        // Hunter money comes from selling drops, not direct kill payouts.
        jobManager.addProgress(player, JobType.HUNTER, xp, 0.0D);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.7f, 1.15f);
    }

    private void applyStreakBonus(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastKillMillis.getOrDefault(uuid, 0L);
        int streak = now - last <= 10000L ? streaks.getOrDefault(uuid, 0) + 1 : 1;
        streaks.put(uuid, streak);
        lastKillMillis.put(uuid, now);
    }
}
