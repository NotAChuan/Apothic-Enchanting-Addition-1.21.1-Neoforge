package com.chuan.apothicflux.client.screen;

import com.chuan.apothicflux.block.Tier;
import com.chuan.apothicflux.menu.FluxStatsBookshelfMenu;
import com.chuan.apothicflux.network.UpdateStatsPayload;
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
            "apothic_flux",
            "textures/gui/flux_stats_bookshelf_tier_gui.png"
    );

    // 【优化1】：能量条常量
    private static final int ENERGY_BAR_X = 7;
    private static final int ENERGY_BAR_Y = 12;
    private static final int ENERGY_BAR_WIDTH = 7;
    private static final int ENERGY_BAR_HEIGHT = 113;
    private static final int ENERGY_BAR_TEXTURE_X = 195;
    private static final int ENERGY_BAR_TEXTURE_Y = 12;

    // 【优化2】：勾选框常量
    private static final int CHECKBOX_SIZE = 17;
    private static final int CHECKBOX_1_X = 19;
    private static final int CHECKBOX_2_X = 75;
    private static final int CHECKBOX_Y = 105;
    private static final int CHECKBOX_SELECTED_TEXTURE_X = 208;
    private static final int CHECKBOX_SELECTED_TEXTURE_Y = 0;

    private static final int CHECK_FONT_1_X = 39;
    private static final int CHECK_FONT_2_X = 95;
    private static final float CHECK_FONT_SCALE = 0.75F;

    // 【优化3】：滑块常量
    private static final int SLIDER_START_X = 50;
    private static final int SLIDER_WIDTH = 110;
    private static final int SLIDER_HEIGHT = 5;
    private static final int SLIDER_KNOB_WIDTH = 4;
    private static final int SLIDER_KNOB_HEIGHT = 7;
    private static final int SLIDER_KNOB_TEXTURE_X_NORMAL = 122;
    private static final int SLIDER_KNOB_TEXTURE_Y_ETERNA = 136;
    private static final int SLIDER_KNOB_TEXTURE_Y_QUANTA = 143;
    private static final int SLIDER_KNOB_TEXTURE_Y_ARCANA = 150;
    private static final int SLIDER_KNOB_TEXTURE_Y_CLUES = 157;

    // 四条滑块的填充纹理：X=0~110，高度=5
    private static final int SLIDER_PROGRESS_TEXTURE_X = 0;
    private static final int SLIDER_PROGRESS_TEXTURE_Y_ETERNA = 136;
    private static final int SLIDER_PROGRESS_TEXTURE_Y_QUANTA = 141;
    private static final int SLIDER_PROGRESS_TEXTURE_Y_ARCANA = 146;
    private static final int SLIDER_PROGRESS_TEXTURE_Y_CLUES = 151;

    // 位阶视觉标尺固定为 0~100；真实可用上限仍由 Tier 决定。
    private static final double ETERNA_VISUAL_MAX = 100.0;

    // 位阶超出当前 Tier 上限后的白色锁定区域纹理。
    // 纹理范围：X=0~110，Y=156~161（宽 110，高 5）。
    private static final int ETERNA_LOCKED_TEXTURE_X = 0;
    private static final int ETERNA_LOCKED_TEXTURE_Y = 156;

    private static final int[] SLIDER_Y_POSITIONS = {19, 41, 63, 85};

    // 【优化4】：数字框常量
    private static final int NUMBER_BOX_X = 174;
    private static final int[] NUMBER_BOX_Y_POSITIONS = {19, 41, 63, 85};

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
        this.imageWidth = 193;
        this.imageHeight = 136;
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
        double eternaMin = 0.0;
        double eternaMax = Math.round(tier.getMaxEterna());
        this.eternaSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[0], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothic_flux.stat.eterna", ""),
                toSliderValue(currentEterna, eternaMin, ETERNA_VISUAL_MAX),
                eternaMin, eternaMax, ETERNA_VISUAL_MAX,
                SLIDER_KNOB_TEXTURE_Y_ETERNA
        ));

        double quantaMin = Tier.MIN_QUANTA;
        double quantaMax = Math.round(tier.getMaxQuanta());
        this.quantaSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[1], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothic_flux.stat.quanta", ""),
                toSliderValue(currentQuanta, quantaMin, quantaMax),
                quantaMin, quantaMax, quantaMax,
                SLIDER_KNOB_TEXTURE_Y_QUANTA
        ));

        double arcanaMin = 0.0;
        double arcanaMax = Math.round(tier.getMaxArcana());
        this.arcanaSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[2], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothic_flux.stat.arcana", ""),
                toSliderValue(currentArcana, arcanaMin, arcanaMax),
                arcanaMin, arcanaMax, arcanaMax,
                SLIDER_KNOB_TEXTURE_Y_ARCANA
        ));

        double cluesMin = 0.0;
        double cluesMax = tier.getMaxClues();
        this.cluesSlider = addRenderableWidget(new StatsSlider(
                startX, guiTop + SLIDER_Y_POSITIONS[3], SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.translatable("gui.apothic_flux.stat.clues", ""),
                toSliderValue(currentClues, cluesMin, cluesMax),
                cluesMin, cluesMax, cluesMax,
                SLIDER_KNOB_TEXTURE_Y_CLUES
        ));

        // 初始化勾选框状态
        this.treasureEnabled = currentTreasure;
        this.stableEnabled = currentStable;

        // 保存按钮
        addRenderableWidget(Button.builder(
                Component.translatable("gui.apothic_flux.save"),
                button -> sendUpdatePacket()
        ).pos(guiLeft + 144, guiTop + CHECKBOX_Y).size(33, 20).build(builder -> new ScaledButton(builder, 0.75F)));
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

        // 让 Minecraft 自己处理滑块、按钮、焦点和 dragging
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        // 保存按钮点击后清除焦点，避免白框一直存在
        if (handled && this.getFocused() instanceof Button) {
            this.setFocused(null);
            this.setDragging(false);
        }

        return handled;
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
            int e = (int) Math.round(eternaSlider.getStatValue());
            int q = (int) Math.round(quantaSlider.getStatValue());
            int a = (int) Math.round(arcanaSlider.getStatValue());
            int c = (int) Math.round(cluesSlider.getStatValue());
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
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, 193, 136, 256, 256);

        // 根据四个滑块当前 value 实时绘制对应颜色的进度填充。
        // renderBg 会先于控件本身绘制，所以滑块按钮会自然盖在进度条上方。
        renderSliderProgress(guiGraphics, eternaSlider, 0, SLIDER_PROGRESS_TEXTURE_Y_ETERNA);
        renderEternaLockedArea(guiGraphics);
        renderSliderProgress(guiGraphics, quantaSlider, 1, SLIDER_PROGRESS_TEXTURE_Y_QUANTA);
        renderSliderProgress(guiGraphics, arcanaSlider, 2, SLIDER_PROGRESS_TEXTURE_Y_ARCANA);
        renderSliderProgress(guiGraphics, cluesSlider, 3, SLIDER_PROGRESS_TEXTURE_Y_CLUES);

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
            String eternaTxt = String.valueOf((int) Math.round(eternaSlider.getStatValue()));
            guiGraphics.drawString(this.font, eternaTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(eternaTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[0], 0x404040, false);

            String quantaTxt = String.valueOf((int) Math.round(quantaSlider.getStatValue()));
            guiGraphics.drawString(this.font, quantaTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(quantaTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[1], 0x404040, false);

            String arcanaTxt = String.valueOf((int) Math.round(arcanaSlider.getStatValue()));
            guiGraphics.drawString(this.font, arcanaTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(arcanaTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[2], 0x404040, false);

            String cluesTxt = String.valueOf((int) Math.round(cluesSlider.getStatValue()));
            guiGraphics.drawString(this.font, cluesTxt,
                    guiLeft + NUMBER_BOX_X - this.font.width(cluesTxt) / 2,
                    guiTop + NUMBER_BOX_Y_POSITIONS[3], 0x404040, false);
        }

        // 【新增】：绘制滑块文字标签（在滑块左侧）
//        int sliderLabelX = guiLeft + 8;  // 滑块左侧，距离 GUI 左边缘 8 像素

        // 第一条滑块（位阶）
        Component eternaLabel = Component.translatable("gui.apothic_flux.label.eterna");
        int eternaLabelX = guiLeft + 16;
        int eternaLabelY = guiTop + SLIDER_Y_POSITIONS[0] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        drawScaledString(guiGraphics, eternaLabel, eternaLabelX, eternaLabelY + 3, CHECK_FONT_SCALE, 0x42c542);

        // 第二条滑块（量子化）
        Component quantaLabel = Component.translatable("gui.apothic_flux.label.quanta");
        int quantaLabelX = guiLeft + 16;
        int quantaLabelY = guiTop + SLIDER_Y_POSITIONS[1] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        drawScaledString(guiGraphics, quantaLabel, quantaLabelX, quantaLabelY + 3, CHECK_FONT_SCALE,0xdf4a4a);

        // 第三条滑块（阿卡那）
        Component arcanaLabel = Component.translatable("gui.apothic_flux.label.arcana");
        int arcanaLabelX = guiLeft + 16;
        int arcanaLabelY = guiTop + SLIDER_Y_POSITIONS[2] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        drawScaledString(guiGraphics, arcanaLabel, arcanaLabelX, arcanaLabelY + 3, CHECK_FONT_SCALE,0x950095);

        // 第四条滑块（魔咒线索）
        Component cluesLabel = Component.translatable("gui.apothic_flux.label.clues");
        int cluesLabelX = guiLeft + 16;
        int cluesLabelY = guiTop + SLIDER_Y_POSITIONS[3] + (SLIDER_HEIGHT - this.font.lineHeight) / 2;
        drawScaledString(guiGraphics, cluesLabel, cluesLabelX, cluesLabelY + 3, CHECK_FONT_SCALE,0xe9ec00);


        // 绘制勾选框文字标签
        int checkboxTextY = guiTop + CHECKBOX_Y
                + Math.round((CHECKBOX_SIZE - this.font.lineHeight * CHECK_FONT_SCALE) / 2.0F);

        Component treasureText = Component.translatable("gui.apothic_flux.stat.treasure", "");
        float treasureWidth = this.font.width(treasureText) * CHECK_FONT_SCALE;

        int treasureX = Math.round(
                guiLeft + CHECK_FONT_1_X + (32 - treasureWidth) / 2.0F
        );
        drawScaledString(guiGraphics, treasureText,
                treasureX, checkboxTextY, CHECK_FONT_SCALE, 0xFFAA00);

        Component stableText = Component.translatable("gui.apothic_flux.stat.provides_stability", "");
        float stableWidth = this.font.width(stableText) * CHECK_FONT_SCALE;

        int stableX = Math.round(
                guiLeft + CHECK_FONT_2_X + (32 - stableWidth) / 2.0F
        );
        drawScaledString(guiGraphics, stableText,
                stableX, checkboxTextY, CHECK_FONT_SCALE, 0xFFAA00);

        // 能量条悬停提示
        int barX = guiLeft + ENERGY_BAR_X;
        int barY = guiTop + ENERGY_BAR_Y;
        if (mouseX >= barX && mouseX <= barX + ENERGY_BAR_WIDTH &&
                mouseY >= barY && mouseY <= barY + ENERGY_BAR_HEIGHT) {

            int cost = switch (tier) {
                case TIER_1 ->
                        com.chuan.apothicflux.config.ApothicAdditionConfig.TIER_1_ENERGY_COST.get();
                case TIER_2 ->
                        com.chuan.apothicflux.config.ApothicAdditionConfig.TIER_2_ENERGY_COST.get();
                case TIER_3 ->
                        com.chuan.apothicflux.config.ApothicAdditionConfig.TIER_3_ENERGY_COST.get();
                case TIER_4 ->
                        com.chuan.apothicflux.config.ApothicAdditionConfig.TIER_4_ENERGY_COST.get();
            };

            java.util.List<Component> tooltip = java.util.List.of(
                    Component.translatable("gui.apothic_flux.energy.fe", menu.getEnergy(), MAX_ENERGY),
                    Component.translatable("gui.apothic_flux.energy.cost", cost)
            );
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 绘制单条滑块的实时填充。
     * value 范围为 0~1，映射到 0~SLIDER_WIDTH 像素。
     */
    private void renderSliderProgress(
            GuiGraphics guiGraphics,
            StatsSlider slider,
            int sliderIndex,
            int textureY
    ) {
        if (slider == null || sliderIndex < 0 || sliderIndex >= SLIDER_Y_POSITIONS.length) {
            return;
        }

        double progress = Math.max(0.0, Math.min(1.0, slider.getValue()));
        int fillWidth = (int) Math.round(SLIDER_WIDTH * progress);

        if (fillWidth <= 0) {
            return;
        }

        int drawX = guiLeft + SLIDER_START_X;
        int drawY = guiTop + SLIDER_Y_POSITIONS[sliderIndex] + 1;

        guiGraphics.blit(
                TEXTURE,
                drawX, drawY,
                SLIDER_PROGRESS_TEXTURE_X, textureY,
                fillWidth, SLIDER_HEIGHT,
                256, 256
        );
    }

    /**
     * 绘制位阶滑块中当前 Tier 无法使用的白色锁定区域。
     * 例如 TIER_1 上限为 25，则从整条轨道的 25% 位置覆盖到 100%。
     */
    private void renderEternaLockedArea(GuiGraphics guiGraphics) {
        double maxProgress = Math.max(
                0.0,
                Math.min(1.0, tier.getMaxEterna() / ETERNA_VISUAL_MAX)
        );

        int unlockedWidth = (int) Math.round(SLIDER_WIDTH * maxProgress);
        int lockedWidth = SLIDER_WIDTH - unlockedWidth;

        if (lockedWidth <= 0) {
            return;
        }

        int drawX = guiLeft + SLIDER_START_X + unlockedWidth;
        int drawY = guiTop + SLIDER_Y_POSITIONS[0] + 1;

        // 从白色整条纹理的对应 X 位置开始裁切，避免左右端纹理错位。
        int textureX = ETERNA_LOCKED_TEXTURE_X + unlockedWidth;

        guiGraphics.blit(
                TEXTURE,
                drawX, drawY,
                textureX, ETERNA_LOCKED_TEXTURE_Y,
                lockedWidth, SLIDER_HEIGHT,
                256, 256
        );
    }

    /**
     * 按指定比例绘制文字。先平移到目标坐标，再缩放，避免手动反算 x/y 坐标。
     */
    private void drawScaledString(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            float scale,
            int color
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    // 【修改1】：自定义滑块类，使用贴图渲染
    private class StatsSlider extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;
        private final double visualMaxValue;

        private final int textureY;

        public StatsSlider(
                int x,
                int y,
                int width,
                int height,
                Component message,
                double value,
                double minValue,
                double maxValue,
                double visualMaxValue,
                int textureY
        ) {
            super(x, y, width, height, message, value);
            this.minValue = minValue;
            this.maxValue = Math.max(minValue + 1.0, maxValue);
            this.visualMaxValue = Math.max(this.maxValue, visualMaxValue);

            this.textureY = textureY;

            this.applyValue();
        }

        @Override
        protected void updateMessage() {
            // 不需要更新消息文本
        }

        @Override
        protected void applyValue() {
            // 构造期间 super() 可能会先调用一次 applyValue()，此时字段尚未赋值，直接跳过。
            if (this.maxValue <= this.minValue || this.visualMaxValue <= this.minValue) {
                return;
            }

            // 鼠标位置先映射到“视觉标尺”。
            // Eterna 的视觉标尺固定是 0~100；其他滑块的视觉标尺与原真实范围相同。
            double stat = this.minValue
                    + this.value * (this.visualMaxValue - this.minValue);

            stat = Math.round(stat);

            // 再夹取到当前 Tier 真正允许的最大值。
            // 因此点击/拖入 Eterna 的白色区域时，会自动停在 Tier 上限。
            stat = Math.max(this.minValue, Math.min(this.maxValue, stat));

            // 用视觉标尺反算 value，保证 Eterna=25 显示在整条轨道的 25%。
            this.value = (stat - this.minValue)
                    / (this.visualMaxValue - this.minValue);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 【修改2】：不绘制原生滑块背景，只绘制自定义滑块按钮

            // 计算滑块按钮位置
            int knobX = this.getX() + (int) ((this.width - SLIDER_KNOB_WIDTH) * this.value);
            int knobY = this.getY();

//            // 【修改3】：根据是否按下选择贴图（使用 isHovered 和鼠标按下状态判断）
//            boolean isPressed = this.isHovered() && FluxStatsBookshelfScreen.this.isDragging();
//            int textureX = isPressed ? SLIDER_KNOB_TEXTURE_X_PRESSED : SLIDER_KNOB_TEXTURE_X_NORMAL;

            // 绘制滑块按钮
            guiGraphics.blit(
                    TEXTURE,
                    knobX, knobY,
                    SLIDER_KNOB_TEXTURE_X_NORMAL, this.textureY,
                    SLIDER_KNOB_WIDTH, SLIDER_KNOB_HEIGHT,
                    256, 256
            );
        }

        public double getValue() {
            return this.value;
        }

        public double getStatValue() {
            double stat = this.minValue
                    + this.value * (this.visualMaxValue - this.minValue);

            return Math.max(this.minValue, Math.min(this.maxValue, stat));
        }
    }

    // 把真实数值换算成滑块的归一化值 [0,1]，并夹取到合法范围
    private static double toSliderValue(double stat, double min, double max) {
        return Math.max(0.0, Math.min(1.0, (stat - min) / (max - min)));
    }

    private class ScaledButton extends Button {

        private final float scale;

        protected ScaledButton(Button.Builder builder, float scale) {
            super(builder);
            this.scale = scale;
        }

        @Override
        protected void renderWidget(
                GuiGraphics guiGraphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            int x = this.getX();
            int y = this.getY();

            guiGraphics.pose().pushPose();

            // 以按钮左上角为缩放原点
            guiGraphics.pose().translate(x, y, 0.0F);
            guiGraphics.pose().scale(this.scale, this.scale, 1.0F);
            guiGraphics.pose().translate(-x, -y, 0.0F);

            super.renderWidget(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    partialTick
            );

            guiGraphics.pose().popPose();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            int visualWidth = Math.round(this.width * this.scale);
            int visualHeight = Math.round(this.height * this.scale);

            return mouseX >= this.getX()
                    && mouseX < this.getX() + visualWidth
                    && mouseY >= this.getY()
                    && mouseY < this.getY() + visualHeight;
        }
    }
}
