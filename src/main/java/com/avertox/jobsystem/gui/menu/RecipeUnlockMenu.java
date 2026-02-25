package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.recipes.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class RecipeUnlockMenu implements BaseMenu {
    private final JobType jobType;
    private final JobManager jobManager;
    private final RecipeManager recipeManager;
    private final Inventory inventory;
    private final List<String> slotsToRecipe = new ArrayList<>();

    public RecipeUnlockMenu(JobType jobType, JobManager jobManager, RecipeManager recipeManager) {
        this.jobType = jobType;
        this.jobManager = jobManager;
        this.recipeManager = recipeManager;
        this.inventory = Bukkit.createInventory(null, 27, "Recipes: " + jobType.name());
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
        if (slot < 10 || slot >= 10 + slotsToRecipe.size()) {
            return;
        }
        String recipeKey = slotsToRecipe.get(slot - 10);
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);
        boolean unlocked = recipeManager.unlock(data, jobType, recipeKey);
        if (unlocked) {
            player.sendMessage("§aRecipe unlocked: " + recipeKey);
        } else {
            player.sendMessage("§cYou do not meet the level requirement.");
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        slotsToRecipe.clear();
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);
        int slot = 10;
        for (String key : recipeManager.availableRecipes(jobType)) {
            int required = recipeManager.requiredLevel(jobType, key);
            boolean unlocked = data.getUnlockedRecipes().contains(key);
            inventory.setItem(
                    slot,
                    MenuUtil.item(unlocked ? Material.LIME_DYE : Material.PAPER, "§e" + key, List.of(
                            "§7Required Level: §f" + required,
                            "§7Status: " + (unlocked ? "§aUnlocked" : "§cLocked"),
                            "§7Click to unlock"
                    ))
            );
            slotsToRecipe.add(key);
            slot++;
        }
    }
}
