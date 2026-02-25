package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.FarmerJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.listener.util.JobMaterials;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class FarmerListener implements Listener {
    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final FarmerJob farmerJob;
    private final Set<String> regrowing = new HashSet<>();

    public FarmerListener(JavaPlugin plugin, JobManager jobManager, ConfigManager configManager, FarmerJob farmerJob) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.farmerJob = farmerJob;
    }

    @EventHandler
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!JobMaterials.CROPS.contains(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.FARMER);
        jobManager.addProgress(
                player,
                JobType.FARMER,
                configManager.getReward(JobType.FARMER, "crop_xp"),
                configManager.getReward(JobType.FARMER, "crop_money")
        );

        if (farmerJob.hasTntAutoHarvest(data.getLevel()) && Math.random() < configManager.getTntAutoHarvestChance()) {
            TNTPrimed tnt = block.getWorld().spawn(block.getLocation().add(0.5, 0.0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(20);
            tnt.setYield(0);
        }

        scheduleRegrowth(block.getLocation(), block.getType(), data.getLevel());
    }

    private void scheduleRegrowth(Location location, Material cropType, int level) {
        String key = location.toVector().toString() + "|" + location.getWorld().getName();
        if (!regrowing.add(key)) {
            return;
        }
        int seconds = configManager.getRegrowthSeconds(JobType.FARMER);
        if (farmerJob.hasImprovedRegrowth(level)) {
            seconds = Math.max(5, seconds - 10);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (location.getBlock().getType() == Material.AIR) {
                location.getBlock().setType(cropType);
            }
            regrowing.remove(key);
        }, seconds * 20L);
    }
}
