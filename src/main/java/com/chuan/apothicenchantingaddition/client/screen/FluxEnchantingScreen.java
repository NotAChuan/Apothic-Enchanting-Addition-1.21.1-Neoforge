package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.menu.FluxEnchantingMenu;
import com.chuan.apothicenchantingaddition.network.FluxActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class FluxEnchantingScreen extends AbstractContainerScreen<FluxEnchantingMenu> {

    private final int MAX_ENERGY = 1000000000;

    public FluxEnchantingScreen(FluxEnchantingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 1000; // 隐藏默认碍事的 "Inventory" 文字
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 【修改点】：按钮移动到槽位2上方 (x=35, y=25)，宽 22 像素，且文字改为纯 "刷新"
        this.addRenderableWidget(Button.builder(Component.literal("刷新"), b -> {
            PacketDistributor.sendToServer(new FluxActionPayload(menu.getBlockEntity().getBlockPos(), 0));
        }).bounds(x + 35, y + 25, 22, 14).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, 0xFFFFFFFF);
        guiGraphics.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, 0xFF555555);
        guiGraphics.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        // 绘制槽位 0(附魔) 和 槽位 1(青金石)
        drawSlotBackground(guiGraphics, x + 14, y + 46, 18, 18);
        drawSlotBackground(guiGraphics, x + 34, y + 46, 18, 18);

        // 【新增】：绘制玩家 3x9 物品栏背景槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                drawSlotBackground(guiGraphics, x + 7 + j * 18, y + 83 + i * 18, 18, 18);
            }
        }
        // 【新增】：绘制玩家快捷栏 1x9 背景槽位
        for (int k = 0; k < 9; ++k) {
            drawSlotBackground(guiGraphics, x + 7 + k * 18, y + 141, 18, 18);
        }

        // 绘制左侧能量条
        int energyBarX = x + 4;
        int energyBarY = y + 10;
        int energyBarWidth = 6;
        int energyBarHeight = 70;
        drawSlotBackground(guiGraphics, energyBarX, energyBarY, energyBarWidth, energyBarHeight);
        int currentEnergy = menu.getEnergy();
        if (currentEnergy > 0) {
            int fillHeight = (int) ((currentEnergy / (float) MAX_ENERGY) * (energyBarHeight - 2));
            guiGraphics.fill(energyBarX + 1, energyBarY + energyBarHeight - 1 - fillHeight, energyBarX + energyBarWidth - 1, energyBarY + energyBarHeight - 1, 0xFFFF0000);
        }

        // 绘制附魔选项按钮
        int baseCost = ApothicAdditionConfig.FLUX_ENCHANTER_BASE_COST.get();
        for (int i = 0; i < 3; i++) {
            int btnX = x + 60;
            int btnY = y + 14 + 19 * i;
            int costLevel = menu.costs[i];
            int feCost = costLevel * baseCost;

            if (costLevel == 0) {
                drawSlotBackground(guiGraphics, btnX, btnY, 108, 19);
            } else {
                boolean canAfford = currentEnergy >= feCost;
                int color = canAfford ? 0xFF00FF00 : 0xFFFF0000;
                guiGraphics.fill(btnX, btnY, btnX + 108, btnY + 19, 0xFF555555);
                guiGraphics.drawString(this.font, Component.literal("Lvl " + costLevel), btnX + 2, btnY + 2, color, false);
                guiGraphics.drawString(this.font, Component.literal(feCost + " FE"), btnX + 2, btnY + 10, canAfford ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            }
        }

        // 【新增】：绘制神化三大属性 (位阶、量子化、阿卡那) 悬停显示在附魔选项框的正下方
        int statY = y + 73;
        guiGraphics.drawString(this.font, Component.literal("E: " + String.format("%.1f", menu.getEterna())), x + 60, statY, 0x55FF55, false);
        guiGraphics.drawString(this.font, Component.literal("Q: " + String.format("%.1f%%", menu.getQuanta())), x + 95, statY, 0xFF5555, false);
        guiGraphics.drawString(this.font, Component.literal("A: " + String.format("%.1f%%", menu.getArcana())), x + 135, statY, 0xFF55FF, false);
    }

    private void drawSlotBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF373737);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF373737);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        for (int i = 0; i < 3; i++) {
            int btnX = x + 60;
            int btnY = y + 14 + 19 * i;
            if (mouseX >= btnX && mouseX <= btnX + 108 && mouseY >= btnY && mouseY <= btnY + 19) {
                int costLevel = menu.costs[i];
                int feCost = costLevel * ApothicAdditionConfig.FLUX_ENCHANTER_BASE_COST.get();
                if (costLevel > 0 && menu.getEnergy() >= feCost) {
                    PacketDistributor.sendToServer(new FluxActionPayload(menu.getBlockEntity().getBlockPos(), i + 1));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // ================= 【新增：附魔按钮的魔咒线索提示】 =================
        for (int i = 0; i < 3; i++) {
            int btnX = x + 60;
            int btnY = y + 14 + 19 * i;

            if (mouseX >= btnX && mouseX <= btnX + 108 && mouseY >= btnY && mouseY <= btnY + 19) {
                int costLevel = menu.costs[i];
                if (costLevel > 0) {
                    java.util.List<Component> tooltip = new java.util.ArrayList<>();

                    // 直接从 Menu 缓存中读取同步过来的线索
                    java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> clues = menu.clientClues[i];

                    if (clues != null && !clues.isEmpty()) {
                        for (net.minecraft.world.item.enchantment.EnchantmentInstance clue : clues) {
                            // 调用 getFullname，神化的 Mixin 会在这里自动接管并注入彩色 TooltipUtil 样式！
                            Component enchantName = net.minecraft.world.item.enchantment.Enchantment.getFullname(clue.enchantment, clue.level);
                            tooltip.add(enchantName);
                        }

                    } else {
                        // 盲盒状态：线索为 0
                        tooltip.add(Component.empty().append(Component.translatable("container.enchant.clue", "")).withStyle(net.minecraft.ChatFormatting.WHITE));
                    }

                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
        }

        if (mouseX >= x + 4 && mouseX <= x + 10 && mouseY >= y + 10 && mouseY <= y + 80) {
            java.util.List<Component> tooltips = java.util.List.of(
                    Component.translatable("gui.apothicenchantingaddition.energy.fe", String.format("%,d", menu.getEnergy()), String.format("%,d", MAX_ENERGY)),
                    Component.translatable("gui.apothicenchantingaddition.energy.cost", String.format("%,d", ApothicAdditionConfig.FLUX_ENCHANTER_TICK_COST.get()))
            );
            guiGraphics.renderComponentTooltip(this.font, tooltips, mouseX, mouseY);
        }
    }
}
