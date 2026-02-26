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
                player.sendMessage("\u00A7eHold your FISHER bound tool in main hand to gain XP/money.");
            } else {
                toolService.grantCurrentTool(player, data, JobType.FISHER);
                player.sendMessage("\u00A7aYou received your FISHER bound tool.");
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
            xp += 1.0D;
            money += 1.0D;
        }

        String rarity = rollRarity(configManager.getFishRarityRates(), level, toolTier);
        CatchOutcome outcome = applyRarityOutcome(item, rarity, fisherJob.unlocksNewFishTypes(level));
        xp += outcome.xpBonus();
        money += outcome.moneyBonus();

        if (fisherJob.fasterReeling(level) && ("rare".equals(rarity) || "epic".equals(rarity) || "legendary".equals(rarity))) {
            xp += 3.0D;
        }

        double specialBonus = maybeSpecialFish(player, item, level, toolTier);
        money += specialBonus;

        jobManager.addProgress(player, JobType.FISHER, xp, money);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.sendMessage("\u00A7bFish rarity: " + rarity + (specialBonus > 0 ? " \u00A76(SPECIAL CATCH!)" : ""));
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

    private CatchOutcome applyRarityOutcome(Item itemEntity, String rarity, boolean advancedUnlocked) {
        return switch (rarity) {
            case "rare" -> {
                int amount = advancedUnlocked ? 2 : 1;
                itemEntity.setItemStack(new ItemStack(Material.SALMON, amount));
                yield new CatchOutcome(2.0D, 3.0D + (advancedUnlocked ? 2.0D : 0.0D));
            }
            case "epic" -> {
                int amount = advancedUnlocked ? 2 : 1;
                itemEntity.setItemStack(new ItemStack(Material.PUFFERFISH, amount));
                yield new CatchOutcome(4.0D, 6.0D + (advancedUnlocked ? 3.0D : 0.0D));
            }
            case "legendary" -> {
                int amount = advancedUnlocked ? 3 : 1;
                itemEntity.setItemStack(new ItemStack(Material.TROPICAL_FISH, amount));
                yield new CatchOutcome(8.0D, 12.0D + (advancedUnlocked ? 4.0D : 0.0D));
            }
            default -> {
                itemEntity.setItemStack(new ItemStack(Material.COD, advancedUnlocked ? 2 : 1));
                yield new CatchOutcome(0.0D, 0.0D);
            }
        };
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
                case 0 -> "\u00A7dNebula Koi";
                case 1 -> "\u00A7bStormfin";
                case 2 -> "\u00A76Sunflare Snapper";
                default -> "\u00A75Voidscale";
            };
            meta.setDisplayName(name);
            meta.setLore(List.of("\u00A77A rare premium catch.", "\u00A7aSells for bonus money."));
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
        double sum = 0.0D;
        for (String key : keys) {
            sum += adjustedWeight(key, rates, level, toolTier);
        }

        double random = Math.random() * sum;
        double running = 0.0D;
        for (String key : keys) {
            running += adjustedWeight(key, rates, level, toolTier);
            if (random <= running) {
                return key;
            }
        }
        return "common";
    }

    private double adjustedWeight(String key, Map<String, Object> rates, int level, int toolTier) {
        double value = ((Number) rates.get(key)).doubleValue();
        if ("rare".equals(key) && level >= 4) {
            value += 0.08D;
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
        return value;
    }

    private record CatchOutcome(double xpBonus, double moneyBonus) {
    }
}
