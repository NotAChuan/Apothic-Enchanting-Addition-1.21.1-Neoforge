package com.chuan.apothicenchantingaddition.jei;

import com.chuan.apothicenchantingaddition.recipe.RitualCraftingRecipe;
import com.chuan.apothicenchantingaddition.recipe.RitualDrawingRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ModRegistry.MOD_ID, value = Dist.CLIENT)
public class RecipeCache {

    public static List<RitualDrawingRecipe> drawingRecipes = new ArrayList<>();
    public static List<RitualCraftingRecipe> ritualRecipes = new ArrayList<>();
    public static List<JeiSpawnerRecipeView> spawnerRecipes = new ArrayList<>();
    public static List<JeiSpawnerRemoveRecipeView> spawnerRemoveRecipes = new ArrayList<>();

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        var recipeManager = event.getRecipeManager();
        drawingRecipes = recipeManager
                .getAllRecipesFor(ModRegistry.DRAWING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        ritualRecipes = recipeManager
                .getAllRecipesFor(ModRegistry.RITUAL_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        spawnerRecipes = SpawnerJeiRecipeUtil.createSpawnerViews(recipeManager
                .getAllRecipesFor(ModRegistry.SPAWNER_TYPE.get())
                .stream().map(holder -> holder.value()).toList());
        spawnerRemoveRecipes = SpawnerJeiRecipeUtil.createSpawnerRemoveViews(recipeManager
                .getAllRecipesFor(ModRegistry.SPAWNER_REMOVE_TYPE.get())
                .stream().map(holder -> holder.value()).toList());
    }
}
