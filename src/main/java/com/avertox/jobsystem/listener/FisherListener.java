package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.jobs.FisherJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.FISHER)) {
            return;
        }

        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), JobType.FISHER);
        if (!toolService.hasUsableTool(player, JobType.FISHER)) {
            if (toolService.hasOwnedToolInInventory(player, JobType.FISHER)) {
                player.sendMessage("§eHold your FISHER bound tool in main hand to gain XP/money.");
            } else {
                toolService.grantCurrentTool(player, data, JobType.FISHER);
                player.sendMessage("§aYou received your FISHER bound tool.");
            }
            return;
        }
        int toolTier = toolService.getHeldTier(player, JobType.FISHER);
        int level = data.getLevel();

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            FishHook hook = event.getHook();
            if (hook != null) {
                int minWait = Math.max(8, 80 - (level * 4) - (toolTier * 4));
                int maxWait = Math.max(25, 220 - (level * 8) - (toolTier * 8));
                hook.setMinWaitTime(minWait);
                hook.setMaxWaitTime(maxWait);
                hook.getWorld().spawnParticle(Particle.WATER_WAKE, hook.getLocation(), 8, 0.25, 0.1, 0.25, 0.02);
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

        if (fisherJob.fasterReeling(level) && ("rare".equals(rarity) || "epic".equals(rarity) || "legendary".equals(rarity))) {
            xp += 3;
        }

        double specialBonus = maybeSpecialFish(player, item, level, toolTier);
        money += specialBonus;

        jobManager.addProgress(player, JobType.FISHER, xp, money);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.sendMessage("§bFish rarity: " + rarity + (specialBonus > 0 ? " §6(SPECIAL CATCH!)" : ""));
    }

    @EventHandler
    public void onRodDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!jobManager.isActiveJob(player.getUniqueId(), JobType.FISHER)) {
            return;
        }
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

    private double maybeSpecialFish(Player player, Item itemEntity, int level, int toolTier) {
        double chance = 0.02D + (level * 0.004D) + (toolTier * 0.006D);
        if (ThreadLocalRandom.current().nextDouble() > Math.min(0.35D, chance)) {
            return 0.0D;
        }
        ItemStack special = new ItemStack(Material.TROPICAL_FISH, 1);
        ItemMeta meta = special.getItemMeta();
        if (meta != null) {
            String name = switch (ThreadLocalRandom.current().nextInt(4)) {
                case 0 -> "§dNebula Koi";
                case 1 -> "§bStormfin";
                case 2 -> "§6Sunflare Snapper";
                default -> "§5Voidscale";
            };
            meta.setDisplayName(name);
            meta.setLore(List.of("§7A rare premium catch.", "§aSells for bonus money."));
            special.setItemMeta(meta);
        }
        itemEntity.setItemStack(special);
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, player.getLocation().add(0, 1.0, 0), 20, 0.35, 0.3, 0.35, 0.08);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.45f);
        return 35.0D + (level * 2.0D) + (toolTier * 3.0D);
    }

    private String rollRarity(Map<String, Object> rates, int level, int toolTier) {
        List<String> keys = new ArrayList<>(rates.keySet());
        if (keys.isEmpty()) {
            return "common";
        }
        double sum = 0;
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
            sum += value;
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
