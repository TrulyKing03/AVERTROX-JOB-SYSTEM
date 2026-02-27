package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.Job;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class JobOverviewMenu implements BaseMenu {
    private final JobManager jobManager;
    private final JobToolService toolService;
    private final MenuManager menuManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final Inventory inventory;

    public JobOverviewMenu(
            JobManager jobManager,
            JobToolService toolService,
            MenuManager menuManager,
            EconomyService economyService,
            ConfigManager configManager
    ) {
        this.jobManager = jobManager;
        this.toolService = toolService;
        this.menuManager = menuManager;
        this.economyService = economyService;
        this.configManager = configManager;
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
        int slot = event.getRawSlot();
        if (slot == 48) {
            menuManager.open(player, new JobSellMenu(jobManager, economyService, configManager));
            return;
        }
        if (slot == 50) {
            menuManager.open(player, new GeneratorBrokerMenu(jobManager, configManager));
            return;
        }

        JobType target = switch (slot) {
            case 19 -> JobType.FARMER;
            case 21 -> JobType.FISHER;
            case 23 -> JobType.WOODCUTTER;
            case 25 -> JobType.MINER;
            case 31 -> JobType.HUNTER;
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
            JobType active = jobManager.getActiveJob(player.getUniqueId());
            player.sendMessage("\u00A7cSwitch cooldown active. Current job: " + active + ". Retry in " + hours + "h " + minutes + "m.");
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), target);
        toolService.grantCurrentTool(player, data, target);
        player.sendMessage("\u00A7aActive job set to " + target.name() + ". Your forged tool has been granted.");
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.BLACK_STAINED_GLASS_PANE, "\u00A70");

        JobType active = jobManager.getActiveJob(player.getUniqueId());
        inventory.setItem(4, MenuUtil.item(Material.NETHER_STAR, "\u00A76\u00A7lHall of Professions", List.of(
                "\u00A77Choose your active profession.",
                "\u00A77Only one can be active at a time.",
                "\u00A77Switching starts a 24h cooldown."
        )));

        placeJobCard(player, JobType.FARMER, Material.GOLDEN_HOE, 19, active);
        placeJobCard(player, JobType.FISHER, Material.FISHING_ROD, 21, active);
        placeJobCard(player, JobType.WOODCUTTER, Material.GOLDEN_AXE, 23, active);
        placeJobCard(player, JobType.MINER, Material.GOLDEN_PICKAXE, 25, active);
        placeJobCard(player, JobType.HUNTER, Material.BOW, 31, active);

        if (active != null) {
            Job.ProgressSnapshot progress = jobManager.getProgress(player.getUniqueId(), active);
            inventory.setItem(40, MenuUtil.item(Material.EXPERIENCE_BOTTLE, "\u00A7eActive Progress - " + active.name(), List.of(
                    "\u00A77Level: \u00A7f" + progress.level() + "/" + progress.maxLevel(),
                    "\u00A77Progress: " + progressBar(progress.progressPercent()),
                    progress.maxed()
                            ? "\u00A76At maximum level."
                            : "\u00A77To Next: \u00A7b" + formatOneDecimal(progress.xpToNext()) + " XP"
            )));
        }

        long remaining = jobManager.getRemainingSwitchCooldownMillis(player.getUniqueId());
        long h = TimeUnit.MILLISECONDS.toHours(remaining);
        long m = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60L;
        inventory.setItem(49, MenuUtil.item(Material.CLOCK, "\u00A7bSwitch Cooldown", List.of(
                "\u00A77Remaining: \u00A7f" + h + "h " + m + "m",
                "\u00A77Cooldown starts when switching",
                "\u00A77to a different profession."
        )));

        inventory.setItem(48, MenuUtil.item(Material.EMERALD_BLOCK, "\u00A7a\u00A7lSell Handler", List.of(
                "\u00A77Open job market to sell",
                "\u00A77collected resources."
        )));
        inventory.setItem(50, MenuUtil.item(Material.VILLAGER_SPAWN_EGG, "\u00A76\u00A7lGenerator Broker", List.of(
                "\u00A77NPC-style interaction menu for generators.",
                "\u00A77Shows authorized block/output rules."
        )));
    }

    private void placeJobCard(Player player, JobType type, Material icon, int slot, JobType active) {
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
        Job.ProgressSnapshot progress = jobManager.getProgress(player.getUniqueId(), type);
        List<String> lore = new ArrayList<>();
        lore.add("\u00A77Level: \u00A7f" + data.getLevel());
        lore.add("\u00A77XP: \u00A7f" + formatOneDecimal(data.getXp()));
        lore.add("\u00A77Progress: " + progressBar(progress.progressPercent()));
        lore.add(progress.maxed()
                ? "\u00A76MAX Level"
                : "\u00A77To Next: \u00A7b" + formatOneDecimal(progress.xpToNext()) + " XP");
        lore.add("\u00A77Money Earned: \u00A7a$" + String.format(Locale.US, "%.2f", data.getMoneyEarned()));
        lore.add("\u00A77Tool Tier: \u00A7f" + toolService.getToolTier(data, type));
        lore.add("\u00A78");
        lore.add(active == type ? "\u00A7a\u00A7lACTIVE PROFESSION" : "\u00A7eClick to become active");
        ItemStack card = MenuUtil.item(icon, title(type), lore);
        inventory.setItem(slot, card);
    }

    private String title(JobType type) {
        return switch (type) {
            case FARMER -> "\u00A7a\u00A7lDemeter's Harvest";
            case FISHER -> "\u00A7b\u00A7lPoseidon's Tide";
            case WOODCUTTER -> "\u00A76\u00A7lArtemis Grove";
            case MINER -> "\u00A7c\u00A7lHephaestus Forge";
            case HUNTER -> "\u00A74\u00A7lAres Pursuit";
        };
    }

    private String progressBar(double progress) {
        int segments = 12;
        int filled = (int) Math.round(Math.max(0.0D, Math.min(1.0D, progress)) * segments);
        StringBuilder bar = new StringBuilder("\u00A78[");
        for (int i = 0; i < segments; i++) {
            bar.append(i < filled ? "\u00A7a|" : "\u00A77|");
        }
        bar.append("\u00A78]");
        return bar.toString();
    }

    private String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
