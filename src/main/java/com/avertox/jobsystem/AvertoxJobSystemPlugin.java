package com.avertox.jobsystem;

import com.avertox.jobsystem.automation.AutomationManager;
import com.avertox.jobsystem.command.JobsAdminCommand;
import com.avertox.jobsystem.command.JobsCommand;
import com.avertox.jobsystem.config.ConfigManager;
import com.avertox.jobsystem.data.MySqlManager;
import com.avertox.jobsystem.economy.EconomyService;
import com.avertox.jobsystem.gui.MenuManager;
import com.avertox.jobsystem.jobs.FarmerJob;
import com.avertox.jobsystem.jobs.FisherJob;
import com.avertox.jobsystem.jobs.HunterJob;
import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.jobs.MinerJob;
import com.avertox.jobsystem.jobs.WoodcutterJob;
import com.avertox.jobsystem.listener.FarmerListener;
import com.avertox.jobsystem.listener.FisherListener;
import com.avertox.jobsystem.listener.FireworkSafetyListener;
import com.avertox.jobsystem.listener.HunterListener;
import com.avertox.jobsystem.listener.MinerListener;
import com.avertox.jobsystem.listener.PlayerConnectionListener;
import com.avertox.jobsystem.listener.AutomationListener;
import com.avertox.jobsystem.listener.CraftingListener;
import com.avertox.jobsystem.listener.PlacedBlockListener;
import com.avertox.jobsystem.listener.ToolLossListener;
import com.avertox.jobsystem.listener.WoodcutterListener;
import com.avertox.jobsystem.recipes.CustomCraftingManager;
import com.avertox.jobsystem.recipes.RecipeManager;
import com.avertox.jobsystem.tracker.PlacedBlockTracker;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class AvertoxJobSystemPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private MySqlManager mySqlManager;
    private EconomyService economyService;
    private JobManager jobManager;
    private RecipeManager recipeManager;
    private CustomCraftingManager customCraftingManager;
    private AutomationManager automationManager;
    private MenuManager menuManager;
    private JobToolService toolService;
    private PlacedBlockTracker placedBlockTracker;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.mySqlManager = new MySqlManager(this, configManager);
        this.mySqlManager.connect();

        this.economyService = new EconomyService(this);
        if (!economyService.setup()) {
            getLogger().severe("Vault economy not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.jobManager = new JobManager(mySqlManager, economyService);
        FarmerJob farmerJob = new FarmerJob(configManager);
        FisherJob fisherJob = new FisherJob(configManager);
        WoodcutterJob woodcutterJob = new WoodcutterJob(configManager);
        MinerJob minerJob = new MinerJob(configManager);
        HunterJob hunterJob = new HunterJob(configManager);
        jobManager.registerJob(farmerJob);
        jobManager.registerJob(fisherJob);
        jobManager.registerJob(woodcutterJob);
        jobManager.registerJob(minerJob);
        jobManager.registerJob(hunterJob);

        this.recipeManager = new RecipeManager();
        this.customCraftingManager = new CustomCraftingManager(this);
        customCraftingManager.registerRecipes();
        this.automationManager = new AutomationManager(this, configManager, mySqlManager);
        automationManager.start();
        this.menuManager = new MenuManager();
        this.toolService = new JobToolService(this);
        this.placedBlockTracker = new PlacedBlockTracker();

        registerListeners(farmerJob, fisherJob, woodcutterJob, minerJob, hunterJob);
        registerCommands();
        scheduleAutosave();
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (automationManager != null) {
            automationManager.stop();
        }
        if (jobManager != null) {
            jobManager.saveAll();
        }
        if (mySqlManager != null) {
            mySqlManager.disconnect();
        }
    }

    private void registerListeners(
            FarmerJob farmerJob,
            FisherJob fisherJob,
            WoodcutterJob woodcutterJob,
            MinerJob minerJob,
            HunterJob hunterJob
    ) {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(menuManager, this);
        pm.registerEvents(new PlayerConnectionListener(jobManager, automationManager), this);
        pm.registerEvents(new FarmerListener(this, jobManager, configManager, farmerJob, toolService, placedBlockTracker), this);
        pm.registerEvents(new FisherListener(jobManager, configManager, fisherJob, toolService), this);
        pm.registerEvents(new WoodcutterListener(jobManager, configManager, woodcutterJob, toolService, placedBlockTracker), this);
        pm.registerEvents(new MinerListener(jobManager, configManager, minerJob, toolService, placedBlockTracker), this);
        pm.registerEvents(new HunterListener(jobManager, configManager, hunterJob, toolService), this);
        pm.registerEvents(new AutomationListener(jobManager, automationManager, economyService, configManager, menuManager, this), this);
        pm.registerEvents(new CraftingListener(jobManager, customCraftingManager), this);
        pm.registerEvents(new ToolLossListener(jobManager, toolService), this);
        pm.registerEvents(new PlacedBlockListener(placedBlockTracker), this);
        pm.registerEvents(new FireworkSafetyListener(), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("jobs");
        if (command == null) {
            getLogger().warning("Command /jobs is not defined in plugin.yml");
        } else {
            command.setExecutor(new JobsCommand(
                    menuManager,
                    jobManager,
                    recipeManager,
                    customCraftingManager,
                    economyService,
                    configManager,
                    toolService
            ));
        }

        PluginCommand adminCommand = getCommand("jobsadmin");
        if (adminCommand == null) {
            getLogger().warning("Command /jobsadmin is not defined in plugin.yml");
            return;
        }
        adminCommand.setExecutor(new JobsAdminCommand(
                menuManager,
                jobManager,
                economyService,
                toolService
        ));
    }

    private void scheduleAutosave() {
        long periodTicks = configManager.getAutosaveMinutes() * 60L * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (jobManager != null) {
                jobManager.saveAll();
            }
        }, periodTicks, periodTicks);
    }
}
