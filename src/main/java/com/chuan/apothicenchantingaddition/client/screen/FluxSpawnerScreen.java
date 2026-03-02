package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.menu.FluxSpawnerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.text.NumberFormat;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class FluxSpawnerScreen extends AbstractContainerScreen<FluxSpawnerMenu> {

    // 使用格式化器让 10 亿能量显示为 1,000,000,000，更易读
    private static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);

    public FluxSpawnerScreen(FluxSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        // 调整玩家背包文字的位置
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 绘制能量条悬浮提示 (Tooltip)
        int energyX = this.leftPos + 12;
        int energyY = this.topPos + 20;
        if (mouseX >= energyX && mouseX < energyX + 16 && mouseY >= energyY && mouseY < energyY + 50) {
            String currentEnergy = FORMATTER.format(this.menu.getEnergy());
            String maxEnergy = FORMATTER.format(1_000_000_000);
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.apothicenchantingaddition.flux_spawner.energy", currentEnergy, maxEnergy),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 1. 绘制纯代码风格的深灰色主背景板
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);

        // 绘制边框高光和阴影，增加立体感
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFFFFFFFF); // 顶部白边
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFFFFFFFF); // 左侧白边
        guiGraphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF555555); // 右侧黑边
        guiGraphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF555555); // 底部黑边

        // 2. 绘制所有槽位 (Slot) 的背景凹槽
        for (Slot slot : this.menu.slots) {
            int slotX = this.leftPos + slot.x;
            int slotY = this.topPos + slot.y;
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B); // 槽位深灰底色
            guiGraphics.fill(slotX, slotY, slotX + 15, slotY + 1, 0xFF373737); // 上阴影
            guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 15, 0xFF373737); // 左阴影
            guiGraphics.fill(slotX + 15, slotY + 1, slotX + 16, slotY + 16, 0xFFFFFFFF); // 右高光
            guiGraphics.fill(slotX + 1, slotY + 15, slotX + 16, slotY + 16, 0xFFFFFFFF); // 下高光
        }

        // 3. 绘制能量条背景 (空槽)
        int energyX = this.leftPos + 12;
        int energyY = this.topPos + 20;
        guiGraphics.fill(energyX, energyY, energyX + 16, energyY + 50, 0xFF333333);

        // 4. 绘制能量条填充 (红色渐变)
        float fillRatio = (float) this.menu.getEnergy() / 1_000_000_000f;
        int fillHeight = (int) (50 * fillRatio);
        if (fillHeight > 0) {
            int fillTopY = energyY + 50 - fillHeight;
            // 绘制红彤彤的能量条！
            guiGraphics.fill(energyX + 1, fillTopY, energyX + 15, energyY + 50, 0xFFBF0000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 绘制玩家背包文字
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 缩小字体绘制右侧的神化属性 (缩放 0.8 倍以防文字越界)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);

        // 注意：因为缩放了 0.8，坐标需要除以 0.8 来适配绝对位置 (X = 130 / 0.8 = 162)
        int textX = (int) (130 / 0.8f);
        int textY = (int) (20 / 0.8f);
        int spacing = (int) (12 / 0.8f);

        Component rsState = this.menu.isRedstoneControl()
                ? Component.translatable("gui.apothicenchantingaddition.flux_spawner.on")
                : Component.translatable("gui.apothicenchantingaddition.flux_spawner.off");

        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.flux_spawner.min_delay", this.menu.getMinDelay()), textX, textY, 0x005500, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.flux_spawner.max_delay", this.menu.getMaxDelay()), textX, textY + spacing, 0x550000, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.flux_spawner.spawn_count", this.menu.getSpawnCount()), textX, textY + spacing * 2, 0x0000AA, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.flux_spawner.redstone", rsState), textX, textY + spacing * 3, 0xAA0000, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.flux_spawner.echoing", this.menu.getEchoing()), textX, textY + spacing * 4, 0xAA00AA, false);

        guiGraphics.pose().popPose();
    }
}
