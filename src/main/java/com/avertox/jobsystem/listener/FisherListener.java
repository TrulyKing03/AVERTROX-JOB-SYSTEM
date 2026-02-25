package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.FisherJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FisherListener implements Listener {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final FisherJob fisherJob;
    private final JobToolService toolService;

    public FisherListener(JobManager jobManager, ConfigManager configManager, FisherJob fisherJob, JobToolService toolService) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.fisherJob = fisherJob;
        this.toolService = toolService;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!toolService.hasUsableTool(player, JobType.FISHER)) {
            return;
        }
        int toolTier = toolService.getHeldTier(player, JobType.FISHER);
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.FISHER);
        int level = data.getLevel();

        if (event.getState() == PlayerFishEvent.State.FISHING && fisherJob.fasterReeling(level)) {
            FishHook hook = event.getHook();
            if (hook != null) {
                hook.setMinWaitTime(20);
                hook.setMaxWaitTime(80);
            }
            return;
        }

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof Item item)) {
            return;
        }

        double xp = configManager.getReward(JobType.FISHER, "catch_xp") * (1.0D + toolTier * 0.10D);
        double money = configManager.getReward(JobType.FISHER, "catch_money") * (1.0D + toolTier * 0.12D);

        // Level 4+: improved rod efficiency and rare fish chance.
        if (fisherJob.hasImprovedRod(level)) {
            xp += 1;
            money += 1;
        }

        String rarity = rollRarity(configManager.getFishRarityRates(), level, toolTier);
        if ("legendary".equals(rarity)) {
            xp += 8;
            money += 12;
        } else if ("epic".equals(rarity)) {
            xp += 4;
            money += 6;
        } else if ("rare".equals(rarity)) {
            xp += 2;
            money += 3;
        }

        // Level 6+: new fish types with higher value.
        if (fisherJob.unlocksNewFishTypes(level)) {
            if ("rare".equals(rarity)) {
                item.setItemStack(new ItemStack(Material.SALMON, 2));
                money += 4;
            } else if ("epic".equals(rarity)) {
                item.setItemStack(new ItemStack(Material.PUFFERFISH, 2));
                money += 8;
            } else if ("legendary".equals(rarity)) {
                item.setItemStack(new ItemStack(Material.TROPICAL_FISH, 3));
                money += 14;
            }
        }

        // Level 8+: faster reeling and rare fish XP bonus.
        if (fisherJob.fasterReeling(level) && ("rare".equals(rarity) || "epic".equals(rarity) || "legendary".equals(rarity))) {
            xp += 3;
        }

        jobManager.addProgress(player, JobType.FISHER, xp, money);
        player.sendMessage("§bFish rarity: " + rarity);
    }

    @EventHandler
    public void onRodDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!toolService.hasUsableTool(player, JobType.FISHER)) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.FISHER);
        if (!fisherJob.hasImprovedRod(data.getLevel())) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        if (mainHand.getType() != Material.FISHING_ROD) {
            return;
        }
        if (Math.random() < 0.45D) {
            event.setCancelled(true);
        }
    }

    private String rollRarity(Map<String, Object> rates, int level, int toolTier) {
        List<String> keys = new ArrayList<>(rates.keySet());
        if (keys.isEmpty()) {
            return "common";
        }
        double sum = 0;
        for (String key : keys) {
            sum += ((Number) rates.get(key)).doubleValue();
        }
        double random = Math.random() * sum;
        double running = 0;
        for (String key : keys) {
            double value = ((Number) rates.get(key)).doubleValue();
            if ("rare".equals(key) && level >= 4) {
                value += 0.08;
            }
            if ("rare".equals(key)) {
                value += toolTier * 0.01D;
            }
            if ("epic".equals(key)) {
                value += toolTier * 0.004D;
            }
            if ("legendary".equals(key)) {
                value += toolTier * 0.002D;
            }
            running += value;
            if (random <= running) {
                return key;
            }
        }
        return "common";
    }
}
