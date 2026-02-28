package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.menu.FluxAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

public class FluxAnvilScreen extends AnvilScreen {

    private final FluxAnvilMenu menu;

    public FluxAnvilScreen(AnvilMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.menu = (FluxAnvilMenu) pMenu;
    }

    // 1. 绘制背景与左侧的 FE 能量条
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 先让原版画出铁砧的基础 GUI 和输入框背景
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        // 设定能量条的位置（在铁砧主界面左侧外挂）
        int barX = this.leftPos - 14;
        int barY = this.topPos + 10;
        int barWidth = 12;
        int barHeight = 66;

        // 绘制能量条底框 (深灰色)
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

        // 计算并绘制当前能量比例 (纯正的 FE 红色)
        int maxEnergy = this.menu.getMaxEnergy();
        int currentEnergy = this.menu.getEnergy();

        if (maxEnergy > 0 && currentEnergy > 0) {
            int filledHeight = (int) (((float) currentEnergy / maxEnergy) * (barHeight - 2));
            guiGraphics.fill(barX + 1, barY + barHeight - 1 - filledHeight, barX + barWidth - 1, barY + barHeight - 1, 0xFFCC2222);
        }
    }

    // 2. 拦截并重写文字渲染 (将“经验”替换为“FE耗电”)
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 重点：我们故意不调用 super.renderLabels()，因为原版会在那里画“附魔花费：X 级”
        // 我们手动补上标题和背包文字的渲染
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // 绘制我们的 FE 耗电提示
        int feCost = this.menu.getFeCost();
        if (feCost > 0) {
            // 判断电量是否足够，足够显绿，不足显红
            boolean hasEnoughEnergy = this.menu.getEnergy() >= feCost || this.minecraft.player.getAbilities().instabuild;
            int color = hasEnoughEnergy ? 8453920 : 16736352; // 8453920=绿, 16736352=红

            Component costText = Component.translatable("gui.apothicenchantingaddition.anvil.cost", feCost);

            // 计算文字宽度，将其靠右对齐放置在原版经验提示的位置
            int textWidth = this.font.width(costText);
            int textX = this.imageWidth - 8 - textWidth;
            int textY = 67;

            // 画一个半透明的黑色背景框增加辨识度（原版铁砧质感）
            guiGraphics.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + 10, 1325400064);
            // 画出最终的耗电文字
            guiGraphics.drawString(this.font, costText, textX, textY, color, false);
        }
    }

    // 3. 绘制鼠标悬停在能量条上的 Tooltip 提示
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 检测鼠标是否悬停在左侧的能量条上
        int barX = this.leftPos - 14;
        int barY = this.topPos + 10;
        int barWidth = 12;
        int barHeight = 66;

        if (mouseX >= barX && mouseX <= barX + barWidth && mouseY >= barY && mouseY <= barY + barHeight) {
            Component tooltip = Component.translatable(
                    "gui.apothicenchantingaddition.energy.fe",
                    this.menu.getEnergy(),
                    this.menu.getMaxEnergy()
            );
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}
