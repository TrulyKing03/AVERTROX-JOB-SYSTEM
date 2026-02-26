package com.avertox.jobsystem.recipes;

import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RecipeManager {
    private final Map<JobType, Map<String, Integer>> requiredLevels = new HashMap<>();

    public RecipeManager() {
        registerDefaults();
    }

    private void registerDefaults() {
        requiredLevels.put(JobType.FARMER, Map.of(
                "nutrient_stew", 4,
                "harvester_bread", 7
        ));
        requiredLevels.put(JobType.FISHER, Map.of(
                "xp_bait", 4,
                "legend_lure", 8
        ));
        requiredLevels.put(JobType.WOODCUTTER, Map.of(
                "resin_plank", 5,
                "reinforced_handle", 8
        ));
        requiredLevels.put(JobType.MINER, Map.of(
                "light_alloy", 5,
                "vein_core", 8
        ));
        requiredLevels.put(JobType.HUNTER, Map.of(
                "tracker_bait", 4,
                "predator_ration", 8
        ));
    }

    public Set<String> availableRecipes(JobType type) {
        return requiredLevels.getOrDefault(type, Map.of()).keySet();
    }

    public int requiredLevel(JobType type, String recipeKey) {
        return requiredLevels.getOrDefault(type, Map.of()).getOrDefault(recipeKey, Integer.MAX_VALUE);
    }

    public boolean unlock(PlayerJobData data, JobType type, String recipeKey) {
        int required = requiredLevel(type, recipeKey);
        if (data.getLevel() < required) {
            return false;
        }
        data.getUnlockedRecipes().add(recipeKey);
        return true;
    }
}
