package com.chuan.apothicenchantingaddition.util;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.recipe.SpawnerRecipe;
import com.chuan.apothicenchantingaddition.recipe.SpawnerRemoveRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FluxSpawnerRecipeResolver {

    private FluxSpawnerRecipeResolver() {
    }

    public record EntityDropProfile(EntityType<?> entityType, int totalRolls, SpawnerRecipe recipe) {
        public boolean usesLootTable() {
            return this.recipe == null;
        }
    }

    public record SpawnPlan(List<EntityDropProfile> profiles, int expCount, boolean hasValidEggs) {
        public boolean producesItems() {
            return this.expCount > 0 || !this.profiles.isEmpty();
        }
    }

    public static SpawnPlan buildPlan(ServerLevel level, Map<EntityType<?>, Integer> eggTypeCounts, int rollsPerEgg, int echoing) {
        if (eggTypeCounts.isEmpty()) {
            return new SpawnPlan(List.of(), 0, false);
        }

        RecipeLookup lookup = RecipeLookup.from(level);
        List<EntityDropProfile> profiles = new ArrayList<>();
        int expEligibleEggs = 0;

        for (Map.Entry<EntityType<?>, Integer> entry : eggTypeCounts.entrySet()) {
            EntityType<?> entityType = entry.getKey();
            int eggCount = entry.getValue();
            if (eggCount <= 0) {
                continue;
            }

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (entityId == null) {
                continue;
            }

            if (lookup.removedEntities.contains(entityId)) {
                continue;
            }

            SpawnerRecipe customRecipe = lookup.spawnerRecipes.get(entityId);
            int totalRolls = Math.max(0, rollsPerEgg * eggCount);
            if (totalRolls <= 0) {
                continue;
            }

            if (customRecipe != null) {
                if (customRecipe.hasDrops()) {
                    profiles.add(new EntityDropProfile(entityType, totalRolls, customRecipe));
                }
            } else {
                profiles.add(new EntityDropProfile(entityType, totalRolls, null));
                expEligibleEggs += eggCount;
            }
        }

        int expBase = ApothicAdditionConfig.FLUX_SPAWNER_EXP_BASE_COUNT.get();
        int expCount = expBase * expEligibleEggs * (1 + echoing);
        return new SpawnPlan(List.copyOf(profiles), expCount, true);
    }

    public static boolean canInsertEgg(Level level, SpawnEggItem egg) {
        if (level == null) {
            return true;
        }
        return !isRemoved(level, egg.getType(ItemStack.EMPTY));
    }

    public static boolean isRemoved(Level level, EntityType<?> entityType) {
        if (level == null || entityType == null) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId == null) {
            return false;
        }
        return RecipeLookup.from(level).removedEntities.contains(entityId);
    }

    private record RecipeLookup(Map<ResourceLocation, SpawnerRecipe> spawnerRecipes, Set<ResourceLocation> removedEntities) {
        private static RecipeLookup from(Level level) {
            Map<ResourceLocation, SpawnerRecipe> spawnerRecipes = new HashMap<>();
            for (var holder : level.getRecipeManager().getAllRecipesFor(ModRegistry.SPAWNER_TYPE.get())) {
                SpawnerRecipe recipe = holder.value();
                spawnerRecipes.putIfAbsent(recipe.entity(), recipe);
            }

            Set<ResourceLocation> removedEntities = new HashSet<>();
            for (var holder : level.getRecipeManager().getAllRecipesFor(ModRegistry.SPAWNER_REMOVE_TYPE.get())) {
                SpawnerRemoveRecipe recipe = holder.value();
                removedEntities.add(recipe.entity());
            }

            return new RecipeLookup(spawnerRecipes, removedEntities);
        }
    }
}
