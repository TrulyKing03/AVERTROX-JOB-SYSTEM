package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.jobs.MinerJob;
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

public class MinerListener implements Listener {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final MinerJob minerJob;

    public MinerListener(JobManager jobManager, ConfigManager configManager, MinerJob minerJob) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.minerJob = minerJob;
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        Player player = event.getPlayer();

        if (JobMaterials.ORES.contains(material)) {
            jobManager.addProgress(
                    player,
                    JobType.MINER,
                    configManager.getReward(JobType.MINER, "ore_xp"),
                    configManager.getReward(JobType.MINER, "ore_money")
            );
            PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.MINER);
            if (minerJob.hasVeinMining(data.getLevel())) {
                breakVein(block, material);
            }
            return;
        }

        if (material == Material.STONE || material == Material.DEEPSLATE) {
            jobManager.addProgress(
                    player,
                    JobType.MINER,
                    configManager.getReward(JobType.MINER, "stone_xp"),
                    configManager.getReward(JobType.MINER, "stone_money")
            );
        }
    }

    private void breakVein(Block origin, Material ore) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Location> visited = new HashSet<>();
        queue.add(origin);
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            if (!visited.add(block.getLocation())) {
                continue;
            }
            if (block.getType() != ore) {
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
            if (visited.size() > 96) {
                return;
            }
        }
    }
}
