package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.recipes.CustomCraftingManager;
import com.avertox.jobsystem.recipes.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeUnlockMenu implements BaseMenu {
    private static final int[] RECIPE_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private static final int[] PREVIEW_GRID_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};

    private final JobType jobType;
    private final JobManager jobManager;
    private final RecipeManager recipeManager;
    private final CustomCraftingManager customCraftingManager;
    private final Inventory inventory;
    private final List<String> slotsToRecipe = new ArrayList<>();

    public RecipeUnlockMenu(
            JobType jobType,
            JobManager jobManager,
            RecipeManager recipeManager,
            CustomCraftingManager customCraftingManager
    ) {
        this.jobType = jobType;
        this.jobManager = jobManager;
        this.recipeManager = recipeManager;
        this.customCraftingManager = customCraftingManager;
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

        if (event.isRightClick()) {
            boolean unlocked = recipeManager.unlock(data, jobType, recipeKey);
            if (unlocked) {
                player.sendMessage("\u00A7aRecipe unlocked: " + recipeKey);
            } else if (data.getUnlockedRecipes().contains(recipeKey)) {
                player.sendMessage("\u00A7eRecipe already unlocked.");
            } else {
                player.sendMessage("\u00A7cYou do not meet the level requirement.");
            }
            return;
        }

        openRecipePreview(player, recipeKey, data);
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.BLUE_STAINED_GLASS_PANE, "\u00A71");
        slotsToRecipe.clear();

        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), jobType);
        inventory.setItem(4, MenuUtil.item(Material.BOOK, "\u00A79\u00A7lMythic Recipe Codex", List.of(
                "\u00A77Profession: \u00A7f" + jobType.name(),
                "\u00A77Level: \u00A7f" + data.getLevel(),
                "\u00A77Unlocked: \u00A7a" + data.getUnlockedRecipes().size(),
                "\u00A7eLeft-Click: preview recipe pattern",
                "\u00A7eRight-Click: unlock recipe"
        )));

        int idx = 0;
        for (String key : recipeManager.availableRecipes(jobType)) {
            if (idx >= RECIPE_SLOTS.length) {
                break;
            }
            int required = recipeManager.requiredLevel(jobType, key);
            boolean unlocked = data.getUnlockedRecipes().contains(key);
            boolean craftableNow = data.getLevel() >= required;

            Material icon;
            String status;
            if (unlocked) {
                icon = Material.LIME_DYE;
                status = "\u00A7aUnlocked (Craftable)";
            } else if (craftableNow) {
                icon = Material.YELLOW_DYE;
                status = "\u00A7eCraftable (Locked)";
            } else {
                icon = Material.RED_DYE;
                status = "\u00A7cUncraftable";
            }

            inventory.setItem(
                    RECIPE_SLOTS[idx],
                    MenuUtil.item(icon, "\u00A7e\u00A7l" + key, List.of(
                            "\u00A77Required Level: \u00A7f" + required,
                            "\u00A77Status: " + status,
                            "\u00A77Vanilla Table: \u00A7aYes",
                            "\u00A7bLeft-Click: preview pattern",
                            unlocked ? "\u00A78Already unlocked" : "\u00A7bRight-Click: unlock recipe"
                    ))
            );
            slotsToRecipe.add(key);
            idx++;
        }
    }

    private void openRecipePreview(Player player, String recipeKey, PlayerJobData data) {
        NamespacedKey key = customCraftingManager.findRecipeKey(jobType, recipeKey);
        if (key == null) {
            player.sendMessage("\u00A7cRecipe pattern not found.");
            return;
        }
        Recipe recipe = Bukkit.getRecipe(key);
        if (!(recipe instanceof ShapedRecipe shaped)) {
            player.sendMessage("\u00A7cRecipe is not a shaped crafting recipe.");
            return;
        }

        Inventory preview = Bukkit.createInventory(null, 27, "Pattern: " + recipeKey);
        MenuUtil.frame(preview, Material.GRAY_STAINED_GLASS_PANE, "\u00A78");

        String[] shape = shaped.getShape();
        Map<Character, ItemStack> ingredients = shaped.getIngredientMap();

        for (int row = 0; row < 3; row++) {
            String line = row < shape.length ? shape[row] : "   ";
            while (line.length() < 3) {
                line += " ";
            }
            for (int col = 0; col < 3; col++) {
                char symbol = line.charAt(col);
                int slot = PREVIEW_GRID_SLOTS[row * 3 + col];
                if (symbol == ' ' || !ingredients.containsKey(symbol) || ingredients.get(symbol) == null) {
                    preview.setItem(slot, MenuUtil.item(Material.BLACK_STAINED_GLASS_PANE, "\u00A78Empty", List.of("\u00A77No ingredient.")));
                    continue;
                }
                preview.setItem(slot, ingredients.get(symbol));
            }
        }

        boolean unlocked = data.getUnlockedRecipes().contains(recipeKey);
        int required = recipeManager.requiredLevel(jobType, recipeKey);
        boolean craftable = unlocked && data.getLevel() >= required;

        preview.setItem(16, MenuUtil.item(Material.CRAFTING_TABLE, "\u00A76\u00A7lResult", List.of(
                "\u00A77Output: \u00A7f" + shaped.getResult().getType(),
                "\u00A77Unlocked: " + (unlocked ? "\u00A7aYes" : "\u00A7cNo"),
                "\u00A77Craftable Now: " + (craftable ? "\u00A7aYes" : "\u00A7cNo")
        )));
        preview.setItem(26, shaped.getResult());

        player.openInventory(preview);
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
