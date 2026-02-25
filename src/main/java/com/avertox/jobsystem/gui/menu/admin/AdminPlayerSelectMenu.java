package com.avertox.jobsystem.gui.menu.admin;

import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.tools.JobToolService;
import com.avertox.jobsystem.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminPlayerSelectMenu implements BaseMenu {
    private final MenuManager menuManager;
    private final JobManager jobManager;
    private final EconomyService economyService;
    private final JobToolService toolService;
    private final Inventory inventory;
    private final List<Player> targets = new ArrayList<>();

    public AdminPlayerSelectMenu(MenuManager menuManager, JobManager jobManager, EconomyService economyService, JobToolService toolService) {
        this.menuManager = menuManager;
        this.jobManager = jobManager;
        this.economyService = economyService;
        this.toolService = toolService;
        this.inventory = Bukkit.createInventory(null, 54, "Admin Panel - Select Player");
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
        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= 45 || raw >= targets.size()) {
            return;
        }
        Player target = targets.get(raw);
        if (target == null || !target.isOnline()) {
            admin.sendMessage("§cThat player is no longer online.");
            return;
        }
        menuManager.open(admin, new AdminPlayerManageMenu(menuManager, jobManager, economyService, toolService, target.getUniqueId()));
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        targets.clear();
        for (Player online : Bukkit.getOnlinePlayers()) {
            targets.add(online);
        }

        for (int i = 0; i < Math.min(45, targets.size()); i++) {
            Player target = targets.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§e" + target.getName());
                meta.setLore(List.of(
                        "§7Click to open admin controls",
                        "§7UUID: §f" + target.getUniqueId()
                ));
                head.setItemMeta(meta);
            }
            inventory.setItem(i, head);
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
    }
}
