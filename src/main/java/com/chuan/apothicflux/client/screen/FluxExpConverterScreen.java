package com.chuan.apothicflux.client.screen;

import com.chuan.apothicflux.menu.FluxExpConverterMenu;
import com.chuan.apothicflux.network.FluxExpConverterActionPayload;
import com.chuan.apothicflux.registry.ModRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class FluxExpConverterScreen extends AbstractContainerScreen<FluxExpConverterMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("apothic_flux", "textures/gui/exp_converter.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int BAR_X = 43;
    private static final int BAR_Y = 48;
    private static final int BAR_WIDTH = 91;
    private static final int BAR_HEIGHT = 6;
    private static final int BAR_TEXT_Y = 39;

    private static final int STORE_ALL_U = 189;
    private static final int STORE_ALL_V = 0;
    private static final int STORE_10_U = 183;
    private static final int STORE_10_V = 3;
    private static final int STORE_1_U = 177;
    private static final int STORE_1_V = 6;
    private static final int TAKE_1_U = 177;
    private static final int TAKE_1_V = 10;
    private static final int TAKE_10_U = 183;
    private static final int TAKE_10_V = 10;
    private static final int TAKE_ALL_U = 189;
    private static final int TAKE_ALL_V = 10;
    private static final int BUTTON_Y = 56;
    private static final int BUTTON_10_HEIGHT_OFFSET = 0;
    private static final int BUTTON_7_HEIGHT_OFFSET = 1;
    private static final int BUTTON_4_HEIGHT_OFFSET = 3;

    public FluxExpConverterScreen(FluxExpConverterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelX = 7;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (!this.menu.getSlot(0).hasItem()) {
            ItemStack ghostStack = ModRegistry.SOLIDIFIED_FLUX_EXPERIENCE.get().getDefaultInstance();
            RenderSystem.setShaderColor(0.6f, 0.8f, 0.8f, 0.8f);
            guiGraphics.renderFakeItem(ghostStack, this.leftPos + 80, this.topPos + 9);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        drawButton(guiGraphics, this.leftPos + 48, this.topPos + BUTTON_Y + BUTTON_10_HEIGHT_OFFSET, STORE_ALL_U, STORE_ALL_V, 6, 10);
        drawButton(guiGraphics, this.leftPos + 63, this.topPos + BUTTON_Y + BUTTON_7_HEIGHT_OFFSET, STORE_10_U, STORE_10_V, 6, 7);
        drawButton(guiGraphics, this.leftPos + 78, this.topPos + BUTTON_Y + BUTTON_4_HEIGHT_OFFSET, STORE_1_U, STORE_1_V, 6, 4);
        drawButton(guiGraphics, this.leftPos + 93, this.topPos + BUTTON_Y + BUTTON_4_HEIGHT_OFFSET, TAKE_1_U, TAKE_1_V, 6, 4);
        drawButton(guiGraphics, this.leftPos + 108, this.topPos + BUTTON_Y + BUTTON_7_HEIGHT_OFFSET, TAKE_10_U, TAKE_10_V, 6, 7);
        drawButton(guiGraphics, this.leftPos + 123, this.topPos + BUTTON_Y + BUTTON_10_HEIGHT_OFFSET, TAKE_ALL_U, TAKE_ALL_V, 6, 10);

        long storedXp = this.menu.getStoredXp();
        guiGraphics.drawCenteredString(this.font, Component.literal(Long.toString(storedXp)), this.leftPos + BAR_X + BAR_WIDTH / 2, this.topPos + BAR_TEXT_Y, 0xFFFFFF);
        if (storedXp > 0L) {
            int fillWidth = Mth.clamp((int) Math.ceil(BAR_WIDTH * Math.min(storedXp / 100000000.0F, 1.0F)), 0, BAR_WIDTH);
            if (fillWidth > 0) {
                guiGraphics.blit(GUI_TEXTURE, this.leftPos + BAR_X, this.topPos + BAR_Y, 0, 166, fillWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(GUI_TEXTURE, x, y, u, v, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component tooltip = getHoveredButtonTooltip(mouseX, mouseY);
        if (tooltip != null) {
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tryHandleAction(mouseX, mouseY, 50.5D, 61.0D, 2)) return true;
        if (tryHandleAction(mouseX, mouseY, 65.5D, 61.0D, 1)) return true;
        if (tryHandleAction(mouseX, mouseY, 80.5D, 61.0D, 0)) return true;
        if (tryHandleAction(mouseX, mouseY, 95.5D, 61.0D, 3)) return true;
        if (tryHandleAction(mouseX, mouseY, 110.5D, 61.0D, 4)) return true;
        if (tryHandleAction(mouseX, mouseY, 125.5D, 61.0D, 5)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean tryHandleAction(double mouseX, double mouseY, double centerX, double centerY, int actionId) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (localX >= centerX - 2.0D && localX <= centerX + 2.0D && localY >= centerY - 5.0D && localY <= centerY + 5.0D) {
            PacketDistributor.sendToServer(new FluxExpConverterActionPayload(menu.getBlockEntity().getBlockPos(), actionId));
            return true;
        }
        return false;
    }

    private Component getHoveredButtonTooltip(int mouseX, int mouseY) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (isInsideButton(localX, localY, 50.5D, 61.0D)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.store_all");
        if (isInsideButton(localX, localY, 65.5D, 61.0D)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.store_10");
        if (isInsideButton(localX, localY, 80.5D, 61.0D)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.store_1");
        if (isInsideButton(localX, localY, 95.5D, 61.0D)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.take_1");
        if (isInsideButton(localX, localY, 110.5D, 61.0D)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.take_10");
        if (isInsideButton(localX, localY, 125.5D, 61.0D)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.take_all");
        return null;
    }

    private boolean isInsideButton(double localX, double localY, double centerX, double centerY) {
        return localX >= centerX - 2.0D && localX <= centerX + 2.0D && localY >= centerY - 5.0D && localY <= centerY + 5.0D;
    }
}
