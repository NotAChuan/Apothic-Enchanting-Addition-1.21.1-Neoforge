package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.menu.FluxAnvilMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

import java.lang.reflect.Field;

public class FluxAnvilScreen extends AnvilScreen {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("apothicenchantingaddition", "textures/gui/flux_anvil_gui.png");

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_BAR_X = 21;
    private static final int ENERGY_BAR_Y = 31;
    private static final int ENERGY_BAR_WIDTH = 26;
    private static final int ENERGY_BAR_HEIGHT = 6;
    private static final int ENERGY_BAR_U = 21;
    private static final int ENERGY_BAR_V = 31;
    private static final int ENERGY_FILL_U = 178;
    private static final int ENERGY_FILL_V = 23;

    private static final int NAME_BOX_X = 59;
    private static final int NAME_BOX_Y = 20;
    private static final int NAME_BOX_WIDTH = 110;
    private static final int NAME_BOX_HEIGHT = 16;
    private static final int NAME_BOX_ACTIVE_U = 0;
    private static final int NAME_BOX_ACTIVE_V = 166;
    private static final int NAME_BOX_INACTIVE_U = 0;
    private static final int NAME_BOX_INACTIVE_V = 182;

    private static final int ARROW_X = 99;
    private static final int ARROW_Y = 45;
    private static final int ARROW_WIDTH = 28;
    private static final int ARROW_HEIGHT = 21;
    private static final int ARROW_NORMAL_U = 99;
    private static final int ARROW_NORMAL_V = 45;
    private static final int ARROW_DISABLED_U = 176;
    private static final int ARROW_DISABLED_V = 0;

    private final FluxAnvilMenu fluxMenu;
    private EditBox cachedNameBox;

    public FluxAnvilScreen(AnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.fluxMenu = (FluxAnvilMenu) menu;
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 7;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateNameBoxState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        this.renderNameBoxState(guiGraphics, x, y);
        this.renderArrow(guiGraphics, x, y);
        this.renderEnergyBar(guiGraphics, x, y);
        this.updateNameBoxState();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
//        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        int feCost = this.fluxMenu.getFeCost();
        if (feCost > 0) {
            boolean enough = this.fluxMenu.getEnergy() >= feCost || this.minecraft.player.getAbilities().instabuild;
            int color = enough ? 8453920 : 16736352;

            Component costText = Component.translatable("gui.apothicenchantingaddition.anvil.cost", feCost);
            int textWidth = this.font.width(costText);
            int textX = this.imageWidth - 8 - textWidth;
            int textY = 67;

            guiGraphics.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + 10, 1325400064);
            guiGraphics.drawString(this.font, costText, textX, textY, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.hasInputItem() && this.isMouseOverNameBox(mouseX, mouseY)) {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = this.leftPos + ENERGY_BAR_X;
        int y = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= x && mouseX < x + ENERGY_BAR_WIDTH && mouseY >= y && mouseY < y + ENERGY_BAR_HEIGHT) {
            Component tooltip = Component.translatable(
                    "gui.apothicenchantingaddition.energy.fe",
                    this.fluxMenu.getEnergy(),
                    this.fluxMenu.getMaxEnergy()
            );
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderNameBoxState(GuiGraphics guiGraphics, int x, int y) {
        if (this.hasInputItem()) {
            guiGraphics.blit(
                    GUI_TEXTURE,
                    x + NAME_BOX_X,
                    y + NAME_BOX_Y,
                    NAME_BOX_ACTIVE_U,
                    NAME_BOX_ACTIVE_V,
                    NAME_BOX_WIDTH,
                    NAME_BOX_HEIGHT,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT
            );
        } else {
            guiGraphics.blit(
                    GUI_TEXTURE,
                    x + NAME_BOX_X,
                    y + NAME_BOX_Y,
                    NAME_BOX_INACTIVE_U,
                    NAME_BOX_INACTIVE_V,
                    NAME_BOX_WIDTH,
                    NAME_BOX_HEIGHT,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT
            );
        }
    }

    private void renderArrow(GuiGraphics guiGraphics, int x, int y) {
        if (this.fluxMenu.getSlot(2).hasItem()) {
            guiGraphics.blit(
                    GUI_TEXTURE,
                    x + ARROW_X,
                    y + ARROW_Y,
                    ARROW_NORMAL_U,
                    ARROW_NORMAL_V,
                    ARROW_WIDTH,
                    ARROW_HEIGHT,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT
            );
        } else {
            guiGraphics.blit(
                    GUI_TEXTURE,
                    x + ARROW_X,
                    y + ARROW_Y,
                    ARROW_DISABLED_U,
                    ARROW_DISABLED_V,
                    ARROW_WIDTH,
                    ARROW_HEIGHT,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT
            );
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(
                GUI_TEXTURE,
                x + ENERGY_BAR_X,
                y + ENERGY_BAR_Y,
                ENERGY_BAR_U,
                ENERGY_BAR_V,
                ENERGY_BAR_WIDTH,
                ENERGY_BAR_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

        int energy = this.fluxMenu.getEnergy();
        int maxEnergy = this.fluxMenu.getMaxEnergy();
        if (energy <= 0 || maxEnergy <= 0) {
            return;
        }

        int filled = Math.min(ENERGY_BAR_WIDTH, Math.max(1, Math.round((energy / (float) maxEnergy) * ENERGY_BAR_WIDTH)));

        guiGraphics.blit(
                GUI_TEXTURE,
                x + ENERGY_BAR_X,
                y + ENERGY_BAR_Y,
                ENERGY_FILL_U,
                ENERGY_FILL_V,
                filled,
                ENERGY_BAR_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    private void updateNameBoxState() {
        EditBox nameBox = this.getNameBox();
        if (nameBox == null) {
            return;
        }

        boolean editable = this.hasInputItem();
        nameBox.setEditable(editable);

        if (editable) {
            nameBox.setVisible(true);
        } else {
            nameBox.setFocused(false);
            nameBox.setVisible(false);
        }
    }

    private boolean hasInputItem() {
        return this.fluxMenu.getSlot(0).hasItem();
    }

    private boolean isMouseOverNameBox(double mouseX, double mouseY) {
        int x = this.leftPos + NAME_BOX_X;
        int y = this.topPos + NAME_BOX_Y;
        return mouseX >= x && mouseX < x + NAME_BOX_WIDTH && mouseY >= y && mouseY < y + NAME_BOX_HEIGHT;
    }

    private EditBox getNameBox() {
        if (this.cachedNameBox != null) {
            return this.cachedNameBox;
        }

        Class<?> current = AnvilScreen.class;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (EditBox.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(this);
                        if (value instanceof EditBox editBox) {
                            this.cachedNameBox = editBox;
                            return editBox;
                        }
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }

        return null;
    }
}
