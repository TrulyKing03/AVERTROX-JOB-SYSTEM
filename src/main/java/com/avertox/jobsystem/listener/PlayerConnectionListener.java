package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.automation.AutomationManager;
import com.avertox.jobsystem.jobs.JobManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final JobManager jobManager;
    private final AutomationManager automationManager;

    public PlayerConnectionListener(JobManager jobManager, AutomationManager automationManager) {
        this.jobManager = jobManager;
        this.automationManager = automationManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        jobManager.loadPlayer(event.getPlayer().getUniqueId());
        automationManager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        jobManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
