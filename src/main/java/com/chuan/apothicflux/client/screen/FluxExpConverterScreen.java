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

    // 经验数字展示区域（纹理坐标 x:42-134 y:49-62），数字居中
    private static final int XP_TEXT_CENTER_X = 88;
    private static final int XP_TEXT_CENTER_Y = 52;

    // 经验条（纹理坐标）：暗色(空) x:7-168 y:64-69，亮色(满) x:7-168 y:169-174，显示在面板下方 y=169
    private static final int XP_BAR_SRC_X = 7;
    private static final int XP_BAR_SRC_DARK_Y = 64;
    private static final int XP_BAR_SRC_BRIGHT_Y = 169;
    private static final int XP_BAR_W = 161;
    private static final int XP_BAR_H = 5;
    // 经验条显示在暗色条（分隔条）处，亮色填充在暗色轨道内
    private static final int XP_BAR_SCREEN_Y = 64;

    // 按钮（纹理坐标 14x14）：存储=右列(dark x195 / bright x227)，取出=左列(dark x179 / bright x211)，行=1/10/全部
    private static final int BTN_SIZE = 14;
    private static final int BTN_HALF = BTN_SIZE / 2;
    private static final int STORE_DARK_X = 195;
    private static final int STORE_BRIGHT_X = 227;
    private static final int TAKE_DARK_X = 179;
    private static final int TAKE_BRIGHT_X = 211;
    private static final int BTN_ROW_1_Y = 2;
    private static final int BTN_ROW_10_Y = 20;
    private static final int BTN_ROW_ALL_Y = 38;
    // 按钮中心点（屏幕坐标，相对 leftPos/topPos）：存储 全部/10/1 在左，取出 1/10/全部 在右，同一行 y=35
    private static final int STORE_ALL_CX = 19, STORE_ALL_CY = 35;
    private static final int STORE_10_CX = 42, STORE_10_CY = 35;
    private static final int STORE_1_CX = 64, STORE_1_CY = 35;
    private static final int TAKE_1_CX = 110, TAKE_1_CY = 35;
    private static final int TAKE_10_CX = 133, TAKE_10_CY = 35;
    private static final int TAKE_ALL_CX = 157, TAKE_ALL_CY = 35;
    // EnderIO 经验文字样式：绿色主体 (0x80FF20 = 8453920) + 四向 1px 黑色描边
    private static final int XP_TEXT_GREEN = 8453920;
    private static final int XP_TEXT_OUTLINE = 0x000000;

    // 当前被按下的按钮（-1=未按下），用于“正常暗色、按下亮色”渲染
    private int pressedButton = -1;

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
            guiGraphics.renderFakeItem(ghostStack, this.leftPos + 80, this.topPos + 27);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        // 按钮（正常=暗色，按下=亮色）：存储(右列) 全部/10/1 在槽左侧，取出(左列) 1/10/全部 在槽右侧
        drawActionButton(guiGraphics, STORE_ALL_CX, STORE_ALL_CY, STORE_DARK_X, BTN_ROW_ALL_Y, STORE_BRIGHT_X, BTN_ROW_ALL_Y, 2);
        drawActionButton(guiGraphics, STORE_10_CX, STORE_10_CY, STORE_DARK_X, BTN_ROW_10_Y, STORE_BRIGHT_X, BTN_ROW_10_Y, 1);
        drawActionButton(guiGraphics, STORE_1_CX, STORE_1_CY, STORE_DARK_X, BTN_ROW_1_Y, STORE_BRIGHT_X, BTN_ROW_1_Y, 0);
        drawActionButton(guiGraphics, TAKE_1_CX, TAKE_1_CY, TAKE_DARK_X, BTN_ROW_1_Y, TAKE_BRIGHT_X, BTN_ROW_1_Y, 3);
        drawActionButton(guiGraphics, TAKE_10_CX, TAKE_10_CY, TAKE_DARK_X, BTN_ROW_10_Y, TAKE_BRIGHT_X, BTN_ROW_10_Y, 4);
        drawActionButton(guiGraphics, TAKE_ALL_CX, TAKE_ALL_CY, TAKE_DARK_X, BTN_ROW_ALL_Y, TAKE_BRIGHT_X, BTN_ROW_ALL_Y, 5);

        long storedXp = this.menu.getStoredXp();
        String xpText = Long.toString(storedXp);
        float xpCenterX = this.leftPos + XP_TEXT_CENTER_X;
        float xpY = this.topPos + XP_TEXT_CENTER_Y;
        int xOffset = this.font.width(xpText) / 2;

        // 黑色描边（左右上下各偏移 1px）
        guiGraphics.drawString(this.font, xpText, xpCenterX + 1 - xOffset, xpY, XP_TEXT_OUTLINE, false);
        guiGraphics.drawString(this.font, xpText, xpCenterX - 1 - xOffset, xpY, XP_TEXT_OUTLINE, false);
        guiGraphics.drawString(this.font, xpText, xpCenterX - xOffset, xpY + 1, XP_TEXT_OUTLINE, false);
        guiGraphics.drawString(this.font, xpText, xpCenterX - xOffset, xpY - 1, XP_TEXT_OUTLINE, false);
        // 绿色主体（EnderIO 同款绿）
        guiGraphics.drawString(this.font, xpText, xpCenterX - xOffset, xpY, XP_TEXT_GREEN, false);

        // 经验条：暗色(空)铺底，有经验则按进度填充亮色
        int fillWidth = Mth.clamp((int) Math.ceil(XP_BAR_W * Math.min(storedXp / 100000000.0F, 1.0F)), 0, XP_BAR_W);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos + XP_BAR_SRC_X, this.topPos + XP_BAR_SCREEN_Y,
                XP_BAR_SRC_X, XP_BAR_SRC_DARK_Y, XP_BAR_W, XP_BAR_H, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (fillWidth > 0) {
            guiGraphics.blit(GUI_TEXTURE, this.leftPos + XP_BAR_SRC_X, this.topPos + XP_BAR_SCREEN_Y,
                    XP_BAR_SRC_X, XP_BAR_SRC_BRIGHT_Y, fillWidth, XP_BAR_H, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    private void drawActionButton(GuiGraphics guiGraphics, int cx, int cy, int darkU, int darkV, int brightU, int brightV, int actionId) {
        int x = this.leftPos + cx - BTN_HALF;
        int y = this.topPos + cy - BTN_HALF;
        boolean pressed = this.pressedButton == actionId;
        int u = pressed ? brightU : darkU;
        int v = pressed ? brightV : darkV;
        guiGraphics.blit(GUI_TEXTURE, x, y, u, v, BTN_SIZE, BTN_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
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
        if (tryHandleAction(mouseX, mouseY, STORE_ALL_CX, STORE_ALL_CY, 2)) return true;
        if (tryHandleAction(mouseX, mouseY, STORE_10_CX, STORE_10_CY, 1)) return true;
        if (tryHandleAction(mouseX, mouseY, STORE_1_CX, STORE_1_CY, 0)) return true;
        if (tryHandleAction(mouseX, mouseY, TAKE_1_CX, TAKE_1_CY, 3)) return true;
        if (tryHandleAction(mouseX, mouseY, TAKE_10_CX, TAKE_10_CY, 4)) return true;
        if (tryHandleAction(mouseX, mouseY, TAKE_ALL_CX, TAKE_ALL_CY, 5)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.pressedButton = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean tryHandleAction(double mouseX, double mouseY, int centerX, int centerY, int actionId) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (localX >= centerX - BTN_HALF && localX <= centerX + BTN_HALF && localY >= centerY - BTN_HALF && localY <= centerY + BTN_HALF) {
            this.pressedButton = actionId;
            PacketDistributor.sendToServer(new FluxExpConverterActionPayload(menu.getBlockEntity().getBlockPos(), actionId));
            return true;
        }
        return false;
    }

    private Component getHoveredButtonTooltip(int mouseX, int mouseY) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (isInsideButton(localX, localY, STORE_ALL_CX, STORE_ALL_CY)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.store_all");
        if (isInsideButton(localX, localY, STORE_10_CX, STORE_10_CY)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.store_10");
        if (isInsideButton(localX, localY, STORE_1_CX, STORE_1_CY)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.store_1");
        if (isInsideButton(localX, localY, TAKE_1_CX, TAKE_1_CY)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.take_1");
        if (isInsideButton(localX, localY, TAKE_10_CX, TAKE_10_CY)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.take_10");
        if (isInsideButton(localX, localY, TAKE_ALL_CX, TAKE_ALL_CY)) return Component.translatable("gui.apothic_flux.exp_converter.tooltip.take_all");
        return null;
    }

    private boolean isInsideButton(double localX, double localY, int centerX, int centerY) {
        return localX >= centerX - BTN_HALF && localX <= centerX + BTN_HALF && localY >= centerY - BTN_HALF && localY <= centerY + BTN_HALF;
    }
}
