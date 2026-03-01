package com.avertox.jobsystem.recipes;

import com.avertox.jobsystem.model.JobType;
import com.avertox.jobsystem.model.PlayerJobData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RecipeManager {
    private final Map<JobType, Map<String, Integer>> requiredLevels = new HashMap<>();

    public RecipeManager(Iterable<ConfiguredRecipeDefinition> configuredRecipes) {
        for (ConfiguredRecipeDefinition recipe : configuredRecipes) {
            requiredLevels
                    .computeIfAbsent(recipe.jobType(), unused -> new HashMap<>())
                    .put(recipe.key(), Math.max(1, recipe.requiredLevel()));
        }
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
