package com.avertox.jobsystem.config;

import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.recipes.ConfiguredRecipeDefinition;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private static final Map<Material, Double> DEFAULT_SELL_PRICES = createDefaultSellPrices();
    private static final Map<JobType, Material> DEFAULT_GENERATOR_BLOCKS = createDefaultGeneratorBlocks();
    private static final Map<JobType, Map<Material, Integer>> DEFAULT_GENERATOR_OUTPUTS = createDefaultGeneratorOutputs();
    private static final Map<JobType, Set<Material>> DEFAULT_JOB_SELL_MATERIALS = createDefaultJobSellMaterials();
    private static final List<ConfiguredRecipeDefinition> DEFAULT_RECIPES = createDefaultRecipes();

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public List<Integer> getLevelThresholds(JobType type) {
        List<Integer> list = config.getIntegerList("jobs." + type.key() + ".level_thresholds");
        return list.isEmpty() ? List.of(0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200) : list;
    }

    public double getUpgradeCost(JobType type, String upgradeKey) {
        return config.getDouble("jobs." + type.key() + ".upgrade_costs." + upgradeKey, -1.0D);
    }

    public int getRegrowthSeconds(JobType type) {
        return config.getInt("jobs." + type.key() + ".regrowth_seconds", 30);
    }

    public double getTntAutoHarvestChance() {
        return config.getDouble("jobs.farmer.tnt_auto_harvest_chance", 0.10D);
    }

    public boolean isFarmerRegrowthAreaRestrictionEnabled() {
        return config.getBoolean("jobs.farmer.regrowth_restrictions.enabled", true);
    }

    public int getFarmerCitySpawnRadiusBlocks() {
        return Math.max(0, config.getInt("jobs.farmer.regrowth_restrictions.city_spawn_radius_blocks", 256));
    }

    public int getFarmerVillageSearchRadiusBlocks() {
        return Math.max(0, config.getInt("jobs.farmer.regrowth_restrictions.village_search_radius_blocks", 384));
    }

    public int getAutomationGenerationSeconds(JobType type) {
        return config.getInt("automation.base_generation_seconds." + type.key(), 30);
    }

    public int getMaxAutomationBlocks(JobType type) {
        return config.getInt("automation.max_blocks_per_player." + type.key(), 3);
    }

    public int getAutomationRequiredJobLevel(JobType type) {
        int maxJobLevel = Math.max(1, getLevelThresholds(type).size());
        int perJob = config.getInt("automation.required_job_level_by_job." + type.key(), -1);
        if (perJob > 0) {
            return Math.min(maxJobLevel, perJob);
        }
        int global = config.getInt("automation.required_job_level", -1);
        if (global > 0) {
            return Math.min(maxJobLevel, global);
        }
        return maxJobLevel;
    }

    public Material getGeneratorBlock(JobType type) {
        String raw = config.getString("automation.generator_blocks." + type.key(), null);
        if (raw != null) {
            Material parsed = Material.matchMaterial(raw.trim());
            if (parsed != null && parsed.isBlock()) {
                return parsed;
            }
        }
        return DEFAULT_GENERATOR_BLOCKS.get(type);
    }

    public JobType resolveGeneratorJob(Material blockType) {
        for (JobType type : JobType.values()) {
            Material configured = getGeneratorBlock(type);
            if (configured == blockType) {
                return type;
            }
        }
        return null;
    }

    public Map<Material, Integer> getGeneratorOutputs(JobType type) {
        ConfigurationSection section = config.getConfigurationSection("automation.generator_outputs." + type.key());
        Map<Material, Integer> parsed = new EnumMap<>(Material.class);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key.trim());
                if (material == null) {
                    continue;
                }
                int amount = Math.max(0, section.getInt(key, 0));
                if (amount > 0) {
                    parsed.put(material, amount);
                }
            }
        }
        if (!parsed.isEmpty()) {
            return parsed;
        }
        Map<Material, Integer> defaults = DEFAULT_GENERATOR_OUTPUTS.get(type);
        if (defaults == null) {
            return new EnumMap<>(Material.class);
        }
        return new EnumMap<>(defaults);
    }

    public Sound getAutomationTickSound() {
        return parseSound("automation.sounds.tick", Sound.BLOCK_NOTE_BLOCK_HAT);
    }

    public Sound getAutomationCompleteSound() {
        return parseSound("automation.sounds.complete", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public float getAutomationTickVolume() {
        return (float) config.getDouble("automation.sounds.tick_volume", 0.35D);
    }

    public float getAutomationTickPitch() {
        return (float) config.getDouble("automation.sounds.tick_pitch", 1.35D);
    }

    public float getAutomationCompleteVolume() {
        return (float) config.getDouble("automation.sounds.complete_volume", 0.60D);
    }

    public float getAutomationCompletePitch() {
        return (float) config.getDouble("automation.sounds.complete_pitch", 1.15D);
    }

    public int getAutosaveMinutes() {
        return config.getInt("autosave_minutes", 5);
    }

    public double getReward(JobType type, String key) {
        String path = "rewards." + type.key() + "." + key;
        if (config.isSet(path)) {
            return config.getDouble(path, 0.0D);
        }
        return defaultReward(type, key);
    }

    public double getSellPrice(Material material) {
        String exactPath = "sell_prices." + material.name();
        if (config.isSet(exactPath)) {
            return config.getDouble(exactPath, -1.0D);
        }
        String lowercasePath = "sell_prices." + material.name().toLowerCase(Locale.ROOT);
        if (config.isSet(lowercasePath)) {
            return config.getDouble(lowercasePath, -1.0D);
        }

        String alias = legacySellAlias(material);
        if (alias != null) {
            String aliasPath = "sell_prices." + alias;
            if (config.isSet(aliasPath)) {
                return config.getDouble(aliasPath, -1.0D);
            }
            String lowercaseAliasPath = "sell_prices." + alias.toLowerCase(Locale.ROOT);
            if (config.isSet(lowercaseAliasPath)) {
                return config.getDouble(lowercaseAliasPath, -1.0D);
            }
        }

        return DEFAULT_SELL_PRICES.getOrDefault(material, -1.0D);
    }

    public Set<Material> getSellableMaterials(JobType type) {
        List<String> configured = config.getStringList("job_sell_materials." + type.key());
        Set<Material> parsed = new HashSet<>();
        for (String raw : configured) {
            Material material = Material.matchMaterial(raw == null ? "" : raw.trim());
            if (material != null) {
                parsed.add(material);
            }
        }
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return DEFAULT_JOB_SELL_MATERIALS.getOrDefault(type, Set.of());
    }

    public List<ConfiguredRecipeDefinition> getConfiguredRecipes() {
        ConfigurationSection root = config.getConfigurationSection("recipes");
        if (root == null) {
            return DEFAULT_RECIPES;
        }

        List<ConfiguredRecipeDefinition> recipes = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String rawJob = section.getString("job", "");
            JobType jobType;
            try {
                jobType = JobType.valueOf(rawJob.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping recipe '" + key + "': invalid job '" + rawJob + "'");
                continue;
            }

            int requiredLevel = Math.max(1, section.getInt("required_level", 1));

            ConfigurationSection resultSection = section.getConfigurationSection("result");
            if (resultSection == null) {
                plugin.getLogger().warning("Skipping recipe '" + key + "': missing result section");
                continue;
            }
            Material resultMaterial = Material.matchMaterial(resultSection.getString("material", "").trim());
            if (resultMaterial == null || resultMaterial == Material.AIR) {
                plugin.getLogger().warning("Skipping recipe '" + key + "': invalid result material");
                continue;
            }
            int resultAmount = Math.max(1, resultSection.getInt("amount", 1));
            String displayName = resultSection.getString("name", key);
            List<String> lore = resultSection.getStringList("lore");

            List<String> shape = section.getStringList("shape");
            if (shape.size() != 3) {
                plugin.getLogger().warning("Skipping recipe '" + key + "': shape must have exactly 3 rows");
                continue;
            }

            ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
            if (ingredientsSection == null) {
                plugin.getLogger().warning("Skipping recipe '" + key + "': missing ingredients section");
                continue;
            }
            Map<Character, Material> ingredients = new HashMap<>();
            for (String ingredientKey : ingredientsSection.getKeys(false)) {
                if (ingredientKey.length() != 1) {
                    plugin.getLogger().warning("Skipping ingredient '" + ingredientKey + "' in recipe '" + key + "': key must be 1 char");
                    continue;
                }
                Material ingredientMaterial = Material.matchMaterial(ingredientsSection.getString(ingredientKey, "").trim());
                if (ingredientMaterial == null || ingredientMaterial == Material.AIR) {
                    plugin.getLogger().warning("Skipping ingredient '" + ingredientKey + "' in recipe '" + key + "': invalid material");
                    continue;
                }
                ingredients.put(ingredientKey.charAt(0), ingredientMaterial);
            }
            if (ingredients.isEmpty()) {
                plugin.getLogger().warning("Skipping recipe '" + key + "': no valid ingredients");
                continue;
            }

            recipes.add(new ConfiguredRecipeDefinition(
                    key,
                    jobType,
                    requiredLevel,
                    resultMaterial,
                    resultAmount,
                    displayName,
                    lore,
                    shape,
                    ingredients
            ));
        }

        return recipes.isEmpty() ? DEFAULT_RECIPES : recipes;
    }

    public double getEconomyMultiplierDefault() {
        return config.getDouble("economy.multiplier.default", 1.0D);
    }

    public double getEconomyMultiplierHighLevel() {
        return config.getDouble("economy.multiplier.high_level", 1.5D);
    }

    public int getEconomyHighLevelThreshold() {
        return config.getInt("economy.multiplier.high_level_threshold", 8);
    }

    private String legacySellAlias(Material material) {
        return switch (material) {
            case COD -> "RAW_COD";
            case SALMON -> "RAW_SALMON";
            case BEEF -> "RAW_BEEF";
            case CHICKEN -> "RAW_CHICKEN";
            case MUTTON -> "RAW_MUTTON";
            case RABBIT -> "RAW_RABBIT";
            default -> null;
        };
    }

    private static Map<Material, Double> createDefaultSellPrices() {
        Map<Material, Double> map = new EnumMap<>(Material.class);

        // Farmer
        putPrice(map, 2.0D, Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.SUGAR_CANE);
        putPrice(map, 3.0D, Material.NETHER_WART, Material.APPLE);
        putPrice(map, 4.0D, Material.PUMPKIN);
        putPrice(map, 1.5D, Material.MELON_SLICE);
        putPrice(map, 2.5D, Material.SWEET_BERRIES, Material.GLOW_BERRIES, Material.CACTUS);
        putPrice(map, 2.0D, Material.COCOA_BEANS, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM);
        putPrice(map, 1.2D, Material.BAMBOO, Material.KELP);
        putPrice(map, 2.0D, Material.DRIED_KELP);

        // Fisher
        putPrice(map, 4.0D, Material.COD, Material.INK_SAC);
        putPrice(map, 6.0D, Material.SALMON);
        putPrice(map, 10.0D, Material.PUFFERFISH);
        putPrice(map, 18.0D, Material.TROPICAL_FISH);
        putPrice(map, 40.0D, Material.NAUTILUS_SHELL);
        putPrice(map, 35.0D, Material.SADDLE, Material.NAME_TAG);
        putPrice(map, 45.0D, Material.ENCHANTED_BOOK);
        putPrice(map, 20.0D, Material.BOW);
        putPrice(map, 25.0D, Material.FISHING_ROD);
        putPrice(map, 1.0D, Material.BOWL);
        putPrice(map, 0.8D, Material.STICK);

        // Woodcutter
        putPrice(map, 2.5D,
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
                Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.CRIMSON_STEM, Material.WARPED_STEM
        );
        putPrice(map, 2.2D,
                Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
                Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_STEM
        );
        putPrice(map, 2.5D,
                Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD, Material.JUNGLE_WOOD,
                Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
                Material.CRIMSON_HYPHAE, Material.WARPED_HYPHAE
        );
        putPrice(map, 2.2D,
                Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_JUNGLE_WOOD,
                Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD, Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
                Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_WARPED_HYPHAE
        );
        putPrice(map, 1.4D,
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
                Material.CRIMSON_PLANKS, Material.WARPED_PLANKS
        );
        putPrice(map, 1.0D,
                Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING,
                Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
        );

        // Miner
        putPrice(map, 4.0D, Material.COAL, Material.QUARTZ);
        putPrice(map, 3.0D, Material.REDSTONE, Material.LAPIS_LAZULI);
        putPrice(map, 8.0D, Material.RAW_IRON);
        putPrice(map, 5.0D, Material.RAW_COPPER);
        putPrice(map, 10.0D, Material.RAW_GOLD);
        putPrice(map, 30.0D, Material.DIAMOND);
        putPrice(map, 26.0D, Material.EMERALD);
        putPrice(map, 1.2D, Material.COBBLESTONE, Material.COBBLED_DEEPSLATE);
        putPrice(map, 2.0D, Material.AMETHYST_SHARD, Material.FLINT, Material.CLAY_BALL);
        putPrice(map, 60.0D, Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        putPrice(map, 1.0D, Material.GOLD_NUGGET, Material.IRON_NUGGET);
        putPrice(map, 9.0D, Material.IRON_INGOT);
        putPrice(map, 6.0D, Material.COPPER_INGOT);
        putPrice(map, 11.0D, Material.GOLD_INGOT);

        // Hunter
        putPrice(map, 3.5D, Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.MUTTON, Material.RABBIT);
        putPrice(map, 5.0D, Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN, Material.COOKED_MUTTON, Material.COOKED_RABBIT);
        putPrice(map, 4.0D, Material.LEATHER);
        putPrice(map, 2.0D, Material.BONE, Material.STRING);
        putPrice(map, 1.5D, Material.ROTTEN_FLESH, Material.ARROW);
        putPrice(map, 2.5D, Material.SPIDER_EYE, Material.FEATHER, Material.RABBIT_HIDE);
        putPrice(map, 9.0D, Material.GUNPOWDER, Material.SLIME_BALL, Material.MAGMA_CREAM);
        putPrice(map, 15.0D, Material.ENDER_PEARL, Material.BLAZE_ROD, Material.PHANTOM_MEMBRANE);
        putPrice(map, 20.0D, Material.GHAST_TEAR, Material.PRISMARINE_CRYSTALS);
        putPrice(map, 8.0D, Material.PRISMARINE_SHARD);
        putPrice(map, 45.0D, Material.SHULKER_SHELL);
        putPrice(map, 25.0D, Material.RABBIT_FOOT);

        return map;
    }

    private static void putPrice(Map<Material, Double> map, double value, Material... materials) {
        for (Material material : materials) {
            map.put(material, value);
        }
    }

    private static Map<JobType, Material> createDefaultGeneratorBlocks() {
        Map<JobType, Material> map = new EnumMap<>(JobType.class);
        map.put(JobType.FARMER, Material.HAY_BLOCK);
        map.put(JobType.FISHER, Material.BARREL);
        map.put(JobType.WOODCUTTER, Material.OAK_WOOD);
        map.put(JobType.MINER, Material.BLAST_FURNACE);
        map.put(JobType.HUNTER, Material.TARGET);
        return map;
    }

    private static Map<JobType, Map<Material, Integer>> createDefaultGeneratorOutputs() {
        Map<JobType, Map<Material, Integer>> map = new EnumMap<>(JobType.class);

        Map<Material, Integer> farmer = new EnumMap<>(Material.class);
        farmer.put(Material.WHEAT, 1);
        map.put(JobType.FARMER, farmer);

        Map<Material, Integer> fisher = new EnumMap<>(Material.class);
        fisher.put(Material.COD, 1);
        map.put(JobType.FISHER, fisher);

        Map<Material, Integer> wood = new EnumMap<>(Material.class);
        wood.put(Material.OAK_LOG, 1);
        map.put(JobType.WOODCUTTER, wood);

        Map<Material, Integer> miner = new EnumMap<>(Material.class);
        miner.put(Material.RAW_IRON, 1);
        map.put(JobType.MINER, miner);

        Map<Material, Integer> hunter = new EnumMap<>(Material.class);
        hunter.put(Material.BONE, 1);
        hunter.put(Material.ARROW, 1);
        map.put(JobType.HUNTER, hunter);

        return map;
    }

    private static Map<JobType, Set<Material>> createDefaultJobSellMaterials() {
        Map<JobType, Set<Material>> map = new EnumMap<>(JobType.class);

        map.put(JobType.FARMER, Set.of(
                Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.NETHER_WART,
                Material.SUGAR_CANE, Material.PUMPKIN, Material.MELON_SLICE, Material.SWEET_BERRIES,
                Material.GLOW_BERRIES, Material.COCOA_BEANS, Material.CACTUS, Material.BAMBOO,
                Material.KELP, Material.DRIED_KELP, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.APPLE
        ));
        map.put(JobType.FISHER, Set.of(
                Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH, Material.INK_SAC,
                Material.NAUTILUS_SHELL, Material.SADDLE, Material.NAME_TAG, Material.ENCHANTED_BOOK,
                Material.BOW, Material.FISHING_ROD, Material.BOWL
        ));
        map.put(JobType.WOODCUTTER, Set.of(
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG,
                Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM,
                Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
                Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_STEM, Material.OAK_WOOD, Material.SPRUCE_WOOD,
                Material.BIRCH_WOOD, Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD,
                Material.CHERRY_WOOD, Material.CRIMSON_HYPHAE, Material.WARPED_HYPHAE, Material.STRIPPED_OAK_WOOD,
                Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_ACACIA_WOOD,
                Material.STRIPPED_DARK_OAK_WOOD, Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
                Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_WARPED_HYPHAE, Material.OAK_PLANKS, Material.SPRUCE_PLANKS,
                Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
                Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.CRIMSON_PLANKS, Material.WARPED_PLANKS,
                Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING,
                Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
        ));
        map.put(JobType.MINER, Set.of(
                Material.COAL, Material.QUARTZ, Material.REDSTONE, Material.LAPIS_LAZULI, Material.RAW_IRON,
                Material.RAW_COPPER, Material.RAW_GOLD, Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT,
                Material.GOLD_NUGGET, Material.IRON_NUGGET, Material.DIAMOND, Material.EMERALD, Material.COBBLESTONE,
                Material.COBBLED_DEEPSLATE, Material.FLINT, Material.CLAY_BALL, Material.AMETHYST_SHARD,
                Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP
        ));
        map.put(JobType.HUNTER, Set.of(
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.MUTTON, Material.RABBIT, Material.COOKED_BEEF,
                Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN, Material.COOKED_MUTTON, Material.COOKED_RABBIT,
                Material.LEATHER, Material.FEATHER, Material.RABBIT_HIDE, Material.RABBIT_FOOT, Material.ROTTEN_FLESH,
                Material.BONE, Material.ARROW, Material.STRING, Material.SPIDER_EYE, Material.GUNPOWDER,
                Material.ENDER_PEARL, Material.BLAZE_ROD, Material.GHAST_TEAR, Material.SLIME_BALL,
                Material.MAGMA_CREAM, Material.PHANTOM_MEMBRANE, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS,
                Material.SHULKER_SHELL
        ));

        return map;
    }

    private static List<ConfiguredRecipeDefinition> createDefaultRecipes() {
        return List.of(
                recipe(
                        "nutrient_stew",
                        JobType.FARMER,
                        4,
                        Material.MUSHROOM_STEW,
                        1,
                        "Nutrient Stew",
                        List.of("Hunger bonus"),
                        List.of("WWW", "CBC", "WWW"),
                        Map.of('W', Material.WHEAT, 'C', Material.CARROT, 'B', Material.BOWL)
                ),
                recipe(
                        "harvester_bread",
                        JobType.FARMER,
                        7,
                        Material.BREAD,
                        1,
                        "Harvester Bread",
                        List.of("Efficiency bonus"),
                        List.of("WWW", "W W", "WWW"),
                        Map.of('W', Material.WHEAT)
                ),
                recipe(
                        "xp_bait",
                        JobType.FISHER,
                        4,
                        Material.STRING,
                        1,
                        "XP Bait",
                        List.of("Fishing XP bonus"),
                        List.of(" FS", "SRS", " SF"),
                        Map.of('F', Material.COD, 'S', Material.STRING, 'R', Material.REDSTONE)
                ),
                recipe(
                        "legend_lure",
                        JobType.FISHER,
                        8,
                        Material.HEART_OF_THE_SEA,
                        1,
                        "Legend Lure",
                        List.of("Rare fish chance bonus"),
                        List.of("GEG", "ERE", "GEG"),
                        Map.of('G', Material.GOLD_INGOT, 'E', Material.ENDER_PEARL, 'R', Material.FISHING_ROD)
                ),
                recipe(
                        "resin_plank",
                        JobType.WOODCUTTER,
                        5,
                        Material.OAK_PLANKS,
                        1,
                        "Resin Plank",
                        List.of("Job component"),
                        List.of("RLR", "L L", "RLR"),
                        Map.of('R', Material.SLIME_BALL, 'L', Material.OAK_LOG)
                ),
                recipe(
                        "reinforced_handle",
                        JobType.WOODCUTTER,
                        8,
                        Material.STICK,
                        1,
                        "Reinforced Handle",
                        List.of("Tool upgrade component"),
                        List.of(" I ", " S ", " S "),
                        Map.of('I', Material.IRON_INGOT, 'S', Material.STICK)
                ),
                recipe(
                        "light_alloy",
                        JobType.MINER,
                        5,
                        Material.IRON_INGOT,
                        1,
                        "Light Alloy",
                        List.of("Upgrade component"),
                        List.of("ICI", "CGC", "ICI"),
                        Map.of('I', Material.IRON_INGOT, 'C', Material.COPPER_INGOT, 'G', Material.GOLD_INGOT)
                ),
                recipe(
                        "vein_core",
                        JobType.MINER,
                        8,
                        Material.DIAMOND,
                        1,
                        "Vein Core",
                        List.of("Vein mining booster"),
                        List.of("ODO", "DED", "ODO"),
                        Map.of('O', Material.OBSIDIAN, 'D', Material.DIAMOND, 'E', Material.EMERALD)
                ),
                recipe(
                        "tracker_bait",
                        JobType.HUNTER,
                        4,
                        Material.RABBIT_STEW,
                        1,
                        "Tracker Bait",
                        List.of("Hunter tracking reagent"),
                        List.of(" M ", "RBR", " M "),
                        Map.of('M', Material.ROTTEN_FLESH, 'R', Material.RABBIT, 'B', Material.BOWL)
                ),
                recipe(
                        "predator_ration",
                        JobType.HUNTER,
                        8,
                        Material.COOKED_BEEF,
                        1,
                        "Predator Ration",
                        List.of("Hunting stamina ration"),
                        List.of("BBB", "RCR", "BBB"),
                        Map.of('B', Material.BEEF, 'R', Material.RABBIT, 'C', Material.COOKED_CHICKEN)
                )
        );
    }

    private static ConfiguredRecipeDefinition recipe(
            String key,
            JobType jobType,
            int requiredLevel,
            Material resultMaterial,
            int resultAmount,
            String displayName,
            List<String> lore,
            List<String> shape,
            Map<Character, Material> ingredients
    ) {
        return new ConfiguredRecipeDefinition(
                key,
                jobType,
                requiredLevel,
                resultMaterial,
                resultAmount,
                displayName,
                lore,
                shape,
                ingredients
        );
    }

    private Sound parseSound(String path, Sound fallback) {
        String raw = config.getString(path, fallback.name());
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private double defaultReward(JobType type, String key) {
        return switch (type) {
            case FARMER -> switch (key) {
                case "crop_xp" -> 8.0D;
                case "crop_money" -> 0.0D;
                default -> 0.0D;
            };
            case FISHER -> switch (key) {
                case "catch_xp" -> 10.0D;
                case "catch_money" -> 8.0D;
                default -> 0.0D;
            };
            case WOODCUTTER -> switch (key) {
                case "log_xp" -> 7.0D;
                case "log_money" -> 0.0D;
                default -> 0.0D;
            };
            case MINER -> switch (key) {
                case "ore_xp" -> 12.0D;
                case "ore_money" -> 0.0D;
                case "stone_xp" -> 4.0D;
                case "stone_money" -> 0.0D;
                default -> 0.0D;
            };
            case HUNTER -> switch (key) {
                case "kill_xp" -> 9.0D;
                case "kill_money" -> 0.0D;
                default -> 0.0D;
            };
        };
    }

    public int getAutomationMaxLevel() {
        return config.getInt("automation.max_level", 10);
    }

    public int getAutomationSlotBase() {
        return config.getInt("automation.slot_base", 6);
    }

    public int getAutomationSlotsPerLevel() {
        return config.getInt("automation.slot_per_level", 2);
    }

    public int getAutomationSpeedUpgradeSeconds() {
        return config.getInt("automation.speed_upgrade_seconds", 2);
    }

    public double getAutomationUpgradeCost(JobType type, int nextLevel) {
        return config.getDouble("automation.upgrade_costs." + type.key() + "." + nextLevel, -1.0D);
    }

    public Map<String, Object> getFishRarityRates() {
        ConfigurationSection section = config.getConfigurationSection("jobs.fisher.fish_rarity");
        return section == null ? Collections.emptyMap() : section.getValues(false);
    }

    public String getMysqlHost() {
        return config.getString("mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("mysql.database", "avertox_jobs").toLowerCase(Locale.ROOT);
    }

    public String getMysqlUsername() {
        return config.getString("mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("mysql.password", "password");
    }

    public int getMysqlPoolSize() {
        return config.getInt("mysql.pool_size", 10);
    }
}
