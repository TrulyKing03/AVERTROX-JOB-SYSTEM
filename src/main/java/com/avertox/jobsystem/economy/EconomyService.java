package com.avertox.jobsystem.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class EconomyService {
    private final JavaPlugin plugin;
    private Object economy;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object rsp = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                return false;
            }
            Method getProvider = rsp.getClass().getMethod("getProvider");
            economy = getProvider.invoke(rsp);
            return economy != null;
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to hook Vault Economy: " + ex.getMessage());
            return false;
        }
    }

    public boolean has(Player player, double amount) {
        if (economy == null) return false;
        try {
            Method method = economy.getClass().getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
            return (boolean) method.invoke(economy, player, amount);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        try {
            Method withdraw = economy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = withdraw.invoke(economy, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return (boolean) success.invoke(response);
        } catch (Exception ex) {
            return false;
        }
    }

    public void deposit(Player player, double amount) {
        if (economy == null) {
            return;
        }
        try {
            Method deposit = economy.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
            deposit.invoke(economy, player, amount);
        } catch (Exception ignored) {
        }
    }
}
