package com.chuan.apothicflux.client.screen;

import com.chuan.apothicflux.block.entity.FluxInfuserBlockEntity;
import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.menu.FluxInfuserMenu;
import com.chuan.apothicflux.util.FluxInfuserCandidates;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 自动通量灌注器 GUI。
 * <p>
 * 开发阶段**完全用代码绘制**（面板、槽位框、进度条、能量条、翻页按钮、发光边框），
 * 不依赖任何 GUI 贴图；后续替换为正式贴图时只需改写 {@link #renderBg}。
 * <p>
 * 左侧 3×4 候选区不是真实槽位，因此点击与 tooltip 都在这里手工处理。
 */
public class FluxInfuserScreen extends AbstractContainerScreen<FluxInfuserMenu> {

    // ===== 配色 =====
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    /** 原版槽位：上/左 1px 深色。 */
    private static final int COLOR_SLOT_BEVEL_DARK = 0xFF373737;
    /** 原版槽位：下/右 1px 白色高光。 */
    private static final int COLOR_SLOT_BEVEL_LIGHT = 0xFFFFFFFF;
    /** 原版槽位：内部 16×16 浅灰底色。 */
    private static final int COLOR_SLOT_INNER = 0xFF8B8B8B;
    private static final int COLOR_TEXT = 0xFF404040;
    private static final int COLOR_GLOW = 0xFFFFD54A;
    private static final int COLOR_BAR_BG = 0xFF373737;
    private static final int COLOR_PROGRESS = 0xFF3CB043;
    private static final int COLOR_ENERGY = 0xFFD83A3A;
    private static final int COLOR_BUTTON = 0xFF8B8B8B;
    private static final int COLOR_BUTTON_HOVER = 0xFFBDBDBD;
    private static final int COLOR_BUTTON_PRESSED = 0xFF6E6E6E;
    /** 置灰用的着色系数（记录表已满且当前输入物无记录时）。 */
    private static final float LOCKED_TINT = 0.30F;

    // ===== 底部信息区 =====
    private static final int STATS_Y = 94;
    private static final int RECORD_Y = 104;
    private static final int PROGRESS_X = 70;
    private static final int PROGRESS_Y = 104;
    private static final int PROGRESS_W = 90;
    private static final int PROGRESS_H = 8;

    // ===== 输入槽下方的翻页按钮 =====
    private static final int BUTTON_SIZE = 11;
    private static final int PREV_BUTTON_X = 66;
    private static final int NEXT_BUTTON_X = 92;
    private static final int PAGE_BUTTON_Y = 66;

    // ===== 能量条（竖，贴右边）=====
    private static final int ENERGY_X = 164;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_W = 8;
    private static final int ENERGY_H = 74;

    /** -1 = 未按下；0 = 上一页；1 = 下一页。 */
    private int pressedButton = -1;

    public FluxInfuserScreen(FluxInfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = FluxInfuserMenu.GUI_WIDTH;
        this.imageHeight = FluxInfuserMenu.GUI_HEIGHT;
    }

    // ===================== 绘制 =====================

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 候选列表在渲染前刷新，保证本帧的槽位框 / tooltip / 点击命中都是一致的
        this.menu.refreshCandidatesIfNeeded();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        drawPanel(guiGraphics);

        // 左侧候选区：属性不满足的候选也会展示，但置灰且不可点击
        boolean lockedByRecordLimit = this.menu.isLockedByRecordLimit();
        for (int i = 0; i < FluxInfuserBlockEntity.CANDIDATE_COUNT; i++) {
            int slotX = candidateSlotX(i);
            int slotY = candidateSlotY(i);
            drawSlotFrame(guiGraphics, slotX - 1, slotY - 1);

            FluxInfuserCandidates.Candidate candidate = this.menu.candidateAt(i);
            if (candidate != null) {
                // 两种置灰原因：属性不满足（不可选）／记录表已满（仍可点击以弹出上限提示）
                boolean greyed = !candidate.satisfied() || lockedByRecordLimit;
                if (greyed) {
                    RenderSystem.setShaderColor(LOCKED_TINT, LOCKED_TINT, LOCKED_TINT, 1.0F);
                }
                guiGraphics.renderFakeItem(candidate.output(), slotX, slotY);
                // 数量角标 + 耐久条：原版槽位是分两步画的，
                // renderFakeItem 只画物品模型，数量与耐久由 renderItemDecorations 负责。
                // 放在 setShaderColor 还原之前，置灰时角标一起变暗。
                guiGraphics.renderItemDecorations(this.font, candidate.output(), slotX, slotY);
                if (greyed) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                // 只有可选中的候选才画发光边框；被属性锁住的候选不画，避免与置灰语义冲突
                if (candidate.satisfied() && this.menu.isGlowing(i)) {
                    drawGlow(guiGraphics, slotX - 1, slotY - 1);
                }
            }
        }

        // 中间输入槽与右侧输出槽的边框（物品由原版槽位渲染负责）
        // 注意：必须用 drawSlotFrameLocal（内部补 leftPos/topPos），
        // 因为 FluxInfuserMenu 里定义的都是 GUI 内局部坐标。
        drawSlotFrameLocal(guiGraphics, FluxInfuserMenu.INPUT_X, FluxInfuserMenu.INPUT_Y);
        for (int row = 0; row < FluxInfuserMenu.OUTPUT_ROWS; row++) {
            for (int col = 0; col < FluxInfuserMenu.OUTPUT_COLS; col++) {
                drawSlotFrameLocal(guiGraphics,
                        FluxInfuserMenu.OUTPUT_X0 + col * FluxInfuserMenu.SLOT_STEP,
                        FluxInfuserMenu.OUTPUT_Y0 + row * FluxInfuserMenu.SLOT_STEP);
            }
        }

        // 玩家背包 3 行 + 快捷栏的边框
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrameLocal(guiGraphics,
                        FluxInfuserMenu.PLAYER_INV_X + col * FluxInfuserMenu.SLOT_STEP,
                        FluxInfuserMenu.PLAYER_INV_Y + row * FluxInfuserMenu.SLOT_STEP);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrameLocal(guiGraphics,
                    FluxInfuserMenu.PLAYER_INV_X + col * FluxInfuserMenu.SLOT_STEP,
                    FluxInfuserMenu.HOTBAR_Y);
        }

        drawEnergyBar(guiGraphics);
        drawTexts(guiGraphics);
        drawPageControls(guiGraphics, mouseX, mouseY);
        drawProgressBar(guiGraphics);
    }

    private void drawPanel(GuiGraphics guiGraphics) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, COLOR_PANEL);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, COLOR_PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, COLOR_PANEL_LIGHT);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, COLOR_PANEL_DARK);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, COLOR_PANEL_DARK);
        guiGraphics.fill(x + 3, y + 15, x + this.imageWidth - 3, y + 92, 0xFFB0B0B0);
    }

    /**
     * 画「GUI 内局部坐标」定义的槽位边框（{@link FluxInfuserMenu} 里的坐标就是这一类）。
     * 内部自动补上 leftPos/topPos，传入的是**槽位物品坐标**（不是边框左上角）。
     */
    private void drawSlotFrameLocal(GuiGraphics guiGraphics, int localX, int localY) {
        drawSlotFrame(guiGraphics, this.leftPos + localX - 1, this.topPos + localY - 1);
    }

    /**
     * 画「屏幕绝对坐标」的槽位边框：上/左 1px 深色、下/右 1px 白色高光、内部 16×16 浅灰。
     * 传入的是边框左上角坐标，即「槽位物品绝对坐标 - 1」，物品恰好落在内部 16×16 区域。
     */
    private void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, COLOR_SLOT_BEVEL_DARK);
        guiGraphics.fill(x + 1, y + 1, x + 18, y + 18, COLOR_SLOT_BEVEL_LIGHT);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT_INNER);
    }

    /** 选中且尚未产出过的候选：发光边框。 */
    private void drawGlow(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.renderOutline(x - 1, y - 1, 20, 20, COLOR_GLOW);
        guiGraphics.renderOutline(x, y, 18, 18, COLOR_GLOW);
    }

    private void drawEnergyBar(GuiGraphics guiGraphics) {
        int x = this.leftPos + ENERGY_X;
        int y = this.topPos + ENERGY_Y;
        guiGraphics.fill(x, y, x + ENERGY_W, y + ENERGY_H, COLOR_BAR_BG);

        int capacity = Math.max(1, ApothicAdditionConfig.FLUX_INFUSER_CAPACITY.get());
        int stored = Mth.clamp(this.menu.getEnergy(), 0, capacity);
        int filled = (int) ((long) (ENERGY_H - 2) * stored / capacity);
        if (filled > 0) {
            guiGraphics.fill(x + 1, y + ENERGY_H - 1 - filled, x + ENERGY_W - 1, y + ENERGY_H - 1, COLOR_ENERGY);
        }
        guiGraphics.renderOutline(x, y, ENERGY_W, ENERGY_H, COLOR_PANEL_DARK);
    }

    private void drawTexts(GuiGraphics guiGraphics) {
        int x = this.leftPos + 8;
        int y = this.topPos + 6;
        guiGraphics.drawString(this.font, this.title, x, y, COLOR_TEXT, false);

        // 属性行
        String stats = Component.translatable("gui.apothic_flux.label.eterna").getString() + ": "
                + formatStat(this.menu.getEterna()) + "  "
                + Component.translatable("gui.apothic_flux.label.quanta").getString() + ": "
                + formatStat(this.menu.getQuanta()) + "  "
                + Component.translatable("gui.apothic_flux.label.arcana").getString() + ": "
                + formatStat(this.menu.getArcana());
        guiGraphics.drawString(this.font, stats, this.leftPos + 8, this.topPos + STATS_Y, COLOR_TEXT, false);

        // 记录行
        String records = Component.translatable("gui.apothic_flux.flux_infuser.record",
                this.menu.getRecordCount(), this.menu.getRecordLimit()).getString();
        guiGraphics.drawString(this.font, records, this.leftPos + 8, this.topPos + RECORD_Y,
                this.menu.isLockedByRecordLimit() ? 0xFFAA0000 : COLOR_TEXT, false);
    }

    private static String formatStat(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format("%.1f", value);
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        int x = this.leftPos + PROGRESS_X;
        int y = this.topPos + PROGRESS_Y;
        guiGraphics.fill(x, y, x + PROGRESS_W, y + PROGRESS_H, COLOR_BAR_BG);

        int craftTime = Math.max(1, ApothicAdditionConfig.FLUX_INFUSER_CRAFT_TIME.get());
        int progress = Mth.clamp(this.menu.getProgress(), 0, craftTime);
        int filled = (int) ((long) (PROGRESS_W - 2) * progress / craftTime);
        if (filled > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + filled, y + PROGRESS_H - 1, COLOR_PROGRESS);
        }
        guiGraphics.renderOutline(x, y, PROGRESS_W, PROGRESS_H, COLOR_PANEL_DARK);
    }

    private void drawPageControls(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.hasMultiplePages()) {
            return;
        }

        drawPageButton(guiGraphics, PREV_BUTTON_X, mouseX, mouseY, 0, "<");
        drawPageButton(guiGraphics, NEXT_BUTTON_X, mouseX, mouseY, 1, ">");

        String page = Component.translatable("gui.apothic_flux.flux_infuser.page",
                this.menu.getPage() + 1, this.menu.getPageCount()).getString();
        int centerX = this.leftPos + (PREV_BUTTON_X + BUTTON_SIZE + NEXT_BUTTON_X) / 2;
        guiGraphics.drawString(this.font, page, centerX - this.font.width(page) / 2,
                this.topPos + PAGE_BUTTON_Y + 2, COLOR_TEXT, false);
    }

    private void drawPageButton(GuiGraphics guiGraphics, int localX, int mouseX, int mouseY, int id, String label) {
        int x = this.leftPos + localX;
        int y = this.topPos + PAGE_BUTTON_Y;
        boolean hovered = isInside(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);

        int color = this.pressedButton == id ? COLOR_BUTTON_PRESSED
                : (hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON);
        guiGraphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, color);
        guiGraphics.renderOutline(x, y, BUTTON_SIZE, BUTTON_SIZE, COLOR_PANEL_DARK);
        guiGraphics.drawString(this.font, label, x + (BUTTON_SIZE - this.font.width(label)) / 2 + 1,
                y + 2, COLOR_TEXT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题与所有文字都在 renderBg 里以绝对坐标绘制，这里留空避免重复
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 原版槽位（输入 / 输出 / 玩家背包）的 tooltip
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        // 左侧候选区是手工绘制的，单独补 tooltip
        int slot = getHoveredCandidateSlot(mouseX, mouseY);
        if (slot >= 0) {
            FluxInfuserCandidates.Candidate candidate = this.menu.candidateAt(slot);
            if (candidate != null) {
                guiGraphics.renderTooltip(this.font, candidate.output(), mouseX, mouseY);
            }
        }
    }

    // ===================== 点击 =====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 候选选择：
        // - 属性不满足 → 置灰不可选，点击直接吞掉、不发请求
        // - 属性满足但「记录表已满」→ 仍要发请求，由服务端拒绝并弹出上限提示
        int slot = getHoveredCandidateSlot(mouseX, mouseY);
        if (slot >= 0) {
            FluxInfuserCandidates.Candidate candidate = this.menu.candidateAt(slot);
            if (candidate != null && candidate.satisfied()) {
                this.menu.requestSelect(candidate.id());
            }
            return true;
        }

        if (this.menu.hasMultiplePages()) {
            if (isInside(mouseX, mouseY, this.leftPos + PREV_BUTTON_X, this.topPos + PAGE_BUTTON_Y,
                    BUTTON_SIZE, BUTTON_SIZE)) {
                this.pressedButton = 0;
                this.menu.changePage(-1);
                return true;
            }
            if (isInside(mouseX, mouseY, this.leftPos + NEXT_BUTTON_X, this.topPos + PAGE_BUTTON_Y,
                    BUTTON_SIZE, BUTTON_SIZE)) {
                this.pressedButton = 1;
                this.menu.changePage(1);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.pressedButton = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ===================== 命中测试 =====================

    private int candidateSlotX(int index) {
        return this.leftPos + FluxInfuserMenu.CANDIDATE_X0
                + (index % FluxInfuserMenu.CANDIDATE_COLS) * FluxInfuserMenu.SLOT_STEP;
    }

    private int candidateSlotY(int index) {
        return this.topPos + FluxInfuserMenu.CANDIDATE_Y0
                + (index / FluxInfuserMenu.CANDIDATE_COLS) * FluxInfuserMenu.SLOT_STEP;
    }

    private int getHoveredCandidateSlot(double mouseX, double mouseY) {
        for (int i = 0; i < FluxInfuserBlockEntity.CANDIDATE_COUNT; i++) {
            if (isInside(mouseX, mouseY, candidateSlotX(i) - 1, candidateSlotY(i) - 1, 18, 18)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
