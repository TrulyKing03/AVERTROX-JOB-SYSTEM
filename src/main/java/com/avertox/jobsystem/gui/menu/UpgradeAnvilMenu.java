package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class UpgradeAnvilMenu implements BaseMenu {
    private final JobType jobType;
    private final JobManager jobManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final JobToolService toolService;
    private final Inventory inventory;

    public UpgradeAnvilMenu(
            JobType jobType,
            JobManager jobManager,
            EconomyService economyService,
            ConfigManager configManager,
            JobToolService toolService
    ) {
        this.jobType = jobType;
        this.jobManager = jobManager;
        this.economyService = economyService;
        this.configManager = configManager;
        this.toolService = toolService;
        this.inventory = Bukkit.createInventory(null, 27, "Upgrade Anvil: " + jobType.name());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        refresh(player);
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);

        if (slot == 11) {
            int currentTier = toolService.getToolTier(data, jobType);
            int nextTier = Math.min(10, currentTier + 1);
            if (currentTier >= 10) {
                player.sendMessage("§6Your tool is already at MAX tier.");
                return;
            }
            if (data.getLevel() < nextTier) {
                player.sendMessage("§cReach job level " + nextTier + " before upgrading this tool tier.");
                return;
            }
            double cost = nextTierCost(nextTier);
            if (!economyService.has(player, cost)) {
                player.sendMessage("§cInsufficient funds. Needed: $" + String.format("%.2f", cost));
                return;
            }
            if (!economyService.withdraw(player, cost)) {
                player.sendMessage("§cTransaction failed.");
                return;
            }
            toolService.setToolTier(data, jobType, nextTier);
            toolService.grantCurrentTool(player, data, jobType);
            player.sendMessage("§aTier upgraded to " + nextTier + " and tool forged.");
            return;
        }

        if (slot == 15) {
            toolService.grantCurrentTool(player, data, jobType);
            player.sendMessage("§aReforged and retrieved your current bound tool.");
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);
        int currentTier = toolService.getToolTier(data, jobType);
        int nextTier = Math.min(10, currentTier + 1);
        double nextCost = currentTier >= 10 ? 0.0D : nextTierCost(nextTier);

        inventory.setItem(
                11,
                MenuUtil.item(Material.ANVIL, "§eUpgrade Tier", List.of(
                        "§7Current Tier: §f" + currentTier,
                        "§7Next Tier: §f" + (currentTier >= 10 ? "MAX" : nextTier),
                        "§7Required Job Level: §f" + (currentTier >= 10 ? "MAX" : nextTier),
                        "§7Cost: §a$" + String.format("%.2f", nextCost),
                        "§aClick to forge a stronger tool"
                ))
        );

        List<String> preview = new ArrayList<>();
        preview.add("§7Current Tier Perks:");
        for (String perk : toolService.perkLore(jobType, currentTier)) {
            preview.add("§a- " + perk);
        }
        if (currentTier < 10) {
            preview.add("§8");
            preview.add("§7Next Tier Perks:");
            for (String perk : toolService.perkLore(jobType, nextTier)) {
                preview.add("§b- " + perk);
            }
        }
        inventory.setItem(13, MenuUtil.item(Material.ENCHANTED_BOOK, "§dPerk Preview", preview));

        inventory.setItem(
                15,
                MenuUtil.item(Material.CHEST, "§bRetrieve Bound Tool", List.of(
                        "§7Current Tier: §f" + currentTier,
                        "§7Reforge and reclaim your tool",
                        "§7Useful after death/loss"
                ))
        );
    }

    private double nextTierCost(int tier) {
        double base = configManager.getUpgradeCost(jobType, bucketKey(tier));
        if (base <= 0.0D) {
            base = 250.0D * Math.max(1, tier);
        }
        return Math.max(100.0D, base * (1.0D + (tier * 0.25D)));
    }

    private String bucketKey(int tier) {
        int bucket = Math.min(3, Math.max(1, ((tier - 1) / 3) + 1));
        return switch (jobType) {
            case FARMER -> "hoe_" + bucket;
            case FISHER -> "rod_" + bucket;
            case WOODCUTTER -> "axe_" + bucket;
            case MINER -> "pickaxe_" + bucket;
        };
    }
}
