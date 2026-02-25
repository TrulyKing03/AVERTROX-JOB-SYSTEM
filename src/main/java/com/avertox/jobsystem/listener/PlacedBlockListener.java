package com.avertox.jobsystem.listener;

import com.avertox.jobsystem.listener.util.JobMaterials;
import com.avertox.jobsystem.tracker.PlacedBlockTracker;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlacedBlockListener implements Listener {
    private final PlacedBlockTracker placedBlockTracker;

    public PlacedBlockListener(PlacedBlockTracker placedBlockTracker) {
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material type = block.getType();
        if (JobMaterials.CROPS.contains(type)
                || JobMaterials.LOGS.contains(type)
                || JobMaterials.ORES.contains(type)
                || type == Material.STONE
                || type == Material.DEEPSLATE) {
            placedBlockTracker.markPlaced(block.getLocation());
        }
    }
}
