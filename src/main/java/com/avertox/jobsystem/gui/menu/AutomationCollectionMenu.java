package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.automation.AutomationBlock;
import com.avertox.jobsystem.automation.AutomationManager;
import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;

public class AutomationCollectionMenu implements BaseMenu {
    private static final int[] STORAGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final JavaPlugin plugin;
    private final AutomationManager automationManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final AutomationBlock block;
    private final Inventory inventory;

    private BukkitTask refreshTask;
    private int lastStoredCount = -1;
    private int lastRemaining = -1;

    public AutomationCollectionMenu(
            JavaPlugin plugin,
            AutomationManager automationManager,
            EconomyService economyService,
            ConfigManager configManager,
            AutomationBlock block
    ) {
        this.plugin = plugin;
        this.automationManager = automationManager;
        this.economyService = economyService;
        this.configManager = configManager;
        this.block = block;
        this.inventory = Bukkit.createInventory(null, 45, "Automation Vault");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        refresh(player);
        player.openInventory(inventory);
        startAutoRefresh(player);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 40) {
            automationManager.giveCollectedItems(player, block);
            player.sendMessage("\u00A7aCollected automation resources.");
            return;
        }
        if (slot == 42) {
            int nextLevel = block.getLevel() + 1;
            if (nextLevel > automationManager.getMaxLevel()) {
                player.sendMessage("\u00A7cAutomation is already max level.");
                return;
            }
            double cost = automationManager.upgradeCost(block.getJobType(), nextLevel);
            if (!economyService.has(player, cost)) {
                player.sendMessage("\u00A7cInsufficient funds. Needed: $" + format(cost));
                return;
            }
            if (!economyService.withdraw(player, cost)) {
                player.sendMessage("\u00A7cTransaction failed.");
                return;
            }
            if (automationManager.upgrade(block)) {
                player.sendMessage("\u00A7aAutomation upgraded to level " + block.getLevel() + ".");
            } else {
                player.sendMessage("\u00A7cUpgrade failed.");
            }
        }
    }

    @Override
    public void refresh(Player player) {
        int remaining = automationManager.getSecondsUntilNextGeneration(block);
        int interval = automationManager.getGenerationIntervalSeconds(block);
        Material frame = frameForTimer(remaining, interval);

        inventory.clear();
        MenuUtil.frame(inventory, frame, "\u00A72");

        int storedCount = storedCount();
        inventory.setItem(4, MenuUtil.item(Material.BEACON, "\u00A7a\u00A7lAutomation Vault", List.of(
                "\u00A77Job: \u00A7f" + block.getJobType(),
                "\u00A77Level: \u00A7f" + block.getLevel() + "/" + automationManager.getMaxLevel(),
                "\u00A77Stored Items: \u00A7f" + storedCount + "/" + block.getCapacity(),
                "\u00A77Used Slots: \u00A7f" + block.getUsedSlots() + "/" + block.getSlotCapacity(),
                "\u00A77Output Interval: \u00A7f" + interval + "s",
                "\u00A77Next Output In: \u00A7e" + remaining + "s",
                "\u00A77Frame Color = timer animation"
        )));

        int visibleSlots = Math.min(STORAGE_SLOTS.length, block.getSlotCapacity());
        for (int i = 0; i < STORAGE_SLOTS.length; i++) {
            if (i >= visibleSlots) {
                inventory.setItem(STORAGE_SLOTS[i], MenuUtil.item(Material.GRAY_STAINED_GLASS_PANE, "\u00A78Locked Slot", List.of(
                        "\u00A77Upgrade automation to unlock more storage slots."
                )));
            }
        }

        int idx = 0;
        for (ItemStack stack : automationManager.previewItems(block)) {
            if (idx >= visibleSlots) {
                break;
            }
            inventory.setItem(STORAGE_SLOTS[idx++], stack);
        }

        int nextLevel = block.getLevel() + 1;
        if (nextLevel > automationManager.getMaxLevel()) {
            inventory.setItem(42, MenuUtil.item(Material.BARRIER, "\u00A7c\u00A7lMax Level", List.of(
                    "\u00A77This automation block cannot be upgraded further."
            )));
        } else {
            double upgradeCost = automationManager.upgradeCost(block.getJobType(), nextLevel);
            int nextInterval = Math.max(1, configManager.getAutomationGenerationSeconds(block.getJobType())
                    - ((nextLevel - 1) * configManager.getAutomationSpeedUpgradeSeconds()));
            int nextSlots = Math.max(1, configManager.getAutomationSlotBase()
                    + ((nextLevel - 1) * Math.max(0, configManager.getAutomationSlotsPerLevel())));
            inventory.setItem(42, MenuUtil.item(Material.ANVIL, "\u00A7e\u00A7lUpgrade Automation", List.of(
                    "\u00A77Next Level: \u00A7f" + nextLevel,
                    "\u00A77Cost: \u00A7a$" + format(upgradeCost),
                    "\u00A77Next Slots: \u00A7f" + nextSlots,
                    "\u00A77Next Interval: \u00A7f" + nextInterval + "s",
                    "\u00A7aClick to upgrade"
            )));
        }

        inventory.setItem(40, MenuUtil.item(Material.HOPPER, "\u00A7a\u00A7lCollect All", List.of(
                "\u00A77Transfer all generated items",
                "\u00A77directly to your inventory."
        )));
    }

    @Override
    public void onClose(Player player) {
        stopAutoRefresh();
    }

    private void startAutoRefresh(Player player) {
        stopAutoRefresh();
        lastStoredCount = storedCount();
        lastRemaining = automationManager.getSecondsUntilNextGeneration(block);

        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopAutoRefresh();
                return;
            }
            if (player.getOpenInventory() == null || !player.getOpenInventory().getTopInventory().equals(inventory)) {
                stopAutoRefresh();
                return;
            }

            int currentStored = storedCount();
            int currentRemaining = automationManager.getSecondsUntilNextGeneration(block);

            if (currentStored > lastStoredCount) {
                Sound complete = configManager.getAutomationCompleteSound();
                if (complete != null) {
                    player.playSound(player.getLocation(), complete, configManager.getAutomationCompleteVolume(), configManager.getAutomationCompletePitch());
                }
            } else {
                Sound tick = configManager.getAutomationTickSound();
                if (tick != null) {
                    player.playSound(player.getLocation(), tick, configManager.getAutomationTickVolume(), configManager.getAutomationTickPitch());
                }
            }

            if (currentStored != lastStoredCount || currentRemaining != lastRemaining) {
                refresh(player);
            }

            lastStoredCount = currentStored;
            lastRemaining = currentRemaining;
        }, 20L, 20L);
    }

    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private int storedCount() {
        return block.getStorage().values().stream().mapToInt(Integer::intValue).sum();
    }

    private Material frameForTimer(int remaining, int interval) {
        if (interval <= 0) {
            return Material.GREEN_STAINED_GLASS_PANE;
        }
        double pct = 1.0D - (Math.max(0, Math.min(interval, remaining)) / (double) interval);
        if (pct < 0.34D) {
            return Material.RED_STAINED_GLASS_PANE;
        }
        if (pct < 0.67D) {
            return Material.YELLOW_STAINED_GLASS_PANE;
        }
        return Material.GREEN_STAINED_GLASS_PANE;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
