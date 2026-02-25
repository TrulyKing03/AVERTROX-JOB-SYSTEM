package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.FisherJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FisherListener implements Listener {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final FisherJob fisherJob;

    public FisherListener(JobManager jobManager, ConfigManager configManager, FisherJob fisherJob) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.fisherJob = fisherJob;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof Item item)) {
            return;
        }
        Player player = event.getPlayer();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.FISHER);

        double xp = configManager.getReward(JobType.FISHER, "catch_xp");
        double money = configManager.getReward(JobType.FISHER, "catch_money");
        if (fisherJob.fasterReeling(data.getLevel())) {
            xp += 2;
        }

        String rarity = rollRarity(configManager.getFishRarityRates(), data.getLevel());
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
        if (fisherJob.unlocksNewFishTypes(data.getLevel()) && "rare".equals(rarity)) {
            item.setItemStack(new ItemStack(Material.SALMON, 2));
        }
        jobManager.addProgress(player, JobType.FISHER, xp, money);
        player.sendMessage("§bFish rarity: " + rarity);
    }

    private String rollRarity(Map<String, Object> rates, int level) {
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
                value += 0.02;
            }
            running += value;
            if (random <= running) {
                return key;
            }
        }
        return "common";
    }
}
