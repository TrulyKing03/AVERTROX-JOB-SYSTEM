package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.jobs.WoodcutterJob;
import com.avertox.jobsystem.listener.util.JobMaterials;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class WoodcutterListener implements Listener {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final WoodcutterJob woodcutterJob;

    public WoodcutterListener(JobManager jobManager, ConfigManager configManager, WoodcutterJob woodcutterJob) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.woodcutterJob = woodcutterJob;
    }

    @EventHandler
    public void onLogBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!JobMaterials.LOGS.contains(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.WOODCUTTER);
        jobManager.addProgress(
                player,
                JobType.WOODCUTTER,
                configManager.getReward(JobType.WOODCUTTER, "log_xp"),
                configManager.getReward(JobType.WOODCUTTER, "log_money")
        );
        if (woodcutterJob.hasTreeFelling(data.getLevel())) {
            fellTree(block);
        }
    }

    private void fellTree(Block origin) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Location> visited = new HashSet<>();
        queue.add(origin);
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            if (!visited.add(block.getLocation())) {
                continue;
            }
            if (!JobMaterials.LOGS.contains(block.getType())) {
                continue;
            }
            block.breakNaturally();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        queue.add(block.getRelative(x, y, z));
                    }
                }
            }
            if (visited.size() > 128) {
                return;
            }
        }
    }
}
