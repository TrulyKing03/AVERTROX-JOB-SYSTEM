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
import java.util.List;
import java.util.Map;

public class CustomCraftingManager {
    private final JavaPlugin plugin;
    private final Map<NamespacedKey, RecipeRequirement> requirements = new HashMap<>();

    public CustomCraftingManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        registerFarmerRecipes();
        registerFisherRecipes();
        registerWoodRecipes();
        registerMinerRecipes();
    }

    public Map<NamespacedKey, RecipeRequirement> getRequirements() {
        return requirements;
    }

    private void registerFarmerRecipes() {
        registerSimple(
                "nutrient_stew",
                JobType.FARMER,
                Material.MUSHROOM_STEW,
                "Nutrient Stew",
                List.of("Hunger bonus"),
                "WWW", "CBC", "WWW",
                Map.of('W', Material.WHEAT, 'C', Material.CARROT, 'B', Material.BOWL)
        );
        registerSimple(
                "harvester_bread",
                JobType.FARMER,
                Material.BREAD,
                "Harvester Bread",
                List.of("Efficiency bonus"),
                "WWW", "W W", "WWW",
                Map.of('W', Material.WHEAT)
        );
    }

    private void registerFisherRecipes() {
        registerSimple(
                "xp_bait",
                JobType.FISHER,
                Material.STRING,
                "XP Bait",
                List.of("Fishing XP bonus"),
                " FS", "SRS", " SF",
                Map.of('F', Material.COD, 'S', Material.STRING, 'R', Material.REDSTONE)
        );
        registerSimple(
                "legend_lure",
                JobType.FISHER,
                Material.HEART_OF_THE_SEA,
                "Legend Lure",
                List.of("Rare fish chance bonus"),
                "GEG", "ERE", "GEG",
                Map.of('G', Material.GOLD_INGOT, 'E', Material.ENDER_PEARL, 'R', Material.FISHING_ROD)
        );
    }

    private void registerWoodRecipes() {
        registerSimple(
                "resin_plank",
                JobType.WOODCUTTER,
                Material.OAK_PLANKS,
                "Resin Plank",
                List.of("Job component"),
                "RLR", "L L", "RLR",
                Map.of('R', Material.SLIME_BALL, 'L', Material.OAK_LOG)
        );
        registerSimple(
                "reinforced_handle",
                JobType.WOODCUTTER,
                Material.STICK,
                "Reinforced Handle",
                List.of("Tool upgrade component"),
                " I ", " S ", " S ",
                Map.of('I', Material.IRON_INGOT, 'S', Material.STICK)
        );
    }

    private void registerMinerRecipes() {
        registerSimple(
                "light_alloy",
                JobType.MINER,
                Material.IRON_INGOT,
                "Light Alloy",
                List.of("Upgrade component"),
                "ICI", "CGC", "ICI",
                Map.of('I', Material.IRON_INGOT, 'C', Material.COPPER_INGOT, 'G', Material.GOLD_INGOT)
        );
        registerSimple(
                "vein_core",
                JobType.MINER,
                Material.DIAMOND,
                "Vein Core",
                List.of("Vein mining booster"),
                "ODO", "DED", "ODO",
                Map.of('O', Material.OBSIDIAN, 'D', Material.DIAMOND, 'E', Material.EMERALD)
        );
    }

    private void registerSimple(
            String id,
            JobType type,
            Material resultMaterial,
            String name,
            List<String> lore,
            String row1, String row2, String row3,
            Map<Character, Material> ingredients
    ) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        ItemStack result = new ItemStack(resultMaterial, 1);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b" + name);
            meta.setLore(lore.stream().map(s -> "§7" + s).toList());
            result.setItemMeta(meta);
        }
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(row1, row2, row3);
        ingredients.forEach(recipe::setIngredient);
        Bukkit.addRecipe(recipe);
        requirements.put(key, new RecipeRequirement(type, id));
    }

    public record RecipeRequirement(JobType jobType, String recipeKey) {}
}
