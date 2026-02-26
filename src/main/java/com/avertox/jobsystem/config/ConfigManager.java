package com.avertox.jobsystem.config;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {
    private static final Map<Material, Double> DEFAULT_SELL_PRICES = createDefaultSellPrices();

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

    public int getAutomationGenerationSeconds(JobType type) {
        return config.getInt("automation.base_generation_seconds." + type.key(), 30);
    }

    public int getMaxAutomationBlocks(JobType type) {
        return config.getInt("automation.max_blocks_per_player." + type.key(), 3);
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
                case "kill_money" -> 7.0D;
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
