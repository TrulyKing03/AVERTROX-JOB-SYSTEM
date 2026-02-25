package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.jobs.JobManager;
import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import com.avertox.jobsystem.tools.JobToolService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToolLossListener implements Listener {
    private final JobManager jobManager;
    private final JobToolService toolService;

    public ToolLossListener(JobManager jobManager, JobToolService toolService) {
        this.jobManager = jobManager;
        this.toolService = toolService;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (!toolService.isJobTool(dropped) || !toolService.isOwnedBy(dropped, player.getUniqueId())) {
            return;
        }
        JobType type = toolService.extractType(dropped);
        if (type == null) {
            return;
        }
        resetToStone(player, type);
    }

    @EventHandler
    public void onBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack broken = event.getBrokenItem();
        if (!toolService.isJobTool(broken) || !toolService.isOwnedBy(broken, player.getUniqueId())) {
            return;
        }
        JobType type = toolService.extractType(broken);
        if (type == null) {
            return;
        }
        resetToStone(player, type);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Set<JobType> lost = new HashSet<>();
        for (ItemStack drop : event.getDrops()) {
            if (!toolService.isJobTool(drop) || !toolService.isOwnedBy(drop, uuid)) {
                continue;
            }
            JobType type = toolService.extractType(drop);
            if (type != null) {
                lost.add(type);
            }
        }
        for (JobType type : lost) {
            resetToStone(player, type);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (!toolService.isJobTool(stack)) {
            return;
        }
        if (toolService.isOwnedBy(stack, player.getUniqueId())) {
            return;
        }
        event.getItem().setItemStack(toolService.createBrokenRelic());
    }

    private void resetToStone(Player player, JobType type) {
        PlayerJobData data = jobManager.getOrCreate(player.getUniqueId(), type);
        toolService.resetToolTier(data, type);
        player.sendMessage("§cYour " + type.name() + " tool was lost. Tool tier reset to Stone.");
    }
}
