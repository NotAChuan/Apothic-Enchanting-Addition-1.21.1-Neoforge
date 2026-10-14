package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.recipe.RitualCraftingRecipe;
import com.chuan.apothicflux.recipe.RitualStatRequirement;
import com.chuan.apothicflux.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RitualRecipeCategory implements IRecipeCategory<RitualCraftingRecipe> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/octagram.png");
    private static final ResourceLocation ENTITY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/entity.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/arrow.png");
    /** 需求环的两个端点图：全 0（暗）与全 100（亮）。 */
    private static final ResourceLocation RING_DARK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/requirement_1.png");
    private static final ResourceLocation RING_LIGHT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/requirement_2.png");

    private static final int WIDTH = 184;
    private static final int HEIGHT = 118;

    private static final int OCT_SIZE = 80;
    private static final int OCT_X = 18;
    private static final int OCT_Y = 18;
    private static final int OCT_CENTER_X = OCT_X + OCT_SIZE / 2;
    private static final int OCT_CENTER_Y = OCT_Y + OCT_SIZE / 2;
    private static final float OUTER_RADIUS = 30.0f;

    private static final int ARROW_X = 118;
    private static final int ARROW_Y = 50;
    private static final int ARROW_W = 16;
    private static final int ARROW_H = 16;

    private static final int OUTPUT_X = 138;
    private static final int OUTPUT_CENTER_Y = 58;

    // Ring texture is 256x256; the UI renders it at 108x108.
    private static final int RING_TEX_SIZE = 256;
    private static final int RING_SIZE = 108;
    private static final int RING_X = OCT_CENTER_X - RING_SIZE / 2;
    private static final int RING_Y = OCT_CENTER_Y - RING_SIZE / 2;
    private static final float TEX_CENTER = 128.0f;
    private static final int ALPHA_THRESHOLD = 128;

    // Angles are in screen coordinates: 0 right, 90 down, 180 left, 270 up.
    // Arcana: from top toward left (counterclockwise).
    private static final List<RequirementArc> ARCS = List.of(
            new RequirementArc("arcana", 0xA64BD6, 152.5f, 268f, true),
            new RequirementArc("eterna", 0x3FA845, 272f, 390f, true),
            new RequirementArc("quanta", 0xC4443A, 29f, 152f, true)
    );

    private record RequirementArc(String stat, int color, float startDeg,
                                  float endDeg, boolean clockwise) {
        float midDeg() {
            return (startDeg + endDeg) / 2f;
        }

        boolean contains(float angle) {
            return angle >= startDeg && angle <= endDeg;
        }

        boolean isFilled(float angle, float percent) {
            float progress = Math.max(0f, Math.min(1f, percent / 100f));
            if (progress <= 0f || !contains(angle)) return false;
            if (progress >= 1f) return true;
            float span = endDeg - startDeg;
            return clockwise
                    ? angle <= startDeg + span * progress
                    : angle >= endDeg - span * progress;
        }
    }

    // Lazy loaded; resource reload can be handled by clearing this cache.
    @Nullable
    private NativeImage ringMask;

    private final IDrawable icon;

    public RitualRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.FLUX_ENCHANTING_TABLE.get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RitualCraftingRecipe> getRecipeType() {
        return ApothicAdditionJeiPlugin.RITUAL_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apothic_flux.category.ritual");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RitualCraftingRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> inputs = recipe.inputs();
        int count = inputs.size();
        if (count == 0) return;

        // 中心：第一个输入物品
        builder.addSlot(RecipeIngredientRole.INPUT, OCT_CENTER_X - 8, OCT_CENTER_Y - 8)
                .addIngredients(inputs.get(0));

        // 外圈：其余输入物品平分圆
        int outerCount = Math.min(count - 1, 16);
        for (int i = 0; i < outerCount; i++) {
            float angle = (float) (i * 2 * Math.PI / outerCount);
            int slotX = (int) (OCT_CENTER_X + OUTER_RADIUS * Math.cos(angle)) - 8;
            int slotY = (int) (OCT_CENTER_Y + OUTER_RADIUS * Math.sin(angle)) - 8;
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY)
                    .addIngredients(inputs.get(i + 1));
        }

        // 统计输出数量，用于布局计算
        int totalOutputs = countOutputs(recipe);

        // 输出物品槽
        if (!recipe.outputItem().isEmpty()) {
            int[] pos = getOutputPos(totalOutputs, 0);
            builder.addSlot(RecipeIngredientRole.OUTPUT, pos[0], pos[1])
                    .addItemStack(recipe.outputItem());
        }

        // 输出流体槽
        Optional<String> fluidOpt = recipe.outputFluid();
        if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) {
            ResourceLocation fluidRL = ResourceLocation.tryParse(fluidOpt.get());
            if (fluidRL != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(fluidRL);
                if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    int fluidIndex = recipe.outputItem().isEmpty() ? 0 : 1;
                    int[] pos = getOutputPos(totalOutputs, fluidIndex);
                    builder.addSlot(RecipeIngredientRole.OUTPUT, pos[0], pos[1])
                            .addFluidStack(fluid, FluidType.BUCKET_VOLUME);
                }
            }
        }
    }

    @Override
    public void draw(RitualCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {

        // 绘制八芒星背景
        guiGraphics.blit(BG_TEXTURE, OCT_X, OCT_Y, 0, 0, OCT_SIZE, OCT_SIZE, OCT_SIZE, OCT_SIZE);

        // 绘制需求环
        drawRequirementRing(guiGraphics, recipe);

        // 绘制箭头
        guiGraphics.blit(ARROW_TEXTURE, ARROW_X, ARROW_Y, 0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);

        // 实体输出渲染
        Optional<String> entityOpt = recipe.outputEntity();
        if (entityOpt.isPresent() && !entityOpt.get().isEmpty()) {
            int totalOutputs = countOutputs(recipe);
            int entityIndex = 0;
            if (!recipe.outputItem().isEmpty()) entityIndex++;
            Optional<String> fluidOpt = recipe.outputFluid();
            if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) entityIndex++;

            int[] pos = getOutputPos(totalOutputs, entityIndex);
            EntityRenderer.render(guiGraphics, entityOpt.get(),
                    pos[0] + 8, pos[1] + 8, 24, ENTITY_TEXTURE);
        }
    }

    // ===================== Ring drawing =====================

    /**
     * Draw at the native 256x256 pixel grid and scale the pose once.
     * This avoids rounding and UV stretching between scanlines.
     */
    private void drawRequirementRing(GuiGraphics graphics, RitualCraftingRecipe recipe) {
        graphics.pose().pushPose();
        graphics.pose().translate(RING_X, RING_Y, 0);
        float scale = (float) RING_SIZE / RING_TEX_SIZE;
        graphics.pose().scale(scale, scale, 1f);
        try {
            graphics.blit(RING_DARK_TEXTURE, 0, 0, 0, 0,
                    RING_TEX_SIZE, RING_TEX_SIZE, RING_TEX_SIZE, RING_TEX_SIZE);

            NativeImage mask = getRingMask();
            if (mask == null) return;

            for (RequirementArc arc : ARCS) {
                float fill = fillValue(recipe, arc.stat());
                if (fill <= 0f) continue;
                drawNativeFilledArc(graphics, mask, arc, fill);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private void drawNativeFilledArc(GuiGraphics graphics, NativeImage mask,
                                     RequirementArc arc, float fill) {
        for (int y = 0; y < RING_TEX_SIZE; y++) {
            int runStart = -1;
            for (int x = 0; x <= RING_TEX_SIZE; x++) {
                boolean lit = false;
                if (x < RING_TEX_SIZE && isOpaque(mask, x, y)) {
                    float angle = pixelAngle(x, y, arc.midDeg());
                    lit = arc.isFilled(angle, fill);
                }
                if (lit && runStart < 0) runStart = x;
                if (!lit && runStart >= 0) {
                    int width = x - runStart;
                    graphics.blit(RING_LIGHT_TEXTURE, runStart, y,
                            runStart, y, width, 1, RING_TEX_SIZE, RING_TEX_SIZE);
                    runStart = -1;
                }
            }
        }
    }

    private static boolean isOpaque(NativeImage image, int x, int y) {
        return ((image.getPixelRGBA(x, y) >>> 24) & 0xff) >= ALPHA_THRESHOLD;
    }

    private static float pixelAngle(int x, int y, float reference) {
        float raw = (float) Math.toDegrees(
                Math.atan2(y + 0.5f - TEX_CENTER, x + 0.5f - TEX_CENTER));
        return unwrapDeg(raw, reference);
    }

    @Nullable
    private NativeImage getRingMask() {
        if (ringMask != null) return ringMask;
        var resource = Minecraft.getInstance().getResourceManager()
                .getResource(RING_LIGHT_TEXTURE);
        if (resource.isEmpty()) return null;
        try (InputStream stream = resource.get().open()) {
            ringMask = NativeImage.read(stream);
            return ringMask;
        } catch (IOException exception) {
            return null;
        }
    }

    private static float unwrapDeg(float deg, float reference) {
        while (deg < reference - 180f) deg += 360f;
        while (deg > reference + 180f) deg -= 360f;
        return deg;
    }

    /** 该属性的填充比例（0~100）：有下限取下限，否则取上限，都没有则 0。 */
    private static float fillValue(RitualCraftingRecipe recipe, String stat) {
        RitualStatRequirement min = recipe.requirements();
        RitualStatRequirement max = recipe.maxRequirements().orElse(null);

        float minValue = statValue(stat, min);
        float maxValue = max == null ? RitualStatRequirement.NO_MAX : statValue(stat, max);

        boolean hasMin = minValue > RitualStatRequirement.NO_MIN;
        boolean hasMax = maxValue < RitualStatRequirement.NO_MAX;

        if (hasMin) return clampPercent(minValue);
        if (hasMax) return clampPercent(maxValue);
        return 0.0f;
    }

    private static float clampPercent(float value) {
        return Math.max(0.0f, Math.min(100.0f, value));
    }

    // ===================== 悬停提示 =====================

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RitualCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        // 箭头：制作时间
        if (mouseX >= ARROW_X && mouseX < ARROW_X + ARROW_W
                && mouseY >= ARROW_Y && mouseY < ARROW_Y + ARROW_H) {
            int seconds = Math.max(1, recipe.craftTime() / 20);
            tooltip.add(Component.translatable("jei.apothic_flux.ritual.time", seconds));
            return;
        }

        // 需求环：落在哪一段弧上就显示那项属性的需求
        RequirementArc arc = arcAt(mouseX, mouseY);
        if (arc != null) {
            tooltip.add(colored(buildRequirementLine(recipe, arc.stat()), arc.color()));
        }
    }

    /**
     * Hit-test the exact visible source pixel, not an approximate annulus.
     * The full ring remains hoverable even when the requirement is 0%.
     */
    @Nullable
    private RequirementArc arcAt(double mouseX, double mouseY) {
        double localX = mouseX - RING_X;
        double localY = mouseY - RING_Y;
        if (localX < 0 || localY < 0
                || localX >= RING_SIZE || localY >= RING_SIZE) return null;

        int texX = Math.min(RING_TEX_SIZE - 1,
                (int) Math.floor(localX * RING_TEX_SIZE / RING_SIZE));
        int texY = Math.min(RING_TEX_SIZE - 1,
                (int) Math.floor(localY * RING_TEX_SIZE / RING_SIZE));

        NativeImage mask = getRingMask();
        if (mask == null || !isOpaque(mask, texX, texY)) return null;

        for (RequirementArc arc : ARCS) {
            if (arc.contains(pixelAngle(texX, texY, arc.midDeg()))) {
                return arc;
            }
        }
        return null;
    }

    /** 单项属性的需求文案："位阶 任意" / "位阶 ≥ 40" / "位阶 ≤ 65" / "45 ≤ 位阶 ≤ 65"。 */
    private Component buildRequirementLine(RitualCraftingRecipe recipe, String stat) {
        RitualStatRequirement min = recipe.requirements();
        RitualStatRequirement max = recipe.maxRequirements().orElse(null);

        float minValue = statValue(stat, min);
        float maxValue = max == null ? RitualStatRequirement.NO_MAX : statValue(stat, max);

        boolean hasMin = minValue > RitualStatRequirement.NO_MIN;
        boolean hasMax = maxValue < RitualStatRequirement.NO_MAX;

        Component label = Component.translatable("jei.apothic_flux.ritual.requirement." + stat);
        int minInt = Math.round(minValue);
        int maxInt = Math.round(maxValue);

        if (hasMin && hasMax) {
            return Component.translatable("jei.apothic_flux.ritual.requirement.range", minInt, label, maxInt);
        } else if (hasMin) {
            return Component.translatable("jei.apothic_flux.ritual.requirement.min", label, minInt);
        } else if (hasMax) {
            return Component.translatable("jei.apothic_flux.ritual.requirement.max", label, maxInt);
        }
        return Component.translatable("jei.apothic_flux.ritual.requirement.any", label);
    }

    private static Component colored(Component text, int rgb) {
        return text.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static float statValue(String stat, RitualStatRequirement requirement) {
        return switch (stat) {
            case "eterna" -> requirement.eterna();
            case "quanta" -> requirement.quanta();
            case "arcana" -> requirement.arcana();
            default -> throw new IllegalArgumentException("Unknown ritual stat: " + stat);
        };
    }

    private int countOutputs(RitualCraftingRecipe recipe) {
        int count = 0;
        if (!recipe.outputItem().isEmpty()) count++;
        Optional<String> fluidOpt = recipe.outputFluid();
        if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) count++;
        Optional<String> entityOpt = recipe.outputEntity();
        if (entityOpt.isPresent() && !entityOpt.get().isEmpty()) count++;
        return count;
    }

    private int[] getOutputPos(int totalOutputs, int index) {
        int rightAreaStartX = OUTPUT_X;
        int rightAreaWidth = WIDTH - OUTPUT_X - 2;
        int slotSize = 18;

        return switch (totalOutputs) {
            case 1 -> new int[]{
                    rightAreaStartX + (rightAreaWidth - slotSize) / 2,
                    OUTPUT_CENTER_Y - slotSize / 2
            };
            case 2 -> {
                int totalW = slotSize * 2 + 4;
                int startX = rightAreaStartX + (rightAreaWidth - totalW) / 2;
                yield new int[]{startX + index * (slotSize + 4), OUTPUT_CENTER_Y - slotSize / 2};
            }
            case 3 -> {
                if (index == 0) {
                    yield new int[]{
                            rightAreaStartX + (rightAreaWidth - slotSize) / 2,
                            OUTPUT_CENTER_Y - slotSize - 2
                    };
                } else {
                    int totalW = slotSize * 2 + 4;
                    int startX = rightAreaStartX + (rightAreaWidth - totalW) / 2;
                    yield new int[]{startX + (index - 1) * (slotSize + 4), OUTPUT_CENTER_Y + 2};
                }
            }
            default -> new int[]{rightAreaStartX, OUTPUT_CENTER_Y - slotSize / 2};
        };
    }
}
