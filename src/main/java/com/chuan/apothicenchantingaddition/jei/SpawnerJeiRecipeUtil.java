package com.chuan.apothicenchantingaddition.jei;

import com.chuan.apothicenchantingaddition.recipe.SpawnerRecipe;
import com.chuan.apothicenchantingaddition.recipe.SpawnerRemoveRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;

public final class SpawnerJeiRecipeUtil {

    private SpawnerJeiRecipeUtil() {
    }

    public static List<JeiSpawnerRecipeView> createSpawnerViews(List<SpawnerRecipe> recipes) {
        List<JeiSpawnerRecipeView> views = new ArrayList<>();
        for (SpawnerRecipe recipe : recipes) {
            ItemStack eggStack = getFirstSpawnEgg(recipe.entity());
            if (!eggStack.isEmpty()) {
                views.add(new JeiSpawnerRecipeView(eggStack, recipe));
            }
        }
        return List.copyOf(views);
    }

    public static List<JeiSpawnerRemoveRecipeView> createSpawnerRemoveViews(List<SpawnerRemoveRecipe> recipes) {
        List<JeiSpawnerRemoveRecipeView> views = new ArrayList<>();
        for (SpawnerRemoveRecipe recipe : recipes) {
            ItemStack eggStack = getFirstSpawnEgg(recipe.entity());
            if (!eggStack.isEmpty()) {
                views.add(new JeiSpawnerRemoveRecipeView(eggStack, recipe));
            }
        }
        return List.copyOf(views);
    }

    public static ItemStack getFirstSpawnEgg(ResourceLocation entityId) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (entityType == null) {
            return ItemStack.EMPTY;
        }

        SpawnEggItem eggItem = SpawnEggItem.byId(entityType);
        return eggItem == null ? ItemStack.EMPTY : eggItem.getDefaultInstance();
    }
}
