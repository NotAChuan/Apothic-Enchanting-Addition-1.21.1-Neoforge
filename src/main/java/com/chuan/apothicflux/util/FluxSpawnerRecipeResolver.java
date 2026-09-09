package com.chuan.apothicflux.util;

import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.integration.productivebees.ProductiveBeesIntegration;
import com.chuan.apothicflux.integration.productivebees.ProductiveBeesRecipeBridge;
import com.chuan.apothicflux.recipe.SpawnerRecipe;
import com.chuan.apothicflux.recipe.SpawnerRemoveRecipe;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Identifies one processed spawner input. The optional bee type is only set
     * for Productive Bees spawn eggs so that different configurable bees sharing
     * the {@code productivebees:configurable_bee} entity can use different recipes.
     */
    public record SpawnerInputKey(EntityType<?> entityType, @Nullable ResourceLocation productiveBeeType) {
    }

    public static SpawnPlan buildPlan(
            ServerLevel level,
            Map<SpawnerInputKey, Integer> eggTypeCounts,
            int rollsPerEgg,
            int echoing,
            int spawnCount,
            boolean honeycombBlockMode,
            int honeycombProductivityBonusPercent
    ) {
        if (eggTypeCounts.isEmpty()) {
            return new SpawnPlan(List.of(), 0, false);
        }

        RecipeLookup lookup = RecipeLookup.from(level);
        List<EntityDropProfile> profiles = new ArrayList<>();
        int expEligibleEggs = 0;
        boolean productiveBeesLoaded = ProductiveBeesIntegration.isLoaded();

        for (Map.Entry<SpawnerInputKey, Integer> entry : eggTypeCounts.entrySet()) {
            SpawnerInputKey inputKey = entry.getKey();
            EntityType<?> entityType = inputKey.entityType();
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

            int totalRolls = Math.max(0, rollsPerEgg * eggCount);
            if (totalRolls <= 0) {
                continue;
            }

            SpawnerRecipe customRecipe = null;
            boolean productiveBeeRecipe = false;
            if (inputKey.productiveBeeType() != null && productiveBeesLoaded) {
                customRecipe = lookup.spawnerRecipes.get(entityId);
                if (customRecipe == null) {
                    customRecipe = ProductiveBeesRecipeBridge.createSpawnerRecipe(
                            level,
                            inputKey.productiveBeeType(),
                            entityType,
                            honeycombBlockMode
                    ).orElse(null);
                    productiveBeeRecipe = customRecipe != null;
                }
            }

            if (customRecipe == null) {
                customRecipe = lookup.spawnerRecipes.get(entityId);
            }

            if (customRecipe != null) {
                if (customRecipe.hasDrops()) {
                    int effectiveRolls = productiveBeeRecipe
                            ? applyProductivity(totalRolls, honeycombProductivityBonusPercent, level.random)
                            : totalRolls;
                    if (effectiveRolls > 0) {
                        profiles.add(new EntityDropProfile(entityType, effectiveRolls, customRecipe));
                    }
                }
            } else {
                profiles.add(new EntityDropProfile(entityType, totalRolls, null));
                expEligibleEggs += eggCount;
            }
        }

        int expBase = ApothicAdditionConfig.FLUX_SPAWNER_EXP_BASE_COUNT.get();
        int expCount = expBase * expEligibleEggs * (1 + echoing) * Math.max(1, spawnCount);
        return new SpawnPlan(List.copyOf(profiles), expCount, true);
    }

    private static int applyProductivity(int baseRolls, int bonusPercent, RandomSource random) {
        if (baseRolls <= 0 || bonusPercent <= 0) {
            return baseRolls;
        }

        double multiplier = 1.0D + (bonusPercent / 100.0D);
        double effectiveRolls = baseRolls * multiplier;
        int wholeRolls = (int) Math.floor(effectiveRolls);
        if (random.nextDouble() < effectiveRolls - wholeRolls) {
            wholeRolls++;
        }
        return wholeRolls;
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

    private record RecipeLookup(
            Map<ResourceLocation, SpawnerRecipe> spawnerRecipes,
            Set<ResourceLocation> removedEntities
    ) {
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
