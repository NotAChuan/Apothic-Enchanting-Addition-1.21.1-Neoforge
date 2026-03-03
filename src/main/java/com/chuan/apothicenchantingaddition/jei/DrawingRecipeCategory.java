package com.chuan.apothicenchantingaddition.jei;

import com.chuan.apothicenchantingaddition.recipe.RitualDrawingRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class DrawingRecipeCategory implements IRecipeCategory<RitualDrawingRecipe> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/octagram.png");
    private static final ResourceLocation ENTITY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/entity.png");

    private static final int WIDTH = 80;
    private static final int HEIGHT = 80;

    private static final int BG_SIZE = 64;
    private static final int BG_X = (WIDTH - BG_SIZE) / 2;
    private static final int BG_Y = 4;

    private static final int ITEM_X = BG_X + (BG_SIZE - 16) / 2;
    private static final int ITEM_Y = BG_Y + (BG_SIZE - 16) / 2;

    private final IDrawable background;
    private final IDrawable icon;

    public DrawingRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.FLUX_ENCHANTING_TABLE.get()));

//        this.icon = guiHelper.createDrawableItemStack(
//                new ItemStack(ModRegistry.RITUAL_CORE_BLOCK.get())
//        );
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RitualDrawingRecipe> getRecipeType() {
        return ApothicAdditionJeiPlugin.DRAWING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apothicenchantingaddition.category.drawing");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RitualDrawingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_X + 1, ITEM_Y + 1)
                .addIngredients(recipe.tool());
    }

    @Override
    public void draw(RitualDrawingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {

        guiGraphics.blit(BG_TEXTURE, BG_X, BG_Y, 0, 0, BG_SIZE, BG_SIZE, BG_SIZE, BG_SIZE);

        String statusKey;
        if (recipe.consumeItem() && recipe.durabilityCost() > 0) {
            statusKey = "jei.apothicenchantingaddition.drawing.damage";
        } else if (recipe.consumeItem()) {
            statusKey = "jei.apothicenchantingaddition.drawing.consume";
        } else {
            statusKey = "jei.apothicenchantingaddition.drawing.no_consume";
        }

        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(statusKey),
                2,
                HEIGHT - 10,
                0x555555,
                false
        );
    }
}
