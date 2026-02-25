package com.avertox.jobsystem.gui.menu;

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
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class JobOverviewMenu implements BaseMenu {
    private final JobManager jobManager;
    private final Inventory inventory;

    public JobOverviewMenu(JobManager jobManager) {
        this.jobManager = jobManager;
        this.inventory = Bukkit.createInventory(null, 27, "Job Overview");
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
        // Navigation hooks can be added here if submenu routing is needed.
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        int slot = 10;
        for (JobType type : JobType.values()) {
            PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
            Material icon = switch (type) {
                case FARMER -> Material.WHEAT;
                case FISHER -> Material.FISHING_ROD;
                case WOODCUTTER -> Material.IRON_AXE;
                case MINER -> Material.IRON_PICKAXE;
            };
            ItemStack stack = MenuUtil.item(
                    icon,
                    "§e" + type.name(),
                    Arrays.asList(
                            "§7Level: §f" + data.getLevel(),
                            "§7XP: §f" + String.format("%.1f", data.getXp()),
                            "§7Money Earned: §a" + String.format("%.2f", data.getMoneyEarned()),
                            "§7Recipes Unlocked: §f" + data.getUnlockedRecipes().size()
                    )
            );
            inventory.setItem(slot, stack);
            slot += 2;
        }
    }
}
