package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.jobs.MinerJob;
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
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MinerListener implements Listener {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final MinerJob minerJob;
    private final JobToolService toolService;
    private final PlacedBlockTracker placedBlockTracker;

    public MinerListener(
            JobManager jobManager,
            ConfigManager configManager,
            MinerJob minerJob,
            JobToolService toolService,
            PlacedBlockTracker placedBlockTracker
    ) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.minerJob = minerJob;
        this.toolService = toolService;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        boolean isMinerBlock = JobMaterials.ORES.contains(material)
                || material == Material.STONE
                || material == Material.DEEPSLATE;
        if (!isMinerBlock) {
            return;
        }

        Player player = event.getPlayer();
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.MINER)) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.MINER);
        if (!toolService.hasUsableTool(player, JobType.MINER)) {
            if (toolService.hasOwnedToolInInventory(player, JobType.MINER)) {
                player.sendMessage("§eHold your MINER bound tool in main hand to gain XP/money.");
            } else {
                toolService.grantCurrentTool(player, data, JobType.MINER);
                player.sendMessage("§aYou received your MINER bound tool.");
            }
            return;
        }
        if (placedBlockTracker.consumeIfPlaced(block.getLocation())) {
            return;
        }
        int toolTier = toolService.getHeldTier(player, JobType.MINER);
        int level = data.getLevel();

        if (JobMaterials.ORES.contains(material)) {
            player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.85f, 0.85f);
            double xp = configManager.getReward(JobType.MINER, "ore_xp") * (1.0D + toolTier * 0.10D);

            // Level 4+: movement and mining speed boosts.
            if (minerJob.hasSpeedBoost(level)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 80, 0, true, false, false));
            }

            // Levels 5-7+: pickaxe upgrades influence rewards and drop rate.
            if (minerJob.hasPickaxeUpgrades(level)) {
                int upgradeTier = getUpgradeTier(data.getUpgrades());
                if (upgradeTier > 0) {
                    xp += upgradeTier * 1.5D;
                    maybeDropBonusOre(block, material, upgradeTier + (toolTier / 3));
                }
            }

            jobManager.addProgress(player, JobType.MINER, xp, 0.0D);

            // Levels 8-10: ore vein mining.
            if (minerJob.hasVeinMining(level)) {
                breakVein(block, material);
            }
            return;
        }

        if (material == Material.STONE || material == Material.DEEPSLATE) {
            double xp = configManager.getReward(JobType.MINER, "stone_xp");
            if (minerJob.hasSpeedBoost(level)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 60, 0, true, false, false));
            }
            jobManager.addProgress(player, JobType.MINER, xp, 0.0D);
        }
    }

    @EventHandler
    public void onMineDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        boolean isMinerBlock = JobMaterials.ORES.contains(material)
                || material == Material.STONE
                || material == Material.DEEPSLATE;
        if (!isMinerBlock) {
            return;
        }

        Player player = event.getPlayer();
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.MINER)) {
            return;
        }
        if (!toolService.hasUsableTool(player, JobType.MINER)) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.MINER);
        if (!minerJob.hasSpeedBoost(data.getLevel())) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 100, 0, true, false, false));
    }

    private void maybeDropBonusOre(Block block, Material ore, int upgradeTier) {
        double chance = Math.min(0.40D, 0.10D + (upgradeTier * 0.08D));
        if (Math.random() >= chance) {
            return;
        }
        Material bonusMaterial = switch (ore) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.RAW_GOLD;
            case NETHER_QUARTZ_ORE -> Material.QUARTZ;
            case ANCIENT_DEBRIS -> Material.ANCIENT_DEBRIS;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            default -> null;
        };
        if (bonusMaterial != null) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(bonusMaterial, 1));
        }
    }

    private int getUpgradeTier(Map<String, Integer> upgrades) {
        if (upgrades.getOrDefault("pickaxe_3", 0) > 0) {
            return 3;
        }
        if (upgrades.getOrDefault("pickaxe_2", 0) > 0) {
            return 2;
        }
        if (upgrades.getOrDefault("pickaxe_1", 0) > 0) {
            return 1;
        }
        return 0;
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
