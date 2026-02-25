package com.avertox.jobsystem.gui.menu.admin;

import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AdminPlayerManageMenu implements BaseMenu {
    private final MenuManager menuManager;
    private final JobManager jobManager;
    private final EconomyService economyService;
    private final JobToolService toolService;
    private final UUID targetUuid;
    private JobType selectedJob = JobType.FARMER;
    private final Inventory inventory;

    public AdminPlayerManageMenu(
            MenuManager menuManager,
            JobManager jobManager,
            EconomyService economyService,
            JobToolService toolService,
            UUID targetUuid
    ) {
        this.menuManager = menuManager;
        this.jobManager = jobManager;
        this.economyService = economyService;
        this.toolService = toolService;
        this.targetUuid = targetUuid;
        this.inventory = Bukkit.createInventory(null, 54, "Admin Controls - Player");
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
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            admin.sendMessage("§cTarget player is offline.");
            menuManager.open(admin, new AdminPlayerSelectMenu(menuManager, jobManager, economyService, toolService));
            return;
        }
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        if (slot == 45) {
            menuManager.open(admin, new AdminPlayerSelectMenu(menuManager, jobManager, economyService, toolService));
            return;
        }
        if (slot == 10) {
            selectedJob = JobType.FARMER;
            return;
        }
        if (slot == 11) {
            selectedJob = JobType.FISHER;
            return;
        }
        if (slot == 12) {
            selectedJob = JobType.WOODCUTTER;
            return;
        }
        if (slot == 13) {
            selectedJob = JobType.MINER;
            return;
        }
        if (slot == 49) {
            jobManager.clearSwitchCooldown(targetUuid);
            admin.sendMessage("§aCleared switch cooldown for " + target.getName());
            target.sendMessage("§aYour job switch cooldown was cleared by an admin.");
            return;
        }
        if (slot == 50) {
            jobManager.forceActivateJob(targetUuid, selectedJob);
            PlayerJobData data = jobManager.getOrCreate(targetUuid, selectedJob);
            toolService.grantCurrentTool(target, data, selectedJob);
            admin.sendMessage("§aForce-set active job to " + selectedJob + " for " + target.getName());
            return;
        }
        if (slot == 53) {
            jobManager.resetJobProgress(targetUuid, selectedJob);
            toolService.resetToolTier(jobManager.getOrCreate(targetUuid, selectedJob), selectedJob);
            toolService.grantCurrentTool(target, jobManager.getOrCreate(targetUuid, selectedJob), selectedJob);
            admin.sendMessage("§cReset " + selectedJob + " progress for " + target.getName());
            return;
        }
        if (slot == 20) {
            modifyXp(click, target, admin);
            return;
        }
        if (slot == 21) {
            modifyLevel(click, target, admin);
            return;
        }
        if (slot == 22) {
            modifyMoney(click, target, admin);
            return;
        }
        if (slot == 23) {
            modifyToolTier(click, target, admin);
            return;
        }
        if (slot == 24) {
            toolService.grantCurrentTool(target, jobManager.getOrCreate(targetUuid, selectedJob), selectedJob);
            admin.sendMessage("§aReforged and gave tool for " + selectedJob + ".");
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(targetUuid, selectedJob);
        long cooldown = jobManager.getRemainingSwitchCooldownMillis(targetUuid);
        long h = TimeUnit.MILLISECONDS.toHours(cooldown);
        long m = TimeUnit.MILLISECONDS.toMinutes(cooldown) % 60L;

        inventory.setItem(10, jobButton(JobType.FARMER, selectedJob == JobType.FARMER));
        inventory.setItem(11, jobButton(JobType.FISHER, selectedJob == JobType.FISHER));
        inventory.setItem(12, jobButton(JobType.WOODCUTTER, selectedJob == JobType.WOODCUTTER));
        inventory.setItem(13, jobButton(JobType.MINER, selectedJob == JobType.MINER));

        inventory.setItem(20, MenuUtil.item(Material.EXPERIENCE_BOTTLE, "§bXP Control", List.of(
                "§7Left: +100 XP",
                "§7Right: -100 XP",
                "§7Shift-Left: +1000 XP",
                "§7Shift-Right: -1000 XP",
                "§7Current: §f" + String.format("%.1f", data.getXp())
        )));
        inventory.setItem(21, MenuUtil.item(Material.BOOK, "§6Level Control", List.of(
                "§7Left: +1 level",
                "§7Right: -1 level",
                "§7Current: §f" + data.getLevel()
        )));
        inventory.setItem(22, MenuUtil.item(Material.GOLD_INGOT, "§aMoney Control", List.of(
                "§7Left: +$250",
                "§7Right: -$250",
                "§7Shift-Left: +$2500",
                "§7Shift-Right: -$2500",
                "§7Money Earned: §f" + String.format("%.2f", data.getMoneyEarned())
        )));
        inventory.setItem(23, MenuUtil.item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§dTool Tier Control", List.of(
                "§7Left: +1 tier",
                "§7Right: -1 tier",
                "§7Current: §f" + toolService.getToolTier(data, selectedJob)
        )));
        inventory.setItem(24, MenuUtil.item(Material.CHEST, "§bGive/Reforge Tool", List.of(
                "§7Give current selected job tool"
        )));

        inventory.setItem(49, MenuUtil.item(Material.CLOCK, "§aClear Switch Cooldown", List.of(
                "§7Remaining: §f" + h + "h " + m + "m",
                "§7Resets cooldown now"
        )));
        inventory.setItem(50, MenuUtil.item(Material.LODESTONE, "§eForce Active Job", List.of(
                "§7Set active job to selected: §f" + selectedJob,
                "§7Starts new cooldown timer"
        )));
        inventory.setItem(53, MenuUtil.item(Material.BARRIER, "§cReset Selected Job", List.of(
                "§7Resets level/xp/money/progress",
                "§7Resets tool tier to stone"
        )));
        inventory.setItem(45, MenuUtil.item(Material.ARROW, "§7Back to Player List", List.of("§7Return")));

        inventory.setItem(4, MenuUtil.item(Material.PLAYER_HEAD, "§fTarget: §e" + target.getName(), List.of(
                "§7Active Job: §f" + String.valueOf(jobManager.getActiveJob(targetUuid)),
                "§7Selected Job Level: §f" + data.getLevel(),
                "§7Editing: §f" + selectedJob
        )));
    }

    private void modifyXp(ClickType click, Player target, Player admin) {
        double delta = switch (click) {
            case LEFT -> 100;
            case RIGHT -> -100;
            case SHIFT_LEFT -> 1000;
            case SHIFT_RIGHT -> -1000;
            default -> 0;
        };
        if (delta == 0) return;
        jobManager.modifyXp(targetUuid, selectedJob, delta);
        admin.sendMessage("§aAdjusted XP (" + selectedJob + ") by " + delta + " for " + target.getName());
    }

    private void modifyLevel(ClickType click, Player target, Player admin) {
        int delta = click == ClickType.LEFT ? 1 : click == ClickType.RIGHT ? -1 : 0;
        if (delta == 0) return;
        jobManager.modifyLevel(targetUuid, selectedJob, delta);
        admin.sendMessage("§aAdjusted level (" + selectedJob + ") by " + delta + " for " + target.getName());
    }

    private void modifyMoney(ClickType click, Player target, Player admin) {
        double delta = switch (click) {
            case LEFT -> 250;
            case RIGHT -> -250;
            case SHIFT_LEFT -> 2500;
            case SHIFT_RIGHT -> -2500;
            default -> 0;
        };
        if (delta == 0) return;
        jobManager.modifyMoneyEarned(targetUuid, selectedJob, delta);
        if (delta > 0) {
            economyService.deposit(target, delta);
        } else {
            economyService.withdraw(target, Math.abs(delta));
        }
        admin.sendMessage("§aAdjusted money (" + selectedJob + ") by " + delta + " for " + target.getName());
    }

    private void modifyToolTier(ClickType click, Player target, Player admin) {
        int delta = click == ClickType.LEFT ? 1 : click == ClickType.RIGHT ? -1 : 0;
        if (delta == 0) return;
        PlayerJobData data = jobManager.getOrCreate(targetUuid, selectedJob);
        int tier = Math.max(1, Math.min(10, toolService.getToolTier(data, selectedJob) + delta));
        toolService.setToolTier(data, selectedJob, tier);
        toolService.grantCurrentTool(target, data, selectedJob);
        admin.sendMessage("§aSet tool tier (" + selectedJob + ") to " + tier + " for " + target.getName());
    }

    private org.bukkit.inventory.ItemStack jobButton(JobType type, boolean selected) {
        Material mat = switch (type) {
            case FARMER -> Material.WHEAT;
            case FISHER -> Material.FISHING_ROD;
            case WOODCUTTER -> Material.IRON_AXE;
            case MINER -> Material.IRON_PICKAXE;
        };
        return MenuUtil.item(mat, (selected ? "§a§l" : "§e") + type.name(), List.of(
                selected ? "§aSelected context" : "§7Click to select context"
        ));
    }
}
