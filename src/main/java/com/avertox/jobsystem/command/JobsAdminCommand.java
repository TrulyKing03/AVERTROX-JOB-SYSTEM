package com.avertox.jobsystem.command;

import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.gui.menu.admin.AdminPlayerManageMenu;
import com.avertox.jobsystem.gui.menu.admin.AdminPlayerSelectMenu;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsAdminCommand implements CommandExecutor {
    private final MenuManager menuManager;
    private final JobManager jobManager;
    private final EconomyService economyService;
    private final JobToolService toolService;

    public JobsAdminCommand(MenuManager menuManager, JobManager jobManager, EconomyService economyService, JobToolService toolService) {
        this.menuManager = menuManager;
        this.jobManager = jobManager;
        this.economyService = economyService;
        this.toolService = toolService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player-only command.");
            return true;
        }
        if (!player.isOp() && !player.hasPermission("avertoxjobs.admin")) {
            player.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§cPlayer not found.");
                return true;
            }
            menuManager.open(player, new AdminPlayerManageMenu(
                    menuManager,
                    jobManager,
                    economyService,
                    toolService,
                    target.getUniqueId()
            ));
            return true;
        }

        menuManager.open(player, new AdminPlayerSelectMenu(
                menuManager,
                jobManager,
                economyService,
                toolService
        ));
        return true;
    }
}
