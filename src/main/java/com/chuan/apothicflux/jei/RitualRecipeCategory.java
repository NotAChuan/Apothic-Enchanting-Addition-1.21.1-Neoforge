package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.recipe.RitualCraftingRecipe;
import com.chuan.apothicflux.recipe.RitualDrawingRecipe;
import com.chuan.apothicflux.recipe.RitualStatRequirement;
import com.chuan.apothicflux.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class RitualRecipeCategory implements IRecipeCategory<RitualCraftingRecipe> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/octagram.png");
    private static final ResourceLocation ENTITY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/entity.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "textures/gui/jei/arrow.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 110;

    private static final int OCT_SIZE = 80;
    private static final int OCT_X = 4;
    private static final int OCT_Y = 10;
    private static final int OCT_CENTER_X = OCT_X + OCT_SIZE / 2;
    private static final int OCT_CENTER_Y = OCT_Y + OCT_SIZE / 2;
    private static final float OUTER_RADIUS = 30.0f;

    private static final int ARROW_X = OCT_X + OCT_SIZE + 4;
    private static final int ARROW_Y = 42;
    private static final int ARROW_W = 16;
    private static final int ARROW_H = 16;

    private static final int OUTPUT_X = ARROW_X + ARROW_W + 4;
    private static final int OUTPUT_CENTER_Y = 50;
    private static final int TIME_Y = 76;
    private static final int REQUIREMENT_Y = 96;

    private final IDrawable background;
    private final IDrawable icon;

    public RitualRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
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

        // ================== 绘制配方时间 ==================

        // 1 秒 = 20 Tick，将 craftTime() 转化为秒数
//        int seconds = recipe.craftTime() / 20;
        int seconds = Math.max(1, recipe.craftTime() / 20);

        // 获取翻译文本
        Component timeText = Component.translatable("jei.apothic_flux.ritual.time", seconds);

        // 测量字体宽度
        int textWidth = Minecraft.getInstance().font.width(timeText);
        int textX = (WIDTH - textWidth) / 2 + 10;
        int textY = TIME_Y;

        // 使用深灰色 (0x555555) 绘制，且关闭文字阴影 (false)，契合 JEI 风格
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                timeText,
                textX,
                textY,
                0x555555,
                false
        );

        Component requirementText = buildRequirementText(recipe);
        if (requirementText != null) {
            int requirementWidth = Minecraft.getInstance().font.width(requirementText);
            int requirementX = (WIDTH - requirementWidth) / 2;
            int requirementY = REQUIREMENT_Y;

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    requirementText,
                    requirementX,
                    requirementY,
                    0x555555,
                    false
            );
        }
    }

    private Component buildRequirementText(RitualCraftingRecipe recipe) {
        RitualStatRequirement min = recipe.requirements();
        RitualStatRequirement max = recipe.maxRequirements().orElse(null);

        List<Component> parts = new ArrayList<>(3);
        appendRequirement(parts, "eterna", min, max);
        appendRequirement(parts, "quanta", min, max);
        appendRequirement(parts, "arcana", min, max);

        if (parts.isEmpty()) {
            return null;
        }

        MutableComponent line = Component.literal("");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                line.append(" ");
            }
            line.append(parts.get(i));
        }
        return line;
    }

    private void appendRequirement(List<Component> parts, String stat,
                                   RitualStatRequirement min, RitualStatRequirement max) {
        float minValue = statValue(stat, min);
        float maxValue = max == null ? RitualStatRequirement.NO_MAX : statValue(stat, max);

        boolean hasMin = minValue > RitualStatRequirement.NO_MIN;
        boolean hasMax = maxValue < RitualStatRequirement.NO_MAX;

        if (!hasMin && !hasMax) {
            return;
        }

        Component label = Component.translatable("jei.apothic_flux.ritual.requirement." + stat);
        int minInt = Math.round(minValue);
        int maxInt = Math.round(maxValue);

        if (hasMin && hasMax) {
            parts.add(Component.translatable("jei.apothic_flux.ritual.requirement.range", label, minInt, maxInt));
        } else if (hasMin) {
            parts.add(Component.translatable("jei.apothic_flux.ritual.requirement.min", label, minInt));
        } else {
            parts.add(Component.translatable("jei.apothic_flux.ritual.requirement.max", label, maxInt));
        }
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
