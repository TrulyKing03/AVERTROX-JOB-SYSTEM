package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.automation.AutomationBlock;
import com.avertox.jobsystem.automation.AutomationManager;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AutomationCollectionMenu implements BaseMenu {
    private static final int[] STORAGE_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};

    private final AutomationManager automationManager;
    private final AutomationBlock block;
    private final Inventory inventory;

    public AutomationCollectionMenu(AutomationManager automationManager, AutomationBlock block) {
        this.automationManager = automationManager;
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
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() == 40) {
            automationManager.giveCollectedItems(player, block);
            player.sendMessage("§aCollected automation resources.");
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.GREEN_STAINED_GLASS_PANE, "§2");

        int storedCount = block.getStorage().values().stream().mapToInt(Integer::intValue).sum();
        inventory.setItem(4, MenuUtil.item(Material.BEACON, "§a§lAutomation Vault", List.of(
                "§7Job: §f" + block.getJobType(),
                "§7Tier: §f" + block.getLevel(),
                "§7Stored Items: §f" + storedCount,
                "§7Capacity: §f" + block.getCapacity()
        )));

        int idx = 0;
        for (ItemStack stack : automationManager.previewItems(block)) {
            if (idx >= STORAGE_SLOTS.length) {
                break;
            }
            inventory.setItem(STORAGE_SLOTS[idx++], stack);
        }

        inventory.setItem(40, MenuUtil.item(Material.HOPPER, "§a§lCollect All", List.of(
                "§7Transfer all generated items",
                "§7directly to your inventory."
        )));
    }
}
