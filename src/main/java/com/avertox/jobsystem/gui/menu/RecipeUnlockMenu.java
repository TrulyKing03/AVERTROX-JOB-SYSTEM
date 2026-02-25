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
    private static final int[] RECIPE_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    private final JobType jobType;
    private final JobManager jobManager;
    private final RecipeManager recipeManager;
    private final Inventory inventory;
    private final List<String> slotsToRecipe = new ArrayList<>();

    public RecipeUnlockMenu(JobType jobType, JobManager jobManager, RecipeManager recipeManager) {
        this.jobType = jobType;
        this.jobManager = jobManager;
        this.recipeManager = recipeManager;
        this.inventory = Bukkit.createInventory(null, 45, "Codex of " + jobType.name());
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
        int idx = indexOfSlot(slot);
        if (idx < 0 || idx >= slotsToRecipe.size()) {
            return;
        }
        String recipeKey = slotsToRecipe.get(idx);
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
        MenuUtil.frame(inventory, Material.BLUE_STAINED_GLASS_PANE, "§1");
        slotsToRecipe.clear();

        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);
        inventory.setItem(4, MenuUtil.item(Material.BOOK, "§9§lMythic Recipe Codex", List.of(
                "§7Profession: §f" + jobType.name(),
                "§7Level: §f" + data.getLevel(),
                "§7Unlocked: §a" + data.getUnlockedRecipes().size()
        )));

        int idx = 0;
        for (String key : recipeManager.availableRecipes(jobType)) {
            if (idx >= RECIPE_SLOTS.length) {
                break;
            }
            int required = recipeManager.requiredLevel(jobType, key);
            boolean unlocked = data.getUnlockedRecipes().contains(key);
            inventory.setItem(
                    RECIPE_SLOTS[idx],
                    MenuUtil.item(unlocked ? Material.LIME_DYE : Material.PAPER, "§e§l" + key, List.of(
                            "§7Required Level: §f" + required,
                            "§7Status: " + (unlocked ? "§aUnlocked" : "§cLocked"),
                            unlocked ? "§8Already learned" : "§bClick to unlock"
                    ))
            );
            slotsToRecipe.add(key);
            idx++;
        }
    }

    private int indexOfSlot(int slot) {
        for (int i = 0; i < RECIPE_SLOTS.length; i++) {
            if (RECIPE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
