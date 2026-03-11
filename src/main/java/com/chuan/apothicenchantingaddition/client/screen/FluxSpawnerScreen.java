package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.menu.FluxSpawnerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.text.NumberFormat;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class FluxSpawnerScreen extends AbstractContainerScreen<FluxSpawnerMenu> {

    private static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apothicenchantingaddition",
            "textures/gui/flux_spawner_gui.png"
    );

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_BAR_X = 52;
    private static final int ENERGY_BAR_Y = 57;
    private static final int ENERGY_BAR_WIDTH = 73;
    private static final int ENERGY_BAR_HEIGHT = 7;

    private static final int ENERGY_TEXTURE_U = 178;
    private static final int ENERGY_TEXTURE_V = 3;
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public FluxSpawnerScreen(FluxSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int energyX = this.leftPos + ENERGY_BAR_X;
        int energyY = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= energyX && mouseX < energyX + ENERGY_BAR_WIDTH && mouseY >= energyY && mouseY < energyY + ENERGY_BAR_HEIGHT) {
            String currentEnergy = FORMATTER.format(this.menu.getEnergy());
            String maxEnergy = FORMATTER.format(1_000_000_000);
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("gui.apothicenchantingaddition.flux_spawner.energy", currentEnergy, maxEnergy),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int energy = this.menu.getEnergy();
        if (energy > 0) {
            int fillWidth = Math.min(ENERGY_BAR_WIDTH, Math.max(0, (int) (ENERGY_BAR_WIDTH * (energy / 1_000_000_000f))));
            if (fillWidth > 0) {
                guiGraphics.blit(
                        GUI_TEXTURE,
                        this.leftPos + ENERGY_BAR_X,
                        this.topPos + ENERGY_BAR_Y,
                        ENERGY_TEXTURE_U,
                        ENERGY_TEXTURE_V,
                        fillWidth,
                        ENERGY_BAR_HEIGHT,
                        TEXTURE_WIDTH,
                        TEXTURE_HEIGHT
                );
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
}
