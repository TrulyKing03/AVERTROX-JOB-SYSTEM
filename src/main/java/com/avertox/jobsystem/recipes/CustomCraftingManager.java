package com.avertox.jobsystem.recipes;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class CustomCraftingManager {
    private final JavaPlugin plugin;
    private final Map<NamespacedKey, RecipeRequirement> requirements = new HashMap<>();

    public CustomCraftingManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes(Iterable<ConfiguredRecipeDefinition> configuredRecipes) {
        requirements.clear();
        for (ConfiguredRecipeDefinition recipe : configuredRecipes) {
            register(recipe);
        }
    }

    public Map<NamespacedKey, RecipeRequirement> getRequirements() {
        return requirements;
    }

    public NamespacedKey findRecipeKey(JobType type, String recipeKey) {
        for (Map.Entry<NamespacedKey, RecipeRequirement> entry : requirements.entrySet()) {
            RecipeRequirement requirement = entry.getValue();
            if (requirement.jobType() == type && requirement.recipeKey().equalsIgnoreCase(recipeKey)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void register(ConfiguredRecipeDefinition definition) {
        if (definition.shape().size() != 3 || definition.ingredients().isEmpty()) {
            plugin.getLogger().warning("Skipping invalid recipe definition: " + definition.key());
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, definition.key());
        ItemStack result = new ItemStack(definition.resultMaterial(), Math.max(1, definition.resultAmount()));
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00A7b" + definition.displayName());
            meta.setLore(definition.lore().stream().map(line -> "\u00A77" + line).toList());
            result.setItemMeta(meta);
        }

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                definition.shape().get(0),
                definition.shape().get(1),
                definition.shape().get(2)
        );
        for (Map.Entry<Character, Material> ingredient : definition.ingredients().entrySet()) {
            if (ingredient.getValue() == null || ingredient.getValue() == Material.AIR) {
                continue;
            }
            recipe.setIngredient(ingredient.getKey(), ingredient.getValue());
        }

        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
        requirements.put(key, new RecipeRequirement(definition.jobType(), definition.key()));
    }

    public record RecipeRequirement(JobType jobType, String recipeKey) {
    }
}
