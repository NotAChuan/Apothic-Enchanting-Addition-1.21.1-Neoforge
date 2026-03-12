package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.block.Tier;
import com.chuan.apothicenchantingaddition.menu.FluxStatsBookshelfMenu;
import com.chuan.apothicenchantingaddition.network.UpdateStatsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class FluxStatsBookshelfScreen extends AbstractContainerScreen<FluxStatsBookshelfMenu> {

    // GUI 贴图资源位置
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apothicenchantingaddition",
            "textures/gui/flux_stats_bookshelf_tier_gui.png"
    );

    // 【优化1】：能量条常量
    private static final int ENERGY_BAR_X = 7;
    private static final int ENERGY_BAR_Y = 12;
    private static final int ENERGY_BAR_WIDTH = 7;
    private static final int ENERGY_BAR_HEIGHT = 113;
    private static final int ENERGY_BAR_TEXTURE_X = 187;
    private static final int ENERGY_BAR_TEXTURE_Y = 12;

    // 【优化2】：勾选框常量
    private static final int CHECKBOX_SIZE = 21;
    private static final int CHECKBOX_1_X = 16;
    private static final int CHECKBOX_2_X = 79;
    private static final int CHECKBOX_Y = 104;
    private static final int CHECKBOX_SELECTED_TEXTURE_X = 195;
    private static final int CHECKBOX_SELECTED_TEXTURE_Y = 22;

    // 【优化3】：滑块常量
    private static final int SLIDER_START_X = 16;
    private static final int SLIDER_WIDTH = 121;
    private static final int SLIDER_HEIGHT = 21;
    private static final int SLIDER_KNOB_WIDTH = 9;
    private static final int SLIDER_KNOB_HEIGHT = 21;
    private static final int SLIDER_KNOB_TEXTURE_X_NORMAL = 195;
    private static final int SLIDER_KNOB_TEXTURE_X_PRESSED = 205;
    private static final int SLIDER_KNOB_TEXTURE_Y = 0;
    private static final int[] SLIDER_Y_POSITIONS = {12, 35, 58, 81};

    // 【优化4】：数字框常量
    private static final int NUMBER_BOX_X = 160;
    private static final int[] NUMBER_BOX_Y_POSITIONS = {18, 41, 64, 87};

    private StatsSlider eternaSlider;
    private StatsSlider quantaSlider;
    private StatsSlider arcanaSlider;
    private StatsSlider cluesSlider;

    private boolean treasureEnabled;
    private boolean stableEnabled;

    private final Tier tier;
    private final int MAX_ENERGY = 1000000;

    // 【优化5】：缓存 GUI 位置
    private int guiLeft;
    private int guiTop;

    public FluxStatsBookshelfScreen(FluxStatsBookshelfMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 186;
        this.imageHeight = 137;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;

        if (menu.getBlockEntity() != null) {
            this.tier = menu.getBlockEntity().getTier();
        } else {
            this.tier = Tier.TIER_1;
        }
    }

    @Override
    protected void init() {
        super.init();
        // 【优化6】：缓存 GUI 位置，避免重复计算
        this.guiLeft = (width - imageWidth) / 2;
        this.guiTop = (height - imageHeight) / 2;

        int currentEterna = menu.getEterna();
        int currentQuanta = menu.getQuanta();
        int currentArcana = menu.getArcana();
        int currentClues = menu.getClues();
        boolean currentTreasure = menu.allowsTreasure();
        boolean currentStable = menu.isStable();

        int startX = guiLeft + SLIDER_START_X;

        // 创建 4 个滑块
        this.eternaSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[0], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothicenchantingaddition.stat.eterna", ""),
                (double) currentEterna / tier.getMaxEterna(),
                Math.round(tier.getMaxEterna())
        ));

        this.quantaSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[1], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothicenchantingaddition.stat.quanta", ""),
                (double) currentQuanta / tier.getMaxQuanta(),
                Math.round(tier.getMaxQuanta())
        ));

        this.arcanaSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[2], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothicenchantingaddition.stat.arcana", ""),
                (double) currentArcana / tier.getMaxArcana(),
                Math.round(tier.getMaxArcana())
        ));

        this.cluesSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[3], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothicenchantingaddition.stat.clues", ""),
                (double) currentClues / tier.getMaxClues(),
                tier.getMaxClues()
        ));

        // 初始化勾选框状态
        this.treasureEnabled = currentTreasure;
        this.stableEnabled = currentStable;

        // 保存按钮
        addRenderableWidget(Button.builder(
                Component.translatable("gui.apothicenchantingaddition.save", "保存"),
                button -> sendUpdatePacket()
        ).pos(guiLeft + 144, guiTop + CHECKBOX_Y).size(33, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 【优化7】：勾选框点击检测
        if (button == 0) {
            if (isMouseOverCheckbox(mouseX, mouseY, CHECKBOX_1_X)) {
                treasureEnabled = !treasureEnabled;
                return true;
            }
            if (isMouseOverCheckbox(mouseX, mouseY, CHECKBOX_2_X)) {
                stableEnabled = !stableEnabled;
                return true;
            }
        }

        // 滑块处理逻辑
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(listener);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // 【优化8】：提取勾选框检测为独立方法
    private boolean isMouseOverCheckbox(double mouseX, double mouseY, int checkboxX) {
        return mouseX >= guiLeft + checkboxX &&
                mouseX <= guiLeft + checkboxX + CHECKBOX_SIZE &&
                mouseY >= guiTop + CHECKBOX_Y &&
                mouseY <= guiTop + CHECKBOX_Y + CHECKBOX_SIZE;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging() && button == 0) {
            if (this.getFocused() != null) {
                this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
                return true;
            }
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
            int e = (int) Math.round(eternaSlider.getValue() * tier.getMaxEterna());
            int q = (int) Math.round(quantaSlider.getValue() * tier.getMaxQuanta());
            int a = (int) Math.round(arcanaSlider.getValue() * tier.getMaxArcana());
            int c = (int) Math.round(cluesSlider.getValue() * tier.getMaxClues());
            boolean t = treasureEnabled;
            boolean s = stableEnabled;

            PacketDistributor.sendToServer(new UpdateStatsPayload(
                    menu.getBlockEntity().getBlockPos(), e, q, a, c, t, s
            ));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 绘制 GUI 主背景
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, 186, 137, 256, 256);

        // 【优化9】：使用常量绘制能量条
        int currentEnergy = menu.getEnergy();
        if (currentEnergy > 0) {
            float energyPercent = (float) currentEnergy / MAX_ENERGY;

            int barX = guiLeft + ENERGY_BAR_X;
            int barY = guiTop + ENERGY_BAR_Y;
            int filledHeight = (int) (ENERGY_BAR_HEIGHT * energyPercent);

            int startY = barY + (ENERGY_BAR_HEIGHT - filledHeight);
            int textureStartY = ENERGY_BAR_TEXTURE_Y + (ENERGY_BAR_HEIGHT - filledHeight);

            guiGraphics.blit(
                    TEXTURE,
                    barX, startY,
                    ENERGY_BAR_TEXTURE_X, textureStartY,
                    ENERGY_BAR_WIDTH, filledHeight,
                    256, 256
            );
        }

        // 【优化10】：使用常量绘制勾选框选中状态
        if (treasureEnabled) {
            guiGraphics.blit(
                    TEXTURE,
                    guiLeft + CHECKBOX_1_X, guiTop + CHECKBOX_Y,
                    CHECKBOX_SELECTED_TEXTURE_X, CHECKBOX_SELECTED_TEXTURE_Y,
                    CHECKBOX_SIZE, CHECKBOX_SIZE,
                    256, 256
            );
        }

        if (stableEnabled) {
            guiGraphics.blit(
                    TEXTURE,
                    guiLeft + CHECKBOX_2_X, guiTop + CHECKBOX_Y,
                    CHECKBOX_SELECTED_TEXTURE_X, CHECKBOX_SELECTED_TEXTURE_Y,
                    CHECKBOX_SIZE, CHECKBOX_SIZE,
                    256, 256
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 【优化11】：使用常量绘制数字
        if (eternaSlider != null) {
            String eternaTxt = String.valueOf((int) Math.round(eternaSlider.getValue() * tier.getMaxEterna()));
            guiGraphics.drawString(this.font, eternaTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(eternaTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[0], 0x404040, false);

            String quantaTxt = String.valueOf((int) Math.round(quantaSlider.getValue() * tier.getMaxQuanta()));
            guiGraphics.drawString(this.font, quantaTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(quantaTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[1], 0x404040, false);

            String arcanaTxt = String.valueOf((int) Math.round(arcanaSlider.getValue() * tier.getMaxArcana()));
            guiGraphics.drawString(this.font, arcanaTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(arcanaTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[2], 0x404040, false);

            String cluesTxt = String.valueOf((int) Math.round(cluesSlider.getValue() * tier.getMaxClues()));
            guiGraphics.drawString(this.font, cluesTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(cluesTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[3], 0x404040, false);
        }

        // 【新增】：绘制滑块文字标签（在滑块左侧）
//        int sliderLabelX = guiLeft + 8;  // 滑块左侧，距离 GUI 左边缘 8 像素

        // 第一条滑块（位阶）
        Component eternaLabel = Component.translatable("gui.apothicenchantingaddition.stat.eterna", "");
        int eternaLabelX = guiLeft + SLIDER_START_X + (SLIDER_WIDTH - this.font.width(eternaLabel)) / 2;
        int eternaLabelY = guiTop + SLIDER_Y_POSITIONS[0] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.drawString(this.font, eternaLabel, eternaLabelX, eternaLabelY, 0xFFFFFF, false);

        // 第二条滑块（量子化）
        Component quantaLabel = Component.translatable("gui.apothicenchantingaddition.stat.quanta", "");
        int quantaLabelX = guiLeft + SLIDER_START_X + (SLIDER_WIDTH - this.font.width(quantaLabel)) / 2;
        int quantaLabelY = guiTop + SLIDER_Y_POSITIONS[1] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.drawString(this.font, quantaLabel, quantaLabelX, quantaLabelY, 0xFFFFFF, false);

        // 第三条滑块（阿卡那）
        Component arcanaLabel = Component.translatable("gui.apothicenchantingaddition.stat.arcana", "");
        int arcanaLabelX = guiLeft + SLIDER_START_X + (SLIDER_WIDTH - this.font.width(arcanaLabel)) / 2;
        int arcanaLabelY = guiTop + SLIDER_Y_POSITIONS[2] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.drawString(this.font, arcanaLabel, arcanaLabelX, arcanaLabelY, 0xFFFFFF, false);

        // 第四条滑块（魔咒线索）
        Component cluesLabel = Component.translatable("gui.apothicenchantingaddition.stat.clues", "");
        int cluesLabelX = guiLeft + SLIDER_START_X + (SLIDER_WIDTH - this.font.width(cluesLabel)) / 2;
        int cluesLabelY = guiTop + SLIDER_Y_POSITIONS[3] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.drawString(this.font, cluesLabel, cluesLabelX, cluesLabelY, 0xFFFFFF, false);


        // 绘制勾选框文字标签
        int checkboxTextY = guiTop + CHECKBOX_Y + (CHECKBOX_SIZE - this.font.lineHeight) / 2;

        Component treasureText = Component.translatable("gui.apothicenchantingaddition.stat.treasure", "");
        guiGraphics.drawString(this.font, treasureText,
                guiLeft + CHECKBOX_1_X + CHECKBOX_SIZE + 3, checkboxTextY, 0x404040, false);

        Component stableText = Component.translatable("gui.apothicenchantingaddition.stat.rectification", "");
        guiGraphics.drawString(this.font, stableText,
                guiLeft + CHECKBOX_2_X + CHECKBOX_SIZE + 3, checkboxTextY, 0x404040, false);

        // 能量条悬停提示
        int barX = guiLeft + ENERGY_BAR_X;
        int barY = guiTop + ENERGY_BAR_Y;
        if (mouseX >= barX && mouseX <= barX + ENERGY_BAR_WIDTH &&
                mouseY >= barY && mouseY <= barY + ENERGY_BAR_HEIGHT) {

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

    // 【修改1】：自定义滑块类，使用贴图渲染
    private class StatsSlider extends AbstractSliderButton {
        private final int maxValue;

        public StatsSlider(int x, int y, int width, int height, Component message, double value, int maxValue) {
            super(x, y, width, height, message, value);
            this.maxValue = Math.max(1, maxValue);
            this.applyValue();
        }

        @Override
        protected void updateMessage() {
            // 不需要更新消息文本
        }

        @Override
        protected void applyValue() {
            this.value = Math.round(this.value * this.maxValue) / (double) this.maxValue;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 【修改2】：不绘制原生滑块背景，只绘制自定义滑块按钮

            // 计算滑块按钮位置
            int knobX = this.getX() + (int) ((this.width - SLIDER_KNOB_WIDTH) * this.value);
            int knobY = this.getY();

            // 【修改3】：根据是否按下选择贴图（使用 isHovered 和鼠标按下状态判断）
            boolean isPressed = this.isHovered() && FluxStatsBookshelfScreen.this.isDragging();
            int textureX = isPressed ? SLIDER_KNOB_TEXTURE_X_PRESSED : SLIDER_KNOB_TEXTURE_X_NORMAL;

            // 绘制滑块按钮
            guiGraphics.blit(
                    TEXTURE,
                    knobX, knobY,
                    textureX, SLIDER_KNOB_TEXTURE_Y,
                    SLIDER_KNOB_WIDTH, SLIDER_KNOB_HEIGHT,
                    256, 256
            );
        }

        public double getValue() {
            return this.value;
        }
    }
}
