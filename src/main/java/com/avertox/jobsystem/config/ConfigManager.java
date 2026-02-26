package com.avertox.jobsystem.config;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {
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
        return config.getDouble("rewards." + type.key() + "." + key, 0.0D);
    }

    public double getSellPrice(Material material) {
        return config.getDouble("sell_prices." + material.name(), -1.0D);
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
