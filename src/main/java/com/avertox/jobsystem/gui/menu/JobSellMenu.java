package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JobSellMenu implements BaseMenu {
    private static final int[] PREVIEW_SLOTS = {11, 12, 13, 14, 15, 20, 21, 23, 24, 29, 30, 31, 32, 33};

    private final JobManager jobManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final Inventory inventory;

    public JobSellMenu(JobManager jobManager, EconomyService economyService, ConfigManager configManager) {
        this.jobManager = jobManager;
        this.economyService = economyService;
        this.configManager = configManager;
        this.inventory = Bukkit.createInventory(null, 45, "Job Handler - Market");
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
        if (slot == 22) {
            sellAll(player);
        } else if (slot == 40) {
            sellMainHand(player);
        }
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.BROWN_STAINED_GLASS_PANE, "\u00A76");

        JobType active = jobManager.getActiveJob(player.getUniqueId());
        SalePreview preview = scan(player, active);

        inventory.setItem(4, MenuUtil.item(Material.GOLD_INGOT, "\u00A76\u00A7lJob Handler", List.of(
                "\u00A77Active Job: \u00A7f" + (active == null ? "None" : active.name()),
                "\u00A77Sell Value: \u00A7a$" + formatMoney(preview.totalValue()),
                "\u00A77Sellable Stacks: \u00A7f" + preview.materialTotals().size()
        )));

        List<Map.Entry<Material, Integer>> entries = new ArrayList<>(preview.materialTotals().entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<Material, Integer> e) -> unitPrice(e.getKey()) * e.getValue()).reversed());
        int idx = 0;
        for (Map.Entry<Material, Integer> entry : entries) {
            if (idx >= PREVIEW_SLOTS.length) {
                break;
            }
            Material material = entry.getKey();
            int amount = entry.getValue();
            double unit = unitPrice(material);
            double total = unit * amount;
            ItemStack stack = new ItemStack(material, Math.min(64, amount));
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("\u00A7e" + material.name());
                meta.setLore(List.of(
                        "\u00A77Amount: \u00A7f" + amount,
                        "\u00A77Unit Price: \u00A7a$" + formatMoney(unit),
                        "\u00A77Stack Value: \u00A7a$" + formatMoney(total)
                ));
                stack.setItemMeta(meta);
            }
            inventory.setItem(PREVIEW_SLOTS[idx++], stack);
        }

        if (active == null) {
            inventory.setItem(22, MenuUtil.item(Material.BARRIER, "\u00A7c\u00A7lNo Active Job", List.of(
                    "\u00A77Select an active job in /jobs",
                    "\u00A77before selling any items."
            )));
            inventory.setItem(40, MenuUtil.item(Material.BARRIER, "\u00A7cMain Hand Disabled", List.of(
                    "\u00A77Activate a job to sell job items."
            )));
        } else {
            inventory.setItem(22, MenuUtil.item(Material.EMERALD_BLOCK, "\u00A7a\u00A7lSell All", List.of(
                    "\u00A77Sell all priced items for your active job",
                    "\u00A77Expected payout: \u00A7a$" + formatMoney(preview.totalValue())
            )));
            inventory.setItem(40, MenuUtil.item(Material.EMERALD, "\u00A7bSell Main Hand", List.of(
                    "\u00A77Quick-sell your held item stack",
                    "\u00A77if it belongs to your active job."
            )));
        }
    }

    private void sellAll(Player player) {
        JobType active = jobManager.getActiveJob(player.getUniqueId());
        if (active == null) {
            player.sendMessage("\u00A7cSelect an active job before selling items.");
            return;
        }
        Set<Material> allowed = configManager.getSellableMaterials(active);
        ItemStack[] storage = player.getInventory().getStorageContents();
        double total = 0.0D;
        int soldItems = 0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (!allowed.contains(stack.getType())) {
                continue;
            }
            double unit = unitPrice(stack.getType());
            if (unit <= 0.0D) {
                continue;
            }
            soldItems += stack.getAmount();
            total += unit * stack.getAmount();
            storage[i] = null;
        }

        if (soldItems == 0) {
            player.sendMessage("\u00A7cNo sellable items found in your inventory.");
            return;
        }

        player.getInventory().setStorageContents(storage);
        payAndTrack(player, total);
        player.sendMessage("\u00A7aSold " + soldItems + " items for \u00A7f$" + formatMoney(total) + "\u00A7a.");
    }

    private void sellMainHand(Player player) {
        JobType active = jobManager.getActiveJob(player.getUniqueId());
        if (active == null) {
            player.sendMessage("\u00A7cSelect an active job before selling items.");
            return;
        }
        Set<Material> allowed = configManager.getSellableMaterials(active);
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage("\u00A7cHold a sellable item in your main hand.");
            return;
        }
        if (!allowed.contains(hand.getType())) {
            player.sendMessage("\u00A7cThis item cannot be sold for your active job (" + active.name() + ").");
            return;
        }
        double unit = unitPrice(hand.getType());
        if (unit <= 0.0D) {
            player.sendMessage("\u00A7cThis item has no sell value.");
            return;
        }
        double total = unit * hand.getAmount();
        int amount = hand.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        payAndTrack(player, total);
        player.sendMessage("\u00A7aSold " + amount + "x " + hand.getType().name() + " for \u00A7f$" + formatMoney(total) + "\u00A7a.");
    }

    private void payAndTrack(Player player, double total) {
        economyService.deposit(player, total);
        JobType active = jobManager.getActiveJob(player.getUniqueId());
        if (active != null) {
            jobManager.addMoneyEarned(player.getUniqueId(), active, total);
        }
    }

    private SalePreview scan(Player player, JobType active) {
        if (active == null) {
            return new SalePreview(Map.of(), 0.0D);
        }
        Set<Material> allowed = configManager.getSellableMaterials(active);
        Map<Material, Integer> totals = new EnumMap<>(Material.class);
        double sum = 0.0D;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (!allowed.contains(stack.getType())) {
                continue;
            }
            double unit = unitPrice(stack.getType());
            if (unit <= 0.0D) {
                continue;
            }
            totals.merge(stack.getType(), stack.getAmount(), Integer::sum);
            sum += unit * stack.getAmount();
        }
        return new SalePreview(totals, sum);
    }

    private double unitPrice(Material material) {
        return configManager.getSellPrice(material);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private record SalePreview(Map<Material, Integer> materialTotals, double totalValue) {
    }
}
