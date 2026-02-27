package com.chuan.apothicenchantingaddition.jei;

import com.chuan.apothicenchantingaddition.recipe.RitualCraftingRecipe;
import com.chuan.apothicenchantingaddition.recipe.RitualDrawingRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class ApothicAdditionJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "jei_plugin");

    // 在 Plugin 类中统一持有唯一的 RecipeType 实例
    public static final RecipeType<RitualDrawingRecipe> DRAWING_TYPE =
            RecipeType.create(ModRegistry.MOD_ID, "drawing", RitualDrawingRecipe.class);

    public static final RecipeType<RitualCraftingRecipe> RITUAL_TYPE =
            RecipeType.create(ModRegistry.MOD_ID, "ritual", RitualCraftingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        try {
            IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
            registration.addRecipeCategories(
                    new DrawingRecipeCategory(guiHelper),
                    new RitualRecipeCategory(guiHelper)
            );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            registration.addRecipes(DRAWING_TYPE, RecipeCache.drawingRecipes);
            registration.addRecipes(RITUAL_TYPE, RecipeCache.ritualRecipes);
            return;
        }

        var recipeManager = connection.getRecipeManager();

        var drawingRecipes = recipeManager
                .getAllRecipesFor(ModRegistry.DRAWING_TYPE.get())
                .stream()
                .map(holder -> holder.value())
                .toList();

        var ritualRecipes = recipeManager
                .getAllRecipesFor(ModRegistry.RITUAL_TYPE.get())
                .stream()
                .map(holder -> holder.value())
                .toList();

        try {
            registration.addRecipes(DRAWING_TYPE, drawingRecipes);
            registration.addRecipes(RITUAL_TYPE, ritualRecipes);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {}

    @Override
    public void onRuntimeUnavailable() {}
}
