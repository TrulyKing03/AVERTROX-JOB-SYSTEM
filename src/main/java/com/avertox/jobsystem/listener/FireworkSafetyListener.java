package com.avertox.jobsystem.listener;

import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class FireworkSafetyListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Firework)) {
            return;
        }
        event.setDamage(0.0D);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireworkExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Firework)) {
            return;
        }
        event.blockList().clear();
        event.setYield(0.0F);
    }
}
