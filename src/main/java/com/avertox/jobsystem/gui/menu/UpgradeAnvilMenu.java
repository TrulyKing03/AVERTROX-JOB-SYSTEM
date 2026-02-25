package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class UpgradeAnvilMenu implements BaseMenu {
    private final JobType jobType;
    private final JobManager jobManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final Inventory inventory;

    public UpgradeAnvilMenu(JobType jobType, JobManager jobManager, EconomyService economyService, ConfigManager configManager) {
        this.jobType = jobType;
        this.jobManager = jobManager;
        this.economyService = economyService;
        this.configManager = configManager;
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
        if (slot < 10 || slot > 12) {
            return;
        }
        int tier = (slot - 9);
        String key = upgradeKey(tier);
        double cost = configManager.getUpgradeCost(jobType, key);
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);
        if (data.getLevel() < 4) {
            player.sendMessage("§cReach level 4 in " + jobType.name() + " to unlock upgrades.");
            return;
        }
        if (!economyService.has(player, cost)) {
            player.sendMessage("§cInsufficient funds.");
            return;
        }
        if (economyService.withdraw(player, cost)) {
            data.getUpgrades().put(key, data.getUpgrades().getOrDefault(key, 0) + 1);
            player.sendMessage("§aPurchased " + key + " for $" + cost);
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        for (int tier = 1; tier <= 3; tier++) {
            String key = upgradeKey(tier);
            double cost = configManager.getUpgradeCost(jobType, key);
            int owned = jobManager.getOrCreate(player.getUniqueId(), jobType).getUpgrades().getOrDefault(key, 0);
            inventory.setItem(
                    9 + tier,
                    MenuUtil.item(Material.ANVIL, "§e" + key.toUpperCase(), List.of(
                            "§7Cost: §a$" + cost,
                            "§7Owned: §f" + owned,
                            "§7Click to purchase"
                    ))
            );
        }
    }

    private String upgradeKey(int tier) {
        return switch (jobType) {
            case FARMER -> "hoe_" + tier;
            case FISHER -> "rod_" + tier;
            case WOODCUTTER -> "axe_" + tier;
            case MINER -> "pickaxe_" + tier;
        };
    }
}
