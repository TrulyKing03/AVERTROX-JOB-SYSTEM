package com.avertox.jobsystem.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MenuUtil {
    private MenuUtil() {
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack glass(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static void frame( org.bukkit.inventory.Inventory inventory, Material pane, String label) {
        int size = inventory.getSize();
        int rows = size / 9;
        ItemStack frameItem = glass(pane, label);
        for (int col = 0; col < 9; col++) {
            inventory.setItem(col, frameItem);
            inventory.setItem((rows - 1) * 9 + col, frameItem);
        }
        for (int row = 0; row < rows; row++) {
            inventory.setItem(row * 9, frameItem);
            inventory.setItem(row * 9 + 8, frameItem);
        }
    }
}
