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
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FarmerListener implements Listener {
    private static final List<Material> RANDOM_CROPS = List.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS
    );

    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final FarmerJob farmerJob;
    private final JobToolService toolService;
    private final PlacedBlockTracker placedBlockTracker;
    private final Set<String> regrowing = new HashSet<>();

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
        player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.25f);
        int toolTier = toolService.getHeldTier(player, JobType.FARMER);
        double xp = configManager.getReward(JobType.FARMER, "crop_xp") * (1.0D + toolTier * 0.10D);
        double money = configManager.getReward(JobType.FARMER, "crop_money") * (1.0D + toolTier * 0.12D);
        jobManager.addProgress(
                player,
                JobType.FARMER,
                xp,
                money
        );

        if (farmerJob.hasTntAutoHarvest(data.getLevel()) && Math.random() < configManager.getTntAutoHarvestChance()) {
            TNTPrimed tnt = block.getWorld().spawn(block.getLocation().add(0.5, 0.0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(20);
            tnt.setYield(0);
        }

        scheduleRegrowth(block.getLocation(), block.getType(), data.getLevel());
    }

    @EventHandler
    public void onHoeHitFarmland(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.FARMLAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!toolService.hasUsableTool(player, JobType.FARMER)) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.getType().name().endsWith("_HOE")) {
            return;
        }

        Block cropBlock = clicked.getRelative(BlockFace.UP);
        if (cropBlock.getType() != Material.AIR) {
            return;
        }

        Material crop = RANDOM_CROPS.get(ThreadLocalRandom.current().nextInt(RANDOM_CROPS.size()));
        cropBlock.setType(crop, false);
        if (cropBlock.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            cropBlock.setBlockData(ageable, false);
        }
        player.playSound(cropBlock.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f);
        event.setCancelled(true);
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
                if (location.getBlock().getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    location.getBlock().setBlockData(ageable, false);
                }
            }
            regrowing.remove(key);
        }, seconds * 20L);
    }
}
