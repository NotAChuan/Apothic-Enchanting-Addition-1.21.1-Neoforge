package com.chuan.apothicenchantingaddition.client.screen;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.menu.FluxEnchantingMenu;
import com.chuan.apothicenchantingaddition.network.FluxActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;

import java.util.ArrayList;
import java.util.List;

public class FluxEnchantingScreen extends AbstractContainerScreen<FluxEnchantingMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apothicenchantingaddition",
            "textures/gui/flux_enchanting_table_gui.png"
    );

    private static final Material ENCHANTING_BOOK_MATERIAL = new Material(
            ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"),
            ResourceLocation.withDefaultNamespace("entity/enchanting_table_book")
    );

    private BookModel bookModel;

    private static final int REFRESH_BOOK_CENTER_X = 33;
    private static final int REFRESH_BOOK_CENTER_Y = 24;
    private static final int REFRESH_BOOK_CLICK_CENTER_X = 33;
    private static final int REFRESH_BOOK_CLICK_CENTER_Y = 34;
    private static final int REFRESH_BOOK_CLICK_SIZE = 20;
    private static final Component REFRESH_TOOLTIP = Component.translatable("gui.apothicenchantingaddition.refresh");

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int MAX_ENERGY = 1000000000;

    private static final int ENCHANT_BAR_X = 60;
    private static final int ENCHANT_BAR_Y = 14;
    private static final int ENCHANT_BAR_WIDTH = 108;
    private static final int ENCHANT_BAR_HEIGHT = 19;
    private static final int ENCHANT_BAR_SPACING = 19;

    private static final int ENCHANT_BAR_NORMAL_U = 148;
    private static final int ENCHANT_BAR_NORMAL_V = 199;
    private static final int ENCHANT_BAR_DISABLED_U = 148;
    private static final int ENCHANT_BAR_DISABLED_V = 218;
    private static final int ENCHANT_BAR_HOVER_U = 148;
    private static final int ENCHANT_BAR_HOVER_V = 237;

    private static final int ENCHANT_ICON_WIDTH = 12;
    private static final int ENCHANT_ICON_HEIGHT = 9;
    private static final int ENCHANT_TEXT_X = 78;
    private static final int ENCHANT_TEXT_Y_OFFSET = 6;
    private static final int ENCHANT_TEXT_MAX_WIDTH = 84;

    private static final int[] ENCHANT_ICON_X = {62, 62, 62};
    private static final int[] ENCHANT_ICON_Y = {18, 37, 56};
    private static final int[] ENCHANT_ICON_ENABLED_U = {3, 19, 35};
    private static final int[] ENCHANT_ICON_ENABLED_V = {226, 226, 226};
    private static final int[] ENCHANT_ICON_DISABLED_U = {3, 19, 35};
    private static final int[] ENCHANT_ICON_DISABLED_V = {242, 242, 242};

    private static final int STAT_BAR_X = 59;
    private static final int STAT_BAR_WIDTH = 110;
    private static final int STAT_BAR_HEIGHT = 5;
    private static final int ETERNA_BAR_Y = 75;
    private static final int QUANTA_BAR_Y = 85;
    private static final int ARCANA_BAR_Y = 95;
    private static final int ENERGY_BAR_Y = 105;

    private static final int ETERNA_FILL_U = 0;
    private static final int ETERNA_FILL_V = 199;
    private static final int QUANTA_FILL_U = 0;
    private static final int QUANTA_FILL_V = 204;
    private static final int ARCANA_FILL_U = 0;
    private static final int ARCANA_FILL_V = 209;
    private static final int ENERGY_FILL_U = 0;
    private static final int ENERGY_FILL_V = 214;

    private static final int LABEL_X = 20;
    private static final int ETERNA_LABEL_Y = 74;
    private static final int QUANTA_LABEL_Y = 84;
    private static final int ARCANA_LABEL_Y = 94;
    private static final int ENERGY_LABEL_Y = 104;

    public FluxEnchantingScreen(FluxEnchantingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 199;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(
                this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK)
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderBook(guiGraphics, x, y);

        int currentEnergy = menu.getEnergy();

        for (int i = 0; i < 3; i++) {
            int btnX = x + ENCHANT_BAR_X;
            int btnY = y + ENCHANT_BAR_Y + ENCHANT_BAR_SPACING * i;
            int feCost = menu.getDisplayedEnergyCost(i);
            boolean hasOption = menu.isInfusionOption(i) || menu.costs[i] > 0;
            boolean canClick = hasOption && currentEnergy >= feCost && (!menu.isInfusionOption(i) || menu.canInfuse());
            boolean hovered = isMouseOverEnchantBar(mouseX, mouseY, i);

            int textureU = ENCHANT_BAR_NORMAL_U;
            int textureV = ENCHANT_BAR_NORMAL_V;
            if (!canClick) {
                textureU = ENCHANT_BAR_DISABLED_U;
                textureV = ENCHANT_BAR_DISABLED_V;
            } else if (hovered) {
                textureU = ENCHANT_BAR_HOVER_U;
                textureV = ENCHANT_BAR_HOVER_V;
            }

            guiGraphics.blit(TEXTURE, btnX, btnY, textureU, textureV, ENCHANT_BAR_WIDTH, ENCHANT_BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

            if (hasOption) {
                int iconX = x + ENCHANT_ICON_X[i];
                int iconY = y + ENCHANT_ICON_Y[i];
                int iconU = canClick ? ENCHANT_ICON_ENABLED_U[i] : ENCHANT_ICON_DISABLED_U[i];
                int iconV = canClick ? ENCHANT_ICON_ENABLED_V[i] : ENCHANT_ICON_DISABLED_V[i];
                guiGraphics.blit(TEXTURE, iconX, iconY, iconU, iconV, ENCHANT_ICON_WIDTH, ENCHANT_ICON_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

                int textColor = canClick ? (hovered ? 0xFFFFD75E : 0xFF8B7C43) : 0xFF5B5850;
                if (menu.isInfusionOption(i)) {
                    guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.infusion"), x + ENCHANT_TEXT_X, btnY + ENCHANT_TEXT_Y_OFFSET, textColor, false);
                } else {
                    EnchantmentNames.getInstance().initSeed(menu.getEnchantmentSeed() + i);
                    FormattedText magicText = EnchantmentNames.getInstance().getRandomName(this.font, ENCHANT_TEXT_MAX_WIDTH);
                    guiGraphics.drawString(this.font, Language.getInstance().getVisualOrder(magicText), x + ENCHANT_TEXT_X, btnY + ENCHANT_TEXT_Y_OFFSET, textColor, false);
                }
            }
        }

        drawStatBar(guiGraphics, x + STAT_BAR_X, y + ETERNA_BAR_Y, ETERNA_FILL_U, ETERNA_FILL_V, menu.getEterna(), 100.0F);
        drawStatBar(guiGraphics, x + STAT_BAR_X, y + QUANTA_BAR_Y, QUANTA_FILL_U, QUANTA_FILL_V, menu.getQuanta(), 100.0F);
        drawStatBar(guiGraphics, x + STAT_BAR_X, y + ARCANA_BAR_Y, ARCANA_FILL_U, ARCANA_FILL_V, menu.getArcana(), 100.0F);
        drawStatBar(guiGraphics, x + STAT_BAR_X, y + ENERGY_BAR_Y, ENERGY_FILL_U, ENERGY_FILL_V, currentEnergy, (float) MAX_ENERGY);

        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.label.eterna"), x + LABEL_X, y + ETERNA_LABEL_Y, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.label.quanta"), x + LABEL_X, y + QUANTA_LABEL_Y, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.label.arcana"), x + LABEL_X, y + ARCANA_LABEL_Y, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.apothicenchantingaddition.label.energy"), x + LABEL_X, y + ENERGY_LABEL_Y, 0xFFFFFFFF, false);
    }

    private void renderBook(GuiGraphics guiGraphics, int x, int y) {
        if (this.bookModel == null || this.minecraft == null) {
            return;
        }

        boolean hasItem = this.menu.getSlot(0).hasItem();
        float open = hasItem ? 1.0F : 0.0F;

        PoseStack pose = guiGraphics.pose();
        MultiBufferSource.BufferSource buffer = this.minecraft.renderBuffers().bufferSource();

        pose.pushPose();
        pose.translate(x + REFRESH_BOOK_CENTER_X, y + REFRESH_BOOK_CENTER_Y + 10.0F, 100.0F);
        pose.scale(24.0F, 24.0F, 24.0F);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(12.0F));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(20.0F));
        pose.translate((1.0F - open) * 0.2F, (1.0F - open) * 0.1F, (1.0F - open) * 0.25F);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-(1.0F - open) * 90.0F - 90.0F));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));

        float pageFlipLeft = hasItem ? 0.1F : 0.0F;
        float pageFlipRight = hasItem ? 0.9F : 0.0F;
        this.bookModel.setupAnim(0.0F, pageFlipLeft, pageFlipRight, open);

        this.bookModel.renderToBuffer(
                pose,
                ENCHANTING_BOOK_MATERIAL.buffer(buffer, RenderType::entitySolid),
                15728880,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );

        buffer.endBatch();
        pose.popPose();
    }

    private void drawStatBar(GuiGraphics guiGraphics, int x, int y, int u, int v, float currentValue, float maxValue) {
        float ratio = maxValue <= 0 ? 0 : Mth.clamp(currentValue / maxValue, 0.0F, 1.0F);
        int fillWidth = Mth.floor(STAT_BAR_WIDTH * ratio);
        if (fillWidth > 0) {
            guiGraphics.blit(TEXTURE, x, y, u, v, fillWidth, STAT_BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    private boolean isMouseOverEnchantBar(double mouseX, double mouseY, int index) {
        int btnX = this.leftPos + ENCHANT_BAR_X;
        int btnY = this.topPos + ENCHANT_BAR_Y + ENCHANT_BAR_SPACING * index;
        return mouseX >= btnX && mouseX < btnX + ENCHANT_BAR_WIDTH && mouseY >= btnY && mouseY < btnY + ENCHANT_BAR_HEIGHT;
    }

    private boolean isMouseOverStatBar(double mouseX, double mouseY, int barY) {
        int x = this.leftPos + STAT_BAR_X;
        int y = this.topPos + barY;
        return mouseX >= x && mouseX <= x + STAT_BAR_WIDTH && mouseY >= y && mouseY <= y + STAT_BAR_HEIGHT;
    }

    private boolean isMouseOverRefreshBook(double mouseX, double mouseY) {
        int x = this.leftPos + REFRESH_BOOK_CLICK_CENTER_X - REFRESH_BOOK_CLICK_SIZE / 2;
        int y = this.topPos + REFRESH_BOOK_CLICK_CENTER_Y - REFRESH_BOOK_CLICK_SIZE / 2;
        return mouseX >= x && mouseX < x + REFRESH_BOOK_CLICK_SIZE && mouseY >= y && mouseY < y + REFRESH_BOOK_CLICK_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverRefreshBook(mouseX, mouseY)) {
            PacketDistributor.sendToServer(new FluxActionPayload(menu.getBlockEntity().getBlockPos(), 0));
            return true;
        }

        for (int i = 0; i < 3; i++) {
            if (isMouseOverEnchantBar(mouseX, mouseY, i)) {
                boolean hasOption = menu.isInfusionOption(i) || menu.costs[i] > 0;
                int feCost = menu.getDisplayedEnergyCost(i);
                boolean canClick = hasOption && menu.getEnergy() >= feCost && (!menu.isInfusionOption(i) || menu.canInfuse());
                if (canClick) {
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

        for (int i = 0; i < 3; i++) {
            if (isMouseOverEnchantBar(mouseX, mouseY, i)) {
                if (menu.isInfusionOption(i)) {
                    int feCost = menu.getDisplayedEnergyCost(i);
                    boolean canAfford = menu.getEnergy() >= feCost;
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("gui.apothicenchantingaddition.infusion").withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.translatable(
                            "gui.apothicenchantingaddition.flux_enchanting.energy_cost",
                            String.format("%,d", feCost)
                    ).withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED));
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    break;
                } else {
                    int costLevel = menu.costs[i];
                    if (costLevel > 0) {
                        List<Component> tooltip = new ArrayList<>();
                        List<EnchantmentInstance> clues = menu.clientClues.get(i);

                        if (clues != null && !clues.isEmpty()) {
                            if (menu.clientAllRevealed[i]) {
                                tooltip.add(Component.translatable("gui.apothicenchantingaddition.all_revealed")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
                            }

                            for (EnchantmentInstance clue : clues) {
                                Component enchantName = Enchantment.getFullname(clue.enchantment, clue.level);
                                tooltip.add(enchantName);
                            }

                            if (!menu.clientAllRevealed[i]) {
                                tooltip.add(Component.translatable("gui.apothicenchantingaddition.some_revealed")
                                        .withStyle(ChatFormatting.GRAY));
                            }
                        } else {
                            tooltip.add(Component.empty().append(Component.translatable("container.enchant.clue", ""))
                                    .withStyle(ChatFormatting.WHITE));
                        }

                        int feCost = menu.getDisplayedEnergyCost(i);
                        boolean canAfford = menu.getEnergy() >= feCost;
                        tooltip.add(Component.translatable(
                                "gui.apothicenchantingaddition.flux_enchanting.energy_cost",
                                String.format("%,d", feCost)
                        ).withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED));

                        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    }
                }
            }
        }

        if (isMouseOverRefreshBook(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(REFRESH_TOOLTIP), mouseX, mouseY);
        }

        if (isMouseOverStatBar(mouseX, mouseY, ETERNA_BAR_Y)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable(
                            "gui.apothicenchantingaddition.stat.eterna",
                            String.format("%.2f / 100", menu.getEterna())
                    )),
                    mouseX, mouseY);

        } else if (isMouseOverStatBar(mouseX, mouseY, QUANTA_BAR_Y)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable(
                            "gui.apothicenchantingaddition.stat.quanta",
                            String.format("%.2f%%", menu.getQuanta())
                    )),
                    mouseX, mouseY);

        } else if (isMouseOverStatBar(mouseX, mouseY, ARCANA_BAR_Y)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable(
                            "gui.apothicenchantingaddition.stat.arcana",
                            String.format("%.2f%%", menu.getArcana())
                    )),
                    mouseX, mouseY);

        } else if (isMouseOverStatBar(mouseX, mouseY, ENERGY_BAR_Y)) {
            List<Component> tooltips = List.of(
                    Component.translatable(
                            "gui.apothicenchantingaddition.stat.energy",
                            String.format("%,d / %,d FE", menu.getEnergy(), MAX_ENERGY)
                    ),
                    Component.translatable(
                            "gui.apothicenchantingaddition.energy.cost",
                            String.format("%,d", ApothicAdditionConfig.FLUX_ENCHANTER_TICK_COST.get())
                    )
            );

            guiGraphics.renderComponentTooltip(this.font, tooltips, mouseX, mouseY);
        }
    }
}
