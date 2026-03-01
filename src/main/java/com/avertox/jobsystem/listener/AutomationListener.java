package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.automation.AutomationBlock;
import com.avertox.jobsystem.automation.AutomationManager;
import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.gui.menu.AutomationCollectionMenu;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public class AutomationListener implements Listener {
    private final JobManager jobManager;
    private final AutomationManager automationManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final MenuManager menuManager;
    private final JavaPlugin plugin;

    public AutomationListener(
            JobManager jobManager,
            AutomationManager automationManager,
            EconomyService economyService,
            ConfigManager configManager,
            MenuManager menuManager,
            JavaPlugin plugin
    ) {
        this.jobManager = jobManager;
        this.automationManager = automationManager;
        this.economyService = economyService;
        this.configManager = configManager;
        this.menuManager = menuManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onAutomationPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        JobType type = configManager.resolveGeneratorJob(block.getType());
        if (type == null) {
            return;
        }
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
        int requiredLevel = configManager.getAutomationRequiredJobLevel(type);
        if (data.getLevel() < requiredLevel) {
            player.sendMessage("\u00A7cYou need level " + requiredLevel + " in " + type.name() + " to place automation.");
            event.setCancelled(true);
            return;
        }
        if (!automationManager.canPlace(player.getUniqueId(), type)) {
            player.sendMessage("\u00A7cAutomation block limit reached for " + type.name() + ".");
            event.setCancelled(true);
            return;
        }
        automationManager.create(player.getUniqueId(), type, block.getLocation());
        player.sendMessage("\u00A7aAutomation block placed.");
    }

    @EventHandler
    public void onAutomationInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        AutomationBlock automationBlock = automationManager.getByLocation(block.getLocation());
        if (automationBlock == null) {
            return;
        }
        if (block.getType() != configManager.getGeneratorBlock(automationBlock.getJobType())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00A7cThis block type is not authorized for that generator job.");
            return;
        }
        Player player = event.getPlayer();
        if (!automationBlock.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("\u00A7cYou do not own this automation block.");
            return;
        }
        menuManager.open(player, new AutomationCollectionMenu(plugin, automationManager, economyService, configManager, automationBlock));
        event.setCancelled(true);
    }
}
