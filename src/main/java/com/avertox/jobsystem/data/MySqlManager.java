package com.avertox.jobsystem.data;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySqlManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public MySqlManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void connect() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(
                "jdbc:mysql://" + configManager.getMysqlHost() + ":" + configManager.getMysqlPort() + "/"
                        + configManager.getMysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true"
        );
        cfg.setUsername(configManager.getMysqlUsername());
        cfg.setPassword(configManager.getMysqlPassword());
        cfg.setMaximumPoolSize(configManager.getMysqlPoolSize());
        cfg.setPoolName("AvertoxJobSystemPool");
        dataSource = new HikariDataSource(cfg);
        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTables() {
        String jobs = "CREATE TABLE IF NOT EXISTS jobs_table (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "job VARCHAR(32) NOT NULL, " +
                "level INT NOT NULL, " +
                "xp DOUBLE NOT NULL, " +
                "recipes TEXT, " +
                "money_earned DOUBLE NOT NULL DEFAULT 0, " +
                "upgrades TEXT, " +
                "PRIMARY KEY (uuid, job)" +
                ")";

        String automation = "CREATE TABLE IF NOT EXISTS automation_table (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "job VARCHAR(32) NOT NULL, " +
                "block_location VARCHAR(255) NOT NULL, " +
                "level INT NOT NULL, " +
                "stored_items TEXT, " +
                "PRIMARY KEY (uuid, job, block_location)" +
                ")";

        try (Connection c = dataSource.getConnection();
             PreparedStatement a = c.prepareStatement(jobs);
             PreparedStatement b = c.prepareStatement(automation)) {
            a.executeUpdate();
            b.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to create tables: " + ex.getMessage());
        }
    }

    public Map<JobType, PlayerJobData> loadJobs(UUID uuid) {
        Map<JobType, PlayerJobData> data = new EnumMap<>(JobType.class);
        String sql = "SELECT job, level, xp, recipes, money_earned, upgrades FROM jobs_table WHERE uuid=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JobType type = JobType.valueOf(rs.getString("job").toUpperCase());
                PlayerJobData jobData = new PlayerJobData();
                jobData.setLevel(rs.getInt("level"));
                jobData.setXp(rs.getDouble("xp"));
                jobData.addMoneyEarned(rs.getDouble("money_earned"));
                parseCsvIntoSet(rs.getString("recipes"), jobData.getUnlockedRecipes());
                parseCsvIntoMap(rs.getString("upgrades"), jobData.getUpgrades());
                data.put(type, jobData);
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load jobs: " + ex.getMessage());
        }
        return data;
    }

    public void saveJobs(UUID uuid, Map<JobType, PlayerJobData> jobs) {
        String sql = "REPLACE INTO jobs_table(uuid, job, level, xp, recipes, money_earned, upgrades) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (Map.Entry<JobType, PlayerJobData> entry : jobs.entrySet()) {
                PlayerJobData data = entry.getValue();
                ps.setString(1, uuid.toString());
                ps.setString(2, entry.getKey().key());
                ps.setInt(3, data.getLevel());
                ps.setDouble(4, data.getXp());
                ps.setString(5, String.join(",", data.getUnlockedRecipes()));
                ps.setDouble(6, data.getMoneyEarned());
                ps.setString(7, mapToCsv(data.getUpgrades()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to save jobs: " + ex.getMessage());
        }
    }

    public Map<String, Integer> loadAutomation(UUID uuid, JobType type) {
        Map<String, Integer> locationsToLevels = new HashMap<>();
        String sql = "SELECT block_location, level FROM automation_table WHERE uuid=? AND job=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.key());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                locationsToLevels.put(rs.getString("block_location"), rs.getInt("level"));
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed loading automation: " + ex.getMessage());
        }
        return locationsToLevels;
    }

    public void saveAutomation(UUID uuid, JobType type, Location location, int level, String storedItems) {
        saveAutomation(uuid, type, serializeLocation(location), level, storedItems);
    }

    public void saveAutomation(UUID uuid, JobType type, String locationKey, int level, String storedItems) {
        String sql = "REPLACE INTO automation_table(uuid, job, block_location, level, stored_items) VALUES (?,?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.key());
            ps.setString(3, locationKey);
            ps.setInt(4, level);
            ps.setString(5, storedItems);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed saving automation: " + ex.getMessage());
        }
    }

    public static String serializeLocation(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void parseCsvIntoSet(String csv, java.util.Set<String> set) {
        if (csv == null || csv.isBlank()) return;
        for (String s : csv.split(",")) {
            if (!s.isBlank()) set.add(s.trim());
        }
    }

    private void parseCsvIntoMap(String csv, Map<String, Integer> map) {
        if (csv == null || csv.isBlank()) return;
        for (String part : csv.split(",")) {
            String[] split = part.split(":");
            if (split.length == 2) {
                try {
                    map.put(split[0], Integer.parseInt(split[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private String mapToCsv(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        return sb.toString();
    }
}
