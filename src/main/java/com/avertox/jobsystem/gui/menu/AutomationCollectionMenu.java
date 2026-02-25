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
    private final AutomationManager automationManager;
    private final AutomationBlock block;
    private final Inventory inventory;

    public AutomationCollectionMenu(AutomationManager automationManager, AutomationBlock block) {
        this.automationManager = automationManager;
        this.block = block;
        this.inventory = Bukkit.createInventory(null, 27, "Automation Collection");
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
        if (event.getRawSlot() == 22) {
            automationManager.giveCollectedItems(player, block);
            player.sendMessage("§aCollected automation resources.");
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        int slot = 10;
        for (ItemStack stack : automationManager.previewItems(block)) {
            if (slot < 17) {
                inventory.setItem(slot++, stack);
            }
        }
        inventory.setItem(22, MenuUtil.item(Material.HOPPER, "§aCollect", List.of("§7Click to claim stored resources.")));
    }
}
