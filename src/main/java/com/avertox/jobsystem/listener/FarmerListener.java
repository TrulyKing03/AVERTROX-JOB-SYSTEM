package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.FarmerJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.listener.util.JobMaterials;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tracker.PlacedBlockTracker;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.StructureType;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FarmerListener implements Listener {
    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final FarmerJob farmerJob;
    private final JobToolService toolService;
    private final PlacedBlockTracker placedBlockTracker;
    private final Set<String> regrowing = new HashSet<>();
    private final Map<String, Boolean> regrowthAreaCache = new HashMap<>();

    public FarmerListener(
            JavaPlugin plugin,
            JobManager jobManager,
            ConfigManager configManager,
            FarmerJob farmerJob,
            JobToolService toolService,
            PlacedBlockTracker placedBlockTracker
    ) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.farmerJob = farmerJob;
        this.toolService = toolService;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!JobMaterials.CROPS.contains(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.FARMER)) {
            return;
        }

        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.FARMER);
        if (!toolService.hasUsableTool(player, JobType.FARMER)) {
            if (toolService.hasOwnedToolInInventory(player, JobType.FARMER)) {
                player.sendMessage("§eHold your FARMER bound tool in main hand to gain XP/money.");
            } else {
                toolService.grantCurrentTool(player, data, JobType.FARMER);
                player.sendMessage("§aYou received your FARMER bound tool.");
            }
            return;
        }

        if (placedBlockTracker.consumeIfPlaced(block.getLocation())) {
            return;
        }

        event.setDropItems(false);
        ItemStack hand = player.getInventory().getItemInMainHand();
        giveHarvestOnlyDrops(player, block, hand);

        player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.25f);
        int toolTier = toolService.getHeldTier(player, JobType.FARMER);
        // Reduced farmer progression rate.
        double xp = configManager.getReward(JobType.FARMER, "crop_xp") * (0.35D + toolTier * 0.03D);
        jobManager.addProgress(player, JobType.FARMER, xp, 0.0D);

        boolean regrowthAllowed = isRegrowthAreaAllowed(block.getLocation());
        if (!regrowthAllowed) {
            player.sendMessage("\u00A7cCrop regrowth only works in city or village areas.");
        }

        if (farmerJob.hasTntAutoHarvest(data.getLevel()) && Math.random() < configManager.getTntAutoHarvestChance()) {
            runTntHarvestBurst(player, block.getLocation(), data.getLevel(), hand, regrowthAllowed);
        }

        if (regrowthAllowed) {
            scheduleRegrowth(block.getLocation(), block.getType(), data.getLevel());
        }
    }

    private void runTntHarvestBurst(Player player, Location center, int level, ItemStack tool, boolean regrowthAllowed) {
        center.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, center.clone().add(0.5, 0.5, 0.5), 18, 1.2, 0.6, 1.2, 0.02);
        player.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.35f);

        int harvested = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distanceSquared(center) > 9.0D) {
                        continue;
                    }
                    Block b = loc.getBlock();
                    if (!JobMaterials.CROPS.contains(b.getType())) {
                        continue;
                    }
                    if (placedBlockTracker.consumeIfPlaced(loc)) {
                        continue;
                    }
                    Material cropType = b.getType();
                    giveHarvestOnlyDrops(player, b, tool);
                    b.setType(Material.AIR, false);
                    if (regrowthAllowed) {
                        scheduleRegrowth(loc, cropType, level);
                    }
                    harvested++;
                }
            }
        }
        if (harvested > 0) {
            double bonusXp = harvested * configManager.getReward(JobType.FARMER, "crop_xp") * 0.15D;
            jobManager.addProgress(player, JobType.FARMER, bonusXp, 0.0D);
            player.sendMessage("§6TNT Harvest: +" + harvested + " crops.");
        }
    }

    private void scheduleRegrowth(Location location, Material cropType, int level) {
        String key = location.toVector() + "|" + location.getWorld().getName();
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
                if (location.getBlock().getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    location.getBlock().setBlockData(ageable, false);
                }
            }
            regrowing.remove(key);
        }, seconds * 20L);
    }

    private boolean isRegrowthAreaAllowed(Location location) {
        if (!configManager.isFarmerRegrowthAreaRestrictionEnabled()) {
            return true;
        }
        if (location.getWorld() == null) {
            return false;
        }

        String chunkKey = location.getWorld().getName() + ":" + (location.getBlockX() >> 4) + ":" + (location.getBlockZ() >> 4);
        Boolean cached = regrowthAreaCache.get(chunkKey);
        if (cached != null) {
            return cached;
        }

        boolean allowed = isWithinCitySpawnRadius(location) || isNearVillage(location);
        if (regrowthAreaCache.size() > 4096) {
            regrowthAreaCache.clear();
        }
        regrowthAreaCache.put(chunkKey, allowed);
        return allowed;
    }

    private boolean isWithinCitySpawnRadius(Location location) {
        int radius = configManager.getFarmerCitySpawnRadiusBlocks();
        if (radius <= 0 || location.getWorld() == null) {
            return false;
        }
        Location spawn = location.getWorld().getSpawnLocation();
        if (!spawn.getWorld().equals(location.getWorld())) {
            return false;
        }
        double dx = location.getX() - spawn.getX();
        double dz = location.getZ() - spawn.getZ();
        return (dx * dx) + (dz * dz) <= (radius * radius);
    }

    private boolean isNearVillage(Location location) {
        int searchRadius = configManager.getFarmerVillageSearchRadiusBlocks();
        if (searchRadius <= 0 || location.getWorld() == null) {
            return false;
        }
        try {
            return location.getWorld().locateNearestStructure(location, StructureType.VILLAGE, searchRadius, false) != null;
        } catch (Exception | NoSuchMethodError ex) {
            return false;
        }
    }

    private void giveHarvestOnlyDrops(Player player, Block crop, ItemStack tool) {
        for (ItemStack drop : crop.getDrops(tool, player)) {
            if (drop.getType().name().endsWith("_SEEDS")) {
                continue;
            }
            player.getInventory().addItem(drop);
        }
    }
}
