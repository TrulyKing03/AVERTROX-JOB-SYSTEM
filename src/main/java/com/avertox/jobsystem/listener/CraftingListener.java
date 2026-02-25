package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.recipes.CustomCraftingManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

public class CraftingListener implements Listener {
    private final JobManager jobManager;
    private final CustomCraftingManager customCraftingManager;

    public CraftingListener(JobManager jobManager, CustomCraftingManager customCraftingManager) {
        this.jobManager = jobManager;
        this.customCraftingManager = customCraftingManager;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
            return;
        }
        NamespacedKey key = shapedRecipe.getKey();
        CustomCraftingManager.RecipeRequirement requirement = customCraftingManager.getRequirements().get(key);
        if (requirement == null) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), requirement.jobType());
        if (!data.getUnlockedRecipes().contains(requirement.recipeKey())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
            return;
        }
        CustomCraftingManager.RecipeRequirement requirement = customCraftingManager.getRequirements().get(shapedRecipe.getKey());
        if (requirement == null) {
            return;
        }
        for (HumanEntity who : event.getViewers()) {
            if (who instanceof Player player) {
                PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), requirement.jobType());
                if (!data.getUnlockedRecipes().contains(requirement.recipeKey())) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou have not unlocked this recipe yet.");
                }
            }
        }
    }
}
