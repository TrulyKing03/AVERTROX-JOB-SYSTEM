package com.avertox.jobsystem.command;

import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.gui.menu.GeneratorBrokerMenu;
import com.avertox.jobsystem.gui.menu.JobOverviewMenu;
import com.avertox.jobsystem.gui.menu.JobSellMenu;
import com.avertox.jobsystem.gui.menu.RecipeUnlockMenu;
import com.avertox.jobsystem.gui.menu.UpgradeAnvilMenu;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.recipes.CustomCraftingManager;
import com.avertox.jobsystem.recipes.RecipeManager;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsCommand implements CommandExecutor {
    private final MenuManager menuManager;
    private final JobManager jobManager;
    private final RecipeManager recipeManager;
    private final CustomCraftingManager customCraftingManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;
    private final JobToolService toolService;

    public JobsCommand(
            MenuManager menuManager,
            JobManager jobManager,
            RecipeManager recipeManager,
            CustomCraftingManager customCraftingManager,
            EconomyService economyService,
            ConfigManager configManager,
            JobToolService toolService
    ) {
        this.menuManager = menuManager;
        this.jobManager = jobManager;
        this.recipeManager = recipeManager;
        this.customCraftingManager = customCraftingManager;
        this.economyService = economyService;
        this.configManager = configManager;
        this.toolService = toolService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (args.length == 0) {
            menuManager.open(player, new JobOverviewMenu(jobManager, toolService, menuManager, economyService, configManager));
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("sell")) {
                menuManager.open(player, new JobSellMenu(jobManager, economyService, configManager));
                return true;
            }
            if (args[0].equalsIgnoreCase("generator") || args[0].equalsIgnoreCase("generators")) {
                menuManager.open(player, new GeneratorBrokerMenu(jobManager, configManager));
                return true;
            }
            if (args[0].equalsIgnoreCase("upgrade") || args[0].equalsIgnoreCase("recipes")) {
                JobType active = jobManager.getActiveJob(player.getUniqueId());
                if (active == null) {
                    player.sendMessage("\u00A7cNo active job selected. Use /jobs to pick one.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("upgrade")) {
                    menuManager.open(player, new UpgradeAnvilMenu(active, jobManager, economyService, configManager, toolService));
                    return true;
                }
                menuManager.open(player, new RecipeUnlockMenu(active, jobManager, recipeManager, customCraftingManager));
                return true;
            }
        }
        if (args.length == 2) {
            JobType type;
            try {
                type = JobType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ex) {
                player.sendMessage("\u00A7cUnknown job.");
                return true;
            }
            if (args[0].equalsIgnoreCase("upgrade")) {
                menuManager.open(player, new UpgradeAnvilMenu(type, jobManager, economyService, configManager, toolService));
                return true;
            }
            if (args[0].equalsIgnoreCase("recipes")) {
                menuManager.open(player, new RecipeUnlockMenu(type, jobManager, recipeManager, customCraftingManager));
                return true;
            }
        }
        player.sendMessage("\u00A7eUsage: /jobs, /jobs sell, /jobs generator, /jobs upgrade <job>, /jobs recipes <job>");
        return true;
    }
}
