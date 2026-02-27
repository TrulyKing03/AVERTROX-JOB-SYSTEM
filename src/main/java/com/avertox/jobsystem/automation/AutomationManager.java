package com.avertox.jobsystem.automation;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.data.MySqlManager;
import com.avertox.jobsystem.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutomationManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MySqlManager mySqlManager;
    private final Map<String, AutomationBlock> blocks = new HashMap<>();
    private final Map<String, Integer> elapsedSeconds = new HashMap<>();
    private BukkitTask task;

    public AutomationManager(JavaPlugin plugin, ConfigManager configManager, MySqlManager mySqlManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.mySqlManager = mySqlManager;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (AutomationBlock block : blocks.values()) {
                processGeneration(block);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        saveAll();
    }

    public boolean canPlace(UUID player, JobType type) {
        long count = blocks.values().stream()
                .filter(b -> b.getOwner().equals(player) && b.getJobType() == type)
                .count();
        return count < configManager.getMaxAutomationBlocks(type);
    }

    public AutomationBlock create(UUID owner, JobType type, Location location) {
        String key = MySqlManager.serializeLocation(location);
        AutomationBlock block = switch (type) {
            case FARMER -> new AutoFarmBlock(owner, key, 1);
            case FISHER -> new AutoFishBlock(owner, key, 1);
            case WOODCUTTER -> new AutoWoodBlock(owner, key, 1);
            case MINER -> new AutoMineBlock(owner, key, 1);
            case HUNTER -> new AutoHunterBlock(owner, key, 1);
        };
        block.configureSlots(configManager.getAutomationSlotBase(), configManager.getAutomationSlotsPerLevel());
        blocks.put(key, block);
        elapsedSeconds.put(key, 0);
        mySqlManager.saveAutomation(owner, type, location, 1, "");
        return block;
    }

    public AutomationBlock getByLocation(Location location) {
        return blocks.get(MySqlManager.serializeLocation(location));
    }

    public boolean upgrade(AutomationBlock block) {
        int maxLevel = Math.max(1, configManager.getAutomationMaxLevel());
        if (block.getLevel() >= maxLevel) {
            return false;
        }
        block.setLevel(block.getLevel() + 1);
        elapsedSeconds.put(block.getLocationKey(), 0);
        mySqlManager.saveAutomation(
                block.getOwner(),
                block.getJobType(),
                block.getLocationKey(),
                block.getLevel(),
                serializeStorage(block.getStorage())
        );
        return true;
    }

    public double upgradeCost(JobType type, int nextLevel) {
        double configured = configManager.getAutomationUpgradeCost(type, nextLevel);
        if (configured > 0.0D) {
            return configured;
        }
        return 300.0D * nextLevel * nextLevel;
    }

    public int getMaxLevel() {
        return Math.max(1, configManager.getAutomationMaxLevel());
    }

    public int getGenerationIntervalSeconds(AutomationBlock block) {
        int base = Math.max(1, configManager.getAutomationGenerationSeconds(block.getJobType()));
        int speedBonus = Math.max(0, configManager.getAutomationSpeedUpgradeSeconds());
        return Math.max(1, base - ((Math.max(1, block.getLevel()) - 1) * speedBonus));
    }

    public int getSecondsUntilNextGeneration(AutomationBlock block) {
        int interval = getGenerationIntervalSeconds(block);
        int elapsed = elapsedSeconds.getOrDefault(block.getLocationKey(), 0);
        return Math.max(0, interval - elapsed);
    }

    public List<ItemStack> collectItems(AutomationBlock block) {
        List<ItemStack> items = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : block.getStorage().entrySet()) {
            int remaining = entry.getValue();
            while (remaining > 0) {
                int stackSize = Math.min(64, remaining);
                items.add(new ItemStack(entry.getKey(), stackSize));
                remaining -= stackSize;
            }
        }
        block.getStorage().clear();
        return items;
    }

    public List<ItemStack> previewItems(AutomationBlock block) {
        List<ItemStack> items = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : block.getStorage().entrySet()) {
            items.add(new ItemStack(entry.getKey(), Math.min(64, entry.getValue())));
        }
        return items;
    }

    public void giveCollectedItems(Player player, AutomationBlock block) {
        for (ItemStack stack : collectItems(block)) {
            player.getInventory().addItem(stack);
        }
    }

    public void loadPlayer(UUID uuid) {
        for (JobType type : JobType.values()) {
            Map<String, Integer> loaded = mySqlManager.loadAutomation(uuid, type);
            for (Map.Entry<String, Integer> entry : loaded.entrySet()) {
                String locationKey = entry.getKey();
                int level = entry.getValue();
                AutomationBlock block = switch (type) {
                    case FARMER -> new AutoFarmBlock(uuid, locationKey, level);
                    case FISHER -> new AutoFishBlock(uuid, locationKey, level);
                    case WOODCUTTER -> new AutoWoodBlock(uuid, locationKey, level);
                    case MINER -> new AutoMineBlock(uuid, locationKey, level);
                    case HUNTER -> new AutoHunterBlock(uuid, locationKey, level);
                };
                block.configureSlots(configManager.getAutomationSlotBase(), configManager.getAutomationSlotsPerLevel());
                blocks.put(locationKey, block);
                elapsedSeconds.put(locationKey, 0);
            }
        }
    }

    public void saveAll() {
        for (AutomationBlock block : blocks.values()) {
            mySqlManager.saveAutomation(
                    block.getOwner(),
                    block.getJobType(),
                    block.getLocationKey(),
                    block.getLevel(),
                    serializeStorage(block.getStorage())
            );
        }
    }

    public Location parseLocation(String locationKey) {
        try {
            String[] split = locationKey.split(":");
            World world = Bukkit.getWorld(split[0]);
            if (world == null) {
                return null;
            }
            int x = Integer.parseInt(split[1]);
            int y = Integer.parseInt(split[2]);
            int z = Integer.parseInt(split[3]);
            return new Location(world, x, y, z);
        } catch (Exception ex) {
            return null;
        }
    }

    private String serializeStorage(Map<Material, Integer> storage) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : storage.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    private void processGeneration(AutomationBlock block) {
        String key = block.getLocationKey();
        int elapsed = elapsedSeconds.getOrDefault(key, 0) + 1;
        int interval = getGenerationIntervalSeconds(block);
        if (elapsed >= interval) {
            if (!generateConfiguredOutput(block)) {
                block.generateTick();
            }
            elapsed = 0;
        }
        elapsedSeconds.put(key, elapsed);
    }

    private boolean generateConfiguredOutput(AutomationBlock block) {
        Map<Material, Integer> outputs = configManager.getGeneratorOutputs(block.getJobType());
        if (outputs.isEmpty()) {
            return false;
        }
        int level = Math.max(1, block.getLevel());
        for (Map.Entry<Material, Integer> entry : outputs.entrySet()) {
            int base = Math.max(0, entry.getValue());
            if (base <= 0) {
                continue;
            }
            block.addResource(entry.getKey(), base * level);
        }
        return true;
    }
}
