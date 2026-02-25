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
        this.inventory = Bukkit.createInventory(null, 54, "Mythic Professions");
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
            case 19 -> JobType.FARMER;
            case 21 -> JobType.FISHER;
            case 23 -> JobType.WOODCUTTER;
            case 25 -> JobType.MINER;
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
        player.sendMessage("§aActive job set to " + target.name() + ". Your forged tool has been granted.");
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.BLACK_STAINED_GLASS_PANE, "§0");

        JobType active = jobManager.getActiveJob(player.getUniqueId());
        inventory.setItem(4, MenuUtil.item(Material.NETHER_STAR, "§6§lHall of Professions", List.of(
                "§7Choose your active profession.",
                "§7Only one can be active at a time.",
                "§7Switching starts a 24h cooldown."
        )));

        placeJobCard(player, JobType.FARMER, Material.GOLDEN_HOE, 19, active);
        placeJobCard(player, JobType.FISHER, Material.FISHING_ROD, 21, active);
        placeJobCard(player, JobType.WOODCUTTER, Material.GOLDEN_AXE, 23, active);
        placeJobCard(player, JobType.MINER, Material.GOLDEN_PICKAXE, 25, active);

        long remaining = jobManager.getRemainingSwitchCooldownMillis(player.getUniqueId());
        long h = TimeUnit.MILLISECONDS.toHours(remaining);
        long m = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60L;
        inventory.setItem(49, MenuUtil.item(Material.CLOCK, "§bSwitch Cooldown", List.of(
                "§7Remaining: §f" + h + "h " + m + "m",
                "§7Cooldown starts when switching",
                "§7to a different profession."
        )));
    }

    private void placeJobCard(Player player, JobType type, Material icon, int slot, JobType active) {
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
        List<String> lore = new ArrayList<>();
        lore.add("§7Level: §f" + data.getLevel());
        lore.add("§7XP: §f" + String.format("%.1f", data.getXp()));
        lore.add("§7Money Earned: §a$" + String.format("%.2f", data.getMoneyEarned()));
        lore.add("§7Tool Tier: §f" + toolService.getToolTier(data, type));
        lore.add("§8");
        lore.add(active == type ? "§a§lACTIVE PROFESSION" : "§eClick to become active");
        ItemStack card = MenuUtil.item(icon, title(type), lore);
        inventory.setItem(slot, card);
    }

    private String title(JobType type) {
        return switch (type) {
            case FARMER -> "§a§lDemeter's Harvest";
            case FISHER -> "§b§lPoseidon's Tide";
            case WOODCUTTER -> "§6§lArtemis Grove";
            case MINER -> "§c§lHephaestus Forge";
        };
    }
}
