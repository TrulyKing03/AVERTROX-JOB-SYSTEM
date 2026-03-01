package com.avertox.jobsystem.gui.menu;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.gui.BaseMenu;
import com.avertox.jobsystem.gui.MenuUtil;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeneratorBrokerMenu implements BaseMenu {
    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final Inventory inventory;

    public GeneratorBrokerMenu(JobManager jobManager, ConfigManager configManager) {
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.inventory = Bukkit.createInventory(null, 45, "Generator Broker");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        refresh(player);
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.7f, 1.0f);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        JobType selected = switch (event.getRawSlot()) {
            case 20 -> JobType.FARMER;
            case 21 -> JobType.FISHER;
            case 22 -> JobType.WOODCUTTER;
            case 23 -> JobType.MINER;
            case 24 -> JobType.HUNTER;
            default -> null;
        };
        if (selected == null) {
            return;
        }
        Material generator = configManager.getGeneratorBlock(selected);
        int requiredLevel = configManager.getAutomationRequiredJobLevel(selected);
        player.sendMessage("\u00A7eGenerator Broker\u00A77: Place \u00A7f" + generator + "\u00A77 for \u00A7f" + selected + "\u00A77 automation once level " + requiredLevel + " is reached.");
    }

    @Override
    public void refresh(Player player) {
        inventory.clear();
        MenuUtil.frame(inventory, Material.ORANGE_STAINED_GLASS_PANE, "\u00A76");
        inventory.setItem(4, MenuUtil.item(Material.VILLAGER_SPAWN_EGG, "\u00A76\u00A7lGenerator Broker", List.of(
                "\u00A77NPC-style generator dialogue menu",
                "\u00A77(ready for later NPC integration).",
                "\u00A77Generators only accept configured blocks.",
                "\u00A77Outputs are restricted to configured materials."
        )));

        placeCard(player, JobType.FARMER, 20);
        placeCard(player, JobType.FISHER, 21);
        placeCard(player, JobType.WOODCUTTER, 22);
        placeCard(player, JobType.MINER, 23);
        placeCard(player, JobType.HUNTER, 24);
    }

    private void placeCard(Player player, JobType type, int slot) {
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
        Material generator = configManager.getGeneratorBlock(type);
        Map<Material, Integer> outputs = configManager.getGeneratorOutputs(type);
        int requiredLevel = configManager.getAutomationRequiredJobLevel(type);

        List<String> lore = new ArrayList<>();
        lore.add("\u00A77Required Level: \u00A7f" + requiredLevel);
        lore.add("\u00A77Your Level: \u00A7f" + data.getLevel());
        lore.add("\u00A77Generator Block: \u00A7f" + generator);
        lore.add("\u00A77Authorized Outputs:");
        for (Map.Entry<Material, Integer> entry : outputs.entrySet()) {
            lore.add("\u00A7a- " + entry.getKey() + " x" + entry.getValue());
        }
        lore.add("\u00A78");
        lore.add(data.getLevel() >= requiredLevel ? "\u00A7aReady to place generator" : "\u00A7cLevel " + requiredLevel + " required");
        lore.add("\u00A7eClick for NPC guidance message");

        inventory.setItem(slot, MenuUtil.item(generator, "\u00A7e\u00A7l" + type.name() + " Generator", lore));
    }
}
