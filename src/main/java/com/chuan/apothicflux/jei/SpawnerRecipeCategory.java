package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.recipe.SpawnerRecipe;
import com.chuan.apothicflux.registry.ModRegistry;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class SpawnerRecipeCategory implements IRecipeCategory<JeiSpawnerRecipeView> {

    private static final int WIDTH = 154;
    private static final int HEIGHT = 76;

    private static final int SLOT_SIZE = 18;
    private static final int EGG_X = 8;
    private static final int EGG_Y = 29;
    private static final int ARROW_X = 38;
    private static final int ARROW_Y = 32;
    private static final int GRID_X = 70;
    private static final int GRID_Y = 11;
    private static final int GRID_COLUMNS = 4;
    private static final int GRID_ROWS = 3;
    private static final int MAX_DISPLAYED_DROPS = GRID_COLUMNS * GRID_ROWS;

    private final IDrawable background;
    private final IDrawable icon;

    public SpawnerRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.FLUX_SPAWNER_ITEM.get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<JeiSpawnerRecipeView> getRecipeType() {
        return ApothicAdditionJeiPlugin.SPAWNER_JEI_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apothic_flux.category.flux_spawner");
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
    public void setRecipe(IRecipeLayoutBuilder builder, JeiSpawnerRecipeView view, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, EGG_X + 1, EGG_Y + 1)
                .addItemStack(view.spawnEgg().copy());

        List<SpawnerRecipe.SpawnerDrop> drops = view.recipe().drops();
        int limit = Math.min(MAX_DISPLAYED_DROPS, drops.size());
        for (int index = 0; index < limit; index++) {
            SpawnerRecipe.SpawnerDrop drop = drops.get(index);
            if (drop.stack().isEmpty()) {
                continue;
            }

            int col = index % GRID_COLUMNS;
            int row = index / GRID_COLUMNS;
            int x = GRID_X + col * SLOT_SIZE + 1;
            int y = GRID_Y + row * SLOT_SIZE + 1;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(drop.stack().copy())
                    .addRichTooltipCallback(new ChanceTooltipCallback(drop.chance()));
        }
    }

    @Override
    public void draw(JeiSpawnerRecipeView view, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawSlotFrame(guiGraphics, EGG_X, EGG_Y);
        drawArrow(guiGraphics);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                drawSlotFrame(guiGraphics, GRID_X + col * SLOT_SIZE, GRID_Y + row * SLOT_SIZE);
            }
        }
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xFF8F8F8F);
        guiGraphics.fill(x + 3, y + 3, x + SLOT_SIZE - 3, y + SLOT_SIZE - 3, 0xFF141414);
    }

    private static void drawArrow(GuiGraphics guiGraphics) {
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("→"), ARROW_X, ARROW_Y, 0xFF808080, false);
    }

    private static String formatChancePercent(float chance) {
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return format.format(chance * 100.0F) + "%";
    }

    private static final class ChanceTooltipCallback implements IRecipeSlotRichTooltipCallback {
        private final float chance;

        private ChanceTooltipCallback(float chance) {
            this.chance = chance;
        }

        @Override
        public void onRichTooltip(IRecipeSlotView slotView, ITooltipBuilder tooltip) {
            if (this.chance >= 1.0F) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jei.apothic_flux.flux_spawner.probability",
                    formatChancePercent(this.chance)
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
