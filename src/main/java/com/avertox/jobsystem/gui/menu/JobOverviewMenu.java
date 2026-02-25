package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JobOverviewMenu implements BaseMenu {
    private final JobManager jobManager;
    private final JobToolService toolService;
    private final Inventory inventory;

    public JobOverviewMenu(JobManager jobManager, JobToolService toolService) {
        this.jobManager = jobManager;
        this.toolService = toolService;
        this.inventory = Bukkit.createInventory(null, 27, "Job Overview");
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
        JobType target = switch (event.getRawSlot()) {
            case 10 -> JobType.FARMER;
            case 12 -> JobType.FISHER;
            case 14 -> JobType.WOODCUTTER;
            case 16 -> JobType.MINER;
            default -> null;
        };
        if (target == null) {
            return;
        }
        JobManager.SwitchResult result = jobManager.activateJob(player.getUniqueId(), target);
        if (!result.success()) {
            long remaining = result.remainingMillis();
            long hours = TimeUnit.MILLISECONDS.toHours(remaining);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60L;
            player.sendMessage("§cYou can switch jobs again in " + hours + "h " + minutes + "m.");
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), target);
        toolService.grantCurrentTool(player, data, target);
        player.sendMessage("§aActive job set to " + target.name() + ". Tool granted.");
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        int[] slots = {10, 12, 14, 16};
        int idx = 0;
        JobType active = jobManager.getActiveJob(player.getUniqueId());
        for (JobType type : JobType.values()) {
            PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
            Material icon = switch (type) {
                case FARMER -> Material.WHEAT;
                case FISHER -> Material.FISHING_ROD;
                case WOODCUTTER -> Material.IRON_AXE;
                case MINER -> Material.IRON_PICKAXE;
            };
            List<String> lore = new ArrayList<>();
            lore.add("§7Level: §f" + data.getLevel());
            lore.add("§7XP: §f" + String.format("%.1f", data.getXp()));
            lore.add("§7Money Earned: §a" + String.format("%.2f", data.getMoneyEarned()));
            lore.add("§7Tool Tier: §f" + toolService.getToolTier(data, type));
            lore.add(active == type ? "§aACTIVE JOB" : "§eClick to learn/switch");
            ItemStack stack = MenuUtil.item(icon, "§e" + type.name(), lore);
            inventory.setItem(slots[idx++], stack);
        }
    }
}
