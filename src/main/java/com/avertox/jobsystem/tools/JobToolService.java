package com.avertox.jobsystem.tools;

import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobToolService {
    private final NamespacedKey ownerKey;
    private final NamespacedKey jobKey;
    private final NamespacedKey tierKey;

    public JobToolService(JavaPlugin plugin) {
        this.ownerKey = new NamespacedKey(plugin, "job_tool_owner");
        this.jobKey = new NamespacedKey(plugin, "job_tool_type");
        this.tierKey = new NamespacedKey(plugin, "job_tool_tier");
    }

    public int getToolTier(PlayerJobData data, JobType type) {
        return Math.max(1, data.getUpgrades().getOrDefault(toolTierKey(type), 1));
    }

    public void setToolTier(PlayerJobData data, JobType type, int tier) {
        data.getUpgrades().put(toolTierKey(type), Math.max(1, Math.min(10, tier)));
    }

    public void resetToolTier(PlayerJobData data, JobType type) {
        setToolTier(data, type, 1);
    }

    public ItemStack createTool(UUID owner, JobType type, int tier) {
        int clamped = Math.max(1, Math.min(10, tier));
        ItemStack stack = new ItemStack(baseMaterial(type, clamped));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(toolName(type, clamped));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Owner-Bound Job Tool");
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.GOLD + clamped);
        lore.add(ChatColor.GRAY + "Class: " + ChatColor.AQUA + type.name());
        lore.add(ChatColor.GRAY + "Perks:");
        for (String perk : perkLore(type, clamped)) {
            lore.add(ChatColor.GREEN + " - " + perk);
        }
        lore.add(ChatColor.RED + "If lost: resets to Stone Tier.");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        applyEnchants(meta, type, clamped);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, owner.toString());
        pdc.set(jobKey, PersistentDataType.STRING, type.name());
        pdc.set(tierKey, PersistentDataType.INTEGER, clamped);

        stack.setItemMeta(meta);
        return stack;
    }

    public List<String> perkLore(JobType type, int tier) {
        List<String> perks = new ArrayList<>();
        int t = Math.max(1, Math.min(10, tier));
        perks.add(ChatColor.BOLD + "+" + (t * 8) + "% action speed");
        perks.add(ChatColor.BOLD + "+" + (t * 6) + "% reward efficiency");
        switch (type) {
            case FARMER -> perks.add(ChatColor.ITALIC + "Demeter's Blessing +" + (t * 2) + "% growth pulse");
            case FISHER -> perks.add(ChatColor.ITALIC + "Poseidon's Favor +" + (t * 3) + "% rare fish chance");
            case WOODCUTTER -> perks.add(ChatColor.ITALIC + "Artemis' Strike +" + (t * 4) + "% felling force");
            case MINER -> perks.add(ChatColor.ITALIC + "Hephaestus Core +" + (t * 4) + "% ore resonance");
            case HUNTER -> perks.add(ChatColor.ITALIC + "Ares Mark +" + (t * 4) + "% bounty precision");
        }
        return perks;
    }

    public boolean isJobTool(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.has(ownerKey, PersistentDataType.STRING)
                && pdc.has(jobKey, PersistentDataType.STRING)
                && pdc.has(tierKey, PersistentDataType.INTEGER);
    }

    public boolean isUsableBy(Player player, ItemStack stack, JobType type) {
        if (!isJobTool(stack)) {
            return false;
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String owner = pdc.get(ownerKey, PersistentDataType.STRING);
        String job = pdc.get(jobKey, PersistentDataType.STRING);
        return player.getUniqueId().toString().equals(owner) && type.name().equals(job);
    }

    public boolean hasUsableTool(Player player, JobType type) {
        return isUsableBy(player, player.getInventory().getItemInMainHand(), type);
    }

    public boolean hasOwnedToolInInventory(Player player, JobType type) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack stack : inv.getContents()) {
            if (isUsableBy(player, stack, type)) {
                return true;
            }
        }
        return false;
    }

    public int getHeldTier(Player player, JobType type) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isUsableBy(player, stack, type)) {
            return 0;
        }
        Integer tier = stack.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        return tier == null ? 1 : Math.max(1, Math.min(10, tier));
    }

    public void grantCurrentTool(Player player, PlayerJobData data, JobType type) {
        removeOwnedTools(player, type);
        int tier = getToolTier(data, type);
        player.getInventory().addItem(createTool(player.getUniqueId(), type, tier));
    }

    public void removeOwnedTools(Player player, JobType type) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || !isUsableBy(player, stack, type)) {
                continue;
            }
            inv.setItem(i, null);
        }
    }

    public JobType extractType(ItemStack stack) {
        if (!isJobTool(stack)) {
            return null;
        }
        String raw = stack.getItemMeta().getPersistentDataContainer().get(jobKey, PersistentDataType.STRING);
        try {
            return raw == null ? null : JobType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isOwnedBy(ItemStack stack, UUID owner) {
        if (!isJobTool(stack)) {
            return false;
        }
        String raw = stack.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return owner.toString().equals(raw);
    }

    public ItemStack createBrokenRelic() {
        ItemStack stack = new ItemStack(Material.FLINT);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Broken Bound Relic");
            meta.setLore(List.of(
                    ChatColor.GRAY + "This tool rejected a non-owner.",
                    ChatColor.RED + "No perks. No progression. No power."
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String toolTierKey(JobType type) {
        return "tool_tier_" + type.key();
    }

    private Material baseMaterial(JobType type, int tier) {
        int t = Math.max(1, Math.min(10, tier));
        return switch (type) {
            case FARMER -> t <= 2 ? Material.WOODEN_HOE : t <= 4 ? Material.STONE_HOE : t <= 6 ? Material.IRON_HOE : t <= 8 ? Material.DIAMOND_HOE : Material.NETHERITE_HOE;
            case FISHER -> Material.FISHING_ROD;
            case WOODCUTTER -> t <= 2 ? Material.WOODEN_AXE : t <= 4 ? Material.STONE_AXE : t <= 6 ? Material.IRON_AXE : t <= 8 ? Material.DIAMOND_AXE : Material.NETHERITE_AXE;
            case MINER -> t <= 2 ? Material.WOODEN_PICKAXE : t <= 4 ? Material.STONE_PICKAXE : t <= 6 ? Material.IRON_PICKAXE : t <= 8 ? Material.DIAMOND_PICKAXE : Material.NETHERITE_PICKAXE;
            case HUNTER -> t <= 2 ? Material.WOODEN_SWORD : t <= 4 ? Material.STONE_SWORD : t <= 6 ? Material.IRON_SWORD : t <= 8 ? Material.DIAMOND_SWORD : Material.NETHERITE_SWORD;
        };
    }

    private String toolName(JobType type, int tier) {
        int i = Math.max(0, Math.min(9, tier - 1));
        String[] names = switch (type) {
            case FARMER -> new String[]{
                    "\u00A76\u00A7lGaia's Sprout",
                    "\u00A7e\u00A7lDemeter's Root",
                    "\u00A7f\u00A7lThresher of Arcadia",
                    "\u00A77\u00A7lAegis Harvest",
                    "\u00A7f\u00A7lChronos Furrow",
                    "\u00A7f\u00A7lHelios Sickle",
                    "\u00A7b\u00A7lOlympian Reaper",
                    "\u00A73\u00A7lTitan Grovefang",
                    "\u00A75\u00A7lAether Bloomcleaver",
                    "\u00A74\u00A7l\u00A7nApex of Elysium"
            };
            case FISHER -> new String[]{
                    "\u00A76\u00A7lNaiad's Thread",
                    "\u00A7e\u00A7lPoseidon's Line",
                    "\u00A7f\u00A7lTide of Nereus",
                    "\u00A77\u00A7lTrident Whisper",
                    "\u00A7f\u00A7lAegean Vortex",
                    "\u00A7f\u00A7lLeviathan Hook",
                    "\u00A7b\u00A7lSiren's Verdict",
                    "\u00A73\u00A7lAbyss Oracle",
                    "\u00A75\u00A7lOceanus Crownline",
                    "\u00A74\u00A7l\u00A7nThrone of the Deep"
            };
            case WOODCUTTER -> new String[]{
                    "\u00A76\u00A7lDryad's Edge",
                    "\u00A7e\u00A7lArtemis Cleaver",
                    "\u00A7f\u00A7lGrovebreaker",
                    "\u00A77\u00A7lCedar of Sparta",
                    "\u00A7f\u00A7lAres Timberfang",
                    "\u00A7f\u00A7lPine of Olympus",
                    "\u00A7b\u00A7lTitan Woodrend",
                    "\u00A73\u00A7lVerdant Cataclysm",
                    "\u00A75\u00A7lElderheart Ruin",
                    "\u00A74\u00A7l\u00A7nWorldtree Executioner"
            };
            case MINER -> new String[]{
                    "\u00A76\u00A7lHephaestus Chip",
                    "\u00A7e\u00A7lForgeborn Fang",
                    "\u00A7f\u00A7lStone of Delphi",
                    "\u00A77\u00A7lUnderforge Pike",
                    "\u00A7f\u00A7lAres Deepcut",
                    "\u00A7f\u00A7lMagma Titan",
                    "\u00A7b\u00A7lOlympian Riftpick",
                    "\u00A73\u00A7lHades Veinbreaker",
                    "\u00A75\u00A7lStyx Coresplitter",
                    "\u00A74\u00A7l\u00A7nCrown of Tartarus"
            };
            case HUNTER -> new String[]{
                    "\u00A76\u00A7lAres Fang",
                    "\u00A7e\u00A7lSkirmisher's Oath",
                    "\u00A7f\u00A7lMoontrail Blade",
                    "\u00A77\u00A7lPredator's Pact",
                    "\u00A7f\u00A7lStalker of Nyx",
                    "\u00A7f\u00A7lRavager's Edge",
                    "\u00A7b\u00A7lHuntmaster Talon",
                    "\u00A73\u00A7lBloodmoon Reaver",
                    "\u00A75\u00A7lApex Pursuer",
                    "\u00A74\u00A7l\u00A7nThrone of the Hunt"
            };
        };
        return names[i] + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + " ";
    }

    private void applyEnchants(ItemMeta meta, JobType type, int tier) {
        int speedLevel = Math.max(1, Math.min(6, (tier + 1) / 2));
        switch (type) {
            case FARMER -> meta.addEnchant(Enchantment.DIG_SPEED, speedLevel, true);
            case FISHER -> {
                meta.addEnchant(Enchantment.LURE, Math.max(1, Math.min(5, tier / 2)), true);
                meta.addEnchant(Enchantment.LUCK, Math.max(1, Math.min(5, (tier + 1) / 2)), true);
            }
            case WOODCUTTER, MINER -> meta.addEnchant(Enchantment.DIG_SPEED, speedLevel, true);
            case HUNTER -> meta.addEnchant(Enchantment.DAMAGE_ALL, Math.max(1, Math.min(5, (tier + 1) / 2)), true);
        }
    }
}
