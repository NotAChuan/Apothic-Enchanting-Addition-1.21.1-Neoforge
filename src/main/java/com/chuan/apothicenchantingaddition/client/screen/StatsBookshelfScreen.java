package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.block.Tier;
import com.chuan.apothicenchantingaddition.menu.StatsBookshelfMenu;
import com.chuan.apothicenchantingaddition.network.UpdateStatsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class StatsBookshelfScreen extends AbstractContainerScreen<StatsBookshelfMenu> {

    private StatsSlider eternaSlider;
    private StatsSlider quantaSlider;
    private StatsSlider arcanaSlider;
    private StatsSlider cluesSlider;
    private Checkbox treasureCheckbox;
    private Checkbox stableCheckbox;

    private final Tier tier;
    private final int MAX_ENERGY = 1000000;

    public StatsBookshelfScreen(StatsBookshelfMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 142;
        this.inventoryLabelY = 1000;

        if (menu.getBlockEntity() != null) {
            this.tier = menu.getBlockEntity().getTier();
        } else {
            this.tier = Tier.TIER_1;
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        float currentEterna = menu.getEterna();
        float currentQuanta = menu.getQuanta();
        float currentArcana = menu.getArcana();
        int currentClues = menu.getClues();
        boolean currentTreasure = menu.allowsTreasure();
        boolean currentStable = menu.isStable();

        int sliderWidth = 104;
        int startX = x + 30;

        // 原生的滑动条初始化，没有多余的干扰
        this.eternaSlider = addRenderableWidget(new StatsSlider(startX, y + 18, sliderWidth, 20,
                Component.translatable("gui.apothicenchantingaddition.stat.eterna", ""), currentEterna / tier.getMaxEterna()));

        this.quantaSlider = addRenderableWidget(new StatsSlider(startX, y + 42, sliderWidth, 20,
                Component.translatable("gui.apothicenchantingaddition.stat.quanta", ""), currentQuanta / tier.getMaxQuanta()));

        this.arcanaSlider = addRenderableWidget(new StatsSlider(startX, y + 66, sliderWidth, 20,
                Component.translatable("gui.apothicenchantingaddition.stat.arcana", ""), currentArcana / tier.getMaxArcana()));

        this.cluesSlider = addRenderableWidget(new StatsSlider(startX, y + 90, sliderWidth, 20,
                Component.translatable("gui.apothicenchantingaddition.stat.clues", ""), (double) currentClues / tier.getMaxClues()));

        this.treasureCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("gui.apothicenchantingaddition.stat.treasure", ""), this.font)
                .pos(startX, y + 115)
                .selected(currentTreasure)
                .build());

        this.stableCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("gui.apothicenchantingaddition.stat.rectification", ""), this.font)
                .pos(startX + 65, y + 115)
                .selected(currentStable)
                .build());

        // 独立的保存按钮，只有点击时才发送数据给服务器
        addRenderableWidget(Button.builder(Component.translatable("gui.apothicenchantingaddition.save", "保存"), button -> {
            sendUpdatePacket();
        }).pos(startX + 128, y + 115).size(28, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 遍历所有控件，如果点中了滑动条等控件，立刻设为焦点并标记正在拖拽
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(listener);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true; // 拦截事件，不再向下传递
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 如果当前有获得焦点的滑动条，且正在拖拽
        if (this.getFocused() != null && this.isDragging() && button == 0) {
            // 直接把拖拽坐标喂给滑动条，并强行返回 true
            this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true; // 【核心拦截】绝对不让原版 ContainerScreen 偷走这个拖拽事件！
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDragging() && button == 0) {
            this.setDragging(false);
            if (this.getFocused() != null) {
                this.getFocused().mouseReleased(mouseX, mouseY, button);
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void sendUpdatePacket() {
        if (menu.getBlockEntity() != null && eternaSlider != null) {
            float e = (float) (eternaSlider.getValue() * tier.getMaxEterna());
            float q = (float) (quantaSlider.getValue() * tier.getMaxQuanta());
            float a = (float) (arcanaSlider.getValue() * tier.getMaxArcana());
            int c = (int) Math.round(cluesSlider.getValue() * tier.getMaxClues());
            boolean t = treasureCheckbox.selected();
            boolean s = stableCheckbox.selected();

            PacketDistributor.sendToServer(new UpdateStatsPayload(
                    menu.getBlockEntity().getBlockPos(), e, q, a, c, t, s
            ));
        }
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

        int barX = x + 8;
        int barY = y + 18;
        int barWidth = 14;
        int barHeight = 117;
        drawSlotBackground(guiGraphics, barX, barY, barWidth, barHeight);

        int currentEnergy = menu.getEnergy();
        if (currentEnergy > 0) {
            int fillHeight = (int) ((currentEnergy / (float) MAX_ENERGY) * (barHeight - 2));
            guiGraphics.fill(barX + 1, barY + barHeight - 1 - fillHeight, barX + barWidth - 1, barY + barHeight - 1, 0xFFD80000);
            guiGraphics.fill(barX + 2, barY + barHeight - 1 - fillHeight, barX + 3, barY + barHeight - 1, 0xFFFF6666);
        }

        int startX = x + 138;
        drawSlotBackground(guiGraphics, startX, y + 19, 44, 18);
        drawSlotBackground(guiGraphics, startX, y + 43, 44, 18);
        drawSlotBackground(guiGraphics, startX, y + 67, 44, 18);
        drawSlotBackground(guiGraphics, startX, y + 91, 44, 18);
    }

    private void drawSlotBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF373737);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF373737);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (eternaSlider != null) {
            int startX = x + 160;
            String eternaTxt = String.format("%.1f", eternaSlider.getValue() * tier.getMaxEterna());
            guiGraphics.drawCenteredString(this.font, eternaTxt, startX, y + 24, 0xFFFFFF);

            String quantaTxt = String.format("%.1f%%", quantaSlider.getValue() * tier.getMaxQuanta());
            guiGraphics.drawCenteredString(this.font, quantaTxt, startX, y + 48, 0xFFFFFF);

            String arcanaTxt = String.format("%.1f%%", arcanaSlider.getValue() * tier.getMaxArcana());
            guiGraphics.drawCenteredString(this.font, arcanaTxt, startX, y + 72, 0xFFFFFF);

            String cluesTxt = String.valueOf((int) Math.round(cluesSlider.getValue() * tier.getMaxClues()));
            guiGraphics.drawCenteredString(this.font, cluesTxt, startX, y + 96, 0xFFFFFF);
        }

        int barX = x + 8;
        int barY = y + 18;
        if (mouseX >= barX && mouseX <= barX + 14 && mouseY >= barY && mouseY <= barY + 117) {
            // 获取对应层级的耗电量
            int cost = switch (tier) {
                case TIER_1 ->
                        com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig.TIER_1_ENERGY_COST.get();
                case TIER_2 ->
                        com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig.TIER_2_ENERGY_COST.get();
                case TIER_3 ->
                        com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig.TIER_3_ENERGY_COST.get();
                case TIER_4 ->
                        com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig.TIER_4_ENERGY_COST.get();
            };

            java.util.List<Component> tooltip = java.util.List.of(
                    Component.translatable("gui.apothicenchantingaddition.energy.fe", menu.getEnergy(), MAX_ENERGY),
                    Component.translatable("gui.apothicenchantingaddition.energy.cost", cost)
            );
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // 最纯净的原版滑动条，不干涉任何发包或拖拽事件拦截
    private class StatsSlider extends AbstractSliderButton {
        public StatsSlider(int x, int y, int width, int height, Component message, double value) {
            super(x, y, width, height, message, value);
        }

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
        }

        public double getValue() {
            return this.value;
        }
    }
}
