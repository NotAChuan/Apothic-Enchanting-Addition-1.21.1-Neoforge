package com.chuan.apothicflux.integration.productivebees;

import com.chuan.apothicflux.recipe.SpawnerRecipe;
import cy.jdkdigital.productivebees.common.crafting.ingredient.BeeIngredient;
import cy.jdkdigital.productivebees.init.ModRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Builds Flux Spawner drop profiles from Productive Bees beehive produce recipes.
 *
 * <p>This class is only loaded and called when Productive Bees is present. The
 * loaded recipes are indexed once per {@link RecipeManager}, so normal spawner
 * operation does not rescan every recipe every cycle.</p>
 */
public final class ProductiveBeesRecipeBridge {

    private static final Map<RecipeManager, Map<ResourceLocation, List<SpawnerRecipe.SpawnerDrop>>> DROP_INDEX =
            new WeakHashMap<>();

    private ProductiveBeesRecipeBridge() {
    }

    public static Optional<SpawnerRecipe> createSpawnerRecipe(
            ServerLevel level,
            ResourceLocation beeType,
            EntityType<?> entityType
    ) {
        if (level == null || beeType == null || entityType == null || !ProductiveBeesIntegration.isLoaded()) {
            return Optional.empty();
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId == null) {
            return Optional.empty();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        Map<ResourceLocation, List<SpawnerRecipe.SpawnerDrop>> index =
                DROP_INDEX.computeIfAbsent(recipeManager, ProductiveBeesRecipeBridge::buildDropIndex);

        List<SpawnerRecipe.SpawnerDrop> drops = index.get(beeType);
        if (drops == null || drops.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SpawnerRecipe(entityId, drops));
    }

    private static Map<ResourceLocation, List<SpawnerRecipe.SpawnerDrop>> buildDropIndex(RecipeManager recipeManager) {
        Map<ResourceLocation, List<SpawnerRecipe.SpawnerDrop>> index = new HashMap<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<RecipeHolder<?>> holders =
                recipeManager.getAllRecipesFor((RecipeType) ModRecipeTypes.ADVANCED_BEEHIVE_TYPE.get());

        for (RecipeHolder<?> holder : holders) {
            Object recipe = holder.value();
            BeeIngredient ingredient = readBeeIngredient(recipe);
            if (ingredient == null || ingredient.getBeeType() == null) {
                continue;
            }

            List<SpawnerRecipe.SpawnerDrop> drops = new ArrayList<>();
            for (Map.Entry<ItemStack, ?> entry : readOutputs(recipe).entrySet()) {
                ItemStack output = entry.getKey();
                Object chancedOutput = entry.getValue();
                if (output == null || output.isEmpty() || chancedOutput == null) {
                    continue;
                }

                float chance = readChance(chancedOutput);
                if (chance <= 0.0F) {
                    continue;
                }

                drops.add(new SpawnerRecipe.SpawnerDrop(output.copy(), chance));
            }

            if (!drops.isEmpty()) {
                index.putIfAbsent(ingredient.getBeeType(), List.copyOf(drops));
            }
        }

        return index;
    }

    private static BeeIngredient readBeeIngredient(Object recipe) {
        try {
            Object value = recipe.getClass().getField("ingredient").get(recipe);
            if (value instanceof Supplier<?> supplier) {
                return supplier.get() instanceof BeeIngredient ingredient ? ingredient : null;
            }
        } catch (ReflectiveOperationException ignored) {
            // Keep the same graceful fallback as other optional bridge paths.
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<ItemStack, ?> readOutputs(Object recipe) {
        try {
            Object value = recipe.getClass().getMethod("getRecipeOutputs").invoke(recipe);
            return value instanceof Map<?, ?> map ? (Map<ItemStack, ?>) map : Map.of();
        } catch (ReflectiveOperationException ignored) {
            return Map.of();
        }
    }

    /**
     * Productive Bees bundles its {@code productivelib} dependency as a nested
     * jar, so {@code ChancedOutput} is not available on this mod's compile
     * classpath. The bridge only needs the output chance, which is read through
     * a tiny reflective accessor.
     */
    private static float readChance(Object chancedOutput) {
        try {
            Object value = chancedOutput.getClass().getMethod("chance").invoke(chancedOutput);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException ignored) {
            return 0.0F;
        }
    }
}
