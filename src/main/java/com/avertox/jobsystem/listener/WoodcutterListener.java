package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.jobs.WoodcutterJob;
import com.avertox.jobsystem.listener.util.JobMaterials;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tracker.PlacedBlockTracker;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final JobToolService toolService;
    private final PlacedBlockTracker placedBlockTracker;

    public WoodcutterListener(
            JobManager jobManager,
            ConfigManager configManager,
            WoodcutterJob woodcutterJob,
            JobToolService toolService,
            PlacedBlockTracker placedBlockTracker
    ) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.woodcutterJob = woodcutterJob;
        this.toolService = toolService;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler
    public void onLogBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!JobMaterials.LOGS.contains(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.WOODCUTTER)) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.WOODCUTTER);
        if (!toolService.hasUsableTool(player, JobType.WOODCUTTER)) {
            if (toolService.hasOwnedToolInInventory(player, JobType.WOODCUTTER)) {
                player.sendMessage("§eHold your WOODCUTTER bound tool in main hand to gain XP/money.");
            } else {
                toolService.grantCurrentTool(player, data, JobType.WOODCUTTER);
                player.sendMessage("§aYou received your WOODCUTTER bound tool.");
            }
            return;
        }
        if (placedBlockTracker.consumeIfPlaced(block.getLocation())) {
            return;
        }
        player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
        int toolTier = toolService.getHeldTier(player, JobType.WOODCUTTER);
        int level = data.getLevel();

        // Levels 1-4: standard chopping.
        double xp = configManager.getReward(JobType.WOODCUTTER, "log_xp") * (1.0D + toolTier * 0.10D);
        double money = configManager.getReward(JobType.WOODCUTTER, "log_money") * (1.0D + toolTier * 0.12D);
        jobManager.addProgress(
                player,
                JobType.WOODCUTTER,
                xp,
                money
        );

        // Level 5: tree felling.
        if (woodcutterJob.hasTreeFelling(level)) {
            fellTree(block);
        }

        // Levels 6-10: chopping speed boost.
        if (woodcutterJob.improvedChoppingAndDurability(level)) {
            int amplifier = toolTier >= 8 ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 80, amplifier, true, false, false));
        }
    }

    @EventHandler
    public void onAxeDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.WOODCUTTER)) {
            return;
        }
        if (!toolService.hasUsableTool(player, JobType.WOODCUTTER)) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.WOODCUTTER);
        int toolTier = toolService.getHeldTier(player, JobType.WOODCUTTER);
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
        double cancelChance = Math.min(0.85D, 0.25D + ((level - 6) * 0.08D) + (toolTier * 0.03D));
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
