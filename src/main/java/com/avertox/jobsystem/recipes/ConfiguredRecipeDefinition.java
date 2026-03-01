package com.avertox.jobsystem.recipes;

import com.avertox.jobsystem.model.JobType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record ConfiguredRecipeDefinition(
        String key,
        JobType jobType,
        int requiredLevel,
        Material resultMaterial,
        int resultAmount,
        String displayName,
        List<String> lore,
        List<String> shape,
        Map<Character, Material> ingredients
) {
}
