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
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        int level = data.getLevel();

        // Levels 1-4: standard chopping.
        jobManager.addProgress(
                player,
                JobType.WOODCUTTER,
                configManager.getReward(JobType.WOODCUTTER, "log_xp"),
                configManager.getReward(JobType.WOODCUTTER, "log_money")
        );

        // Level 5: tree felling.
        if (woodcutterJob.hasTreeFelling(level)) {
            fellTree(block);
        }

        // Levels 6-10: chopping speed boost.
        if (woodcutterJob.improvedChoppingAndDurability(level)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 80, 0, true, false, false));
        }
    }

    @EventHandler
    public void onAxeDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.WOODCUTTER);
        int level = data.getLevel();
        if (!woodcutterJob.improvedChoppingAndDurability(level)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        String typeName = mainHand.getType().name();
        if (!typeName.endsWith("_AXE")) {
            return;
        }
        // Levels 6-10: durability reduction scaling.
        double cancelChance = Math.min(0.65D, 0.25D + ((level - 6) * 0.08D));
        if (Math.random() < cancelChance) {
            event.setCancelled(true);
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
