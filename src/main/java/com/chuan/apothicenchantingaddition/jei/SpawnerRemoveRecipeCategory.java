package com.chuan.apothicenchantingaddition.jei;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


public class SpawnerRemoveRecipeCategory implements IRecipeCategory<JeiSpawnerRemoveRecipeView> {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 54;
    private static final int SLOT_SIZE = 18;
    private static final int EGG_X = (WIDTH - SLOT_SIZE) / 2;
    private static final int EGG_Y = 18;

    private final IDrawable background;
    private final IDrawable icon;

    public SpawnerRemoveRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.FLUX_SPAWNER_ITEM.get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<JeiSpawnerRemoveRecipeView> getRecipeType() {
        return ApothicAdditionJeiPlugin.SPAWNER_REMOVE_JEI_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apothic_flux.category.flux_spawner_remove");
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
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, JeiSpawnerRemoveRecipeView view, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, EGG_X + 1, EGG_Y + 1)
                .addItemStack(view.spawnEgg().copy())
                .addRichTooltipCallback(RemoveTooltipCallback.INSTANCE);
    }

    @Override
    public void draw(JeiSpawnerRemoveRecipeView view, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawSlotFrame(guiGraphics, EGG_X, EGG_Y);
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xFF8F8F8F);
        guiGraphics.fill(x + 3, y + 3, x + SLOT_SIZE - 3, y + SLOT_SIZE - 3, 0xFF141414);
    }

    private static final class RemoveTooltipCallback implements IRecipeSlotRichTooltipCallback {
        private static final RemoveTooltipCallback INSTANCE = new RemoveTooltipCallback();

        @Override
        public void onRichTooltip(IRecipeSlotView slotView, ITooltipBuilder tooltip) {
            tooltip.add(Component.translatable("jei.apothic_flux.flux_spawner_remove.blocked")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
