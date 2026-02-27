package com.avertox.jobsystem.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuManager implements Listener {
    private final Map<UUID, BaseMenu> openMenus = new HashMap<>();

    public void open(Player player, BaseMenu menu) {
        BaseMenu existing = openMenus.get(player.getUniqueId());
        if (existing != null) {
            existing.onClose(player);
        }
        openMenus.put(player.getUniqueId(), menu);
        menu.open(player);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        if (!event.getView().getTopInventory().equals(menu.getInventory())) {
            return;
        }
        event.setCancelled(true);
        menu.handleClick(event);
        if (openMenus.get(player.getUniqueId()) != menu) {
            return;
        }
        menu.refresh(player);
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        if (!event.getView().getTopInventory().equals(menu.getInventory())) {
            return;
        }
        openMenus.remove(player.getUniqueId());
        menu.onClose(player);
    }
}
