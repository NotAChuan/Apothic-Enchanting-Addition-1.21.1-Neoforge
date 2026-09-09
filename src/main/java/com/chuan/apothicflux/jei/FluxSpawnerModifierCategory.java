package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.registry.ModRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FluxSpawnerModifierCategory implements IRecipeCategory<FluxSpawnerModifierView> {

    private static final int WIDTH = 169;
    private static final int HEIGHT = 75;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("apothic_spawners", "textures/gui/spawner_jei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FluxSpawnerModifierCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(TEXTURE, 0, 0, WIDTH, HEIGHT).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.FLUX_SPAWNER_ITEM.get()));
    }

    @Override
    public RecipeType<FluxSpawnerModifierView> getRecipeType() {
        return ApothicAdditionJeiPlugin.FLUX_SPAWNER_MODIFIER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apothic_flux.category.flux_spawner_modifier");
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
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluxSpawnerModifierView view, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 11)
                .addIngredients(view.mainhand());

        boolean hasOffhand = view.hasOffhand();
        if (hasOffhand) {
            builder.addSlot(RecipeIngredientRole.INPUT, 11, 48)
                    .addIngredients(view.offhand());
        }

        ItemStack fluxSpawner = new ItemStack(ModRegistry.FLUX_SPAWNER_ITEM.get());
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
                .addIngredient(VanillaTypes.ITEM_STACK, fluxSpawner);
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addIngredient(VanillaTypes.ITEM_STACK, fluxSpawner.copy());
    }

    @Override
    public void draw(FluxSpawnerModifierView view, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        boolean hasOffhand = view.hasOffhand();
        if (!hasOffhand) {
            guiGraphics.blit(TEXTURE, 1, 31, 0, 0, 88, 28, 34,256, 256);
        }

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0.0D, 0.5D, 0.0D);
        guiGraphics.renderFakeItem(new ItemStack(ModRegistry.FLUX_SPAWNER_ITEM.get()), 31, 29);
        pose.popPose();

        drawHoverHint(guiGraphics, font, mouseX, mouseY, -1, 9, 13, 25,
                List.of(Component.translatable("jei.apothic_flux.flux_spawner_modifier.mainhand")));

        if (hasOffhand) {
            List<Component> offhandTooltip = new java.util.ArrayList<>();
            offhandTooltip.add(Component.translatable("jei.apothic_flux.flux_spawner_modifier.offhand"));
            if (!view.consumesOffhand()) {
                offhandTooltip.add(Component.translatable("jei.apothic_flux.flux_spawner_modifier.offhand_not_consumed"));
            }
            drawHoverHint(guiGraphics, font, mouseX, mouseY, -1, 9, 50, 62, offhandTooltip);
        }

        drawHoverHint(guiGraphics, font, mouseX, mouseY, 33, 49, 30, 46,
                List.of(Component.translatable("jei.apothic_flux.flux_spawner_modifier.right_click")));

        List<FluxSpawnerModifierLine> lines = view.lines();
        int x = 168;
        int y = 37 - (lines.size() * 9 + 2) / 2 + 2;

        for (FluxSpawnerModifierLine line : lines) {
            int width = font.width(line.display());
            boolean hovered = mouseX >= x - width && mouseX < x && mouseY >= y && mouseY < y + 9;
            guiGraphics.drawString(font, line.display(), x - width, y, hovered ? 0xFFE0E0E0 : 0xFF808080, false);

            if (hovered) {
                guiGraphics.renderComponentTooltip(font, line.tooltip(), (int) mouseX, (int) mouseY);
            }

            y += 9;
        }
    }

    private static void drawHoverHint(GuiGraphics guiGraphics, Font font,
                                      double mouseX, double mouseY,
                                      int minX, int maxX, int minY, int maxY,
                                      List<Component> tooltip) {
        if (mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
            guiGraphics.renderComponentTooltip(font, tooltip, (int) mouseX, (int) mouseY);
        }
    }
}
