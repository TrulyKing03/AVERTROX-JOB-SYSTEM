package com.avertox.jobsystem.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface BaseMenu {
    Inventory getInventory();

    void open(Player player);

    void handleClick(InventoryClickEvent event);

    void refresh(Player player);

    default void onClose(Player player) {
    }
}
