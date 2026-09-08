package com.chuan.apothicflux.recipe;

import com.chuan.apothicflux.registry.ModRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RitualCraftingRecipe(
        List<Ingredient> inputs,
        ItemStack outputItem,
        Optional<String> outputFluid,
        Optional<String> outputEntity,
        int craftTime,
        RitualStatRequirement requirements,
        Optional<RitualStatRequirement> maxRequirements
) implements Recipe<RitualRecipeInput> {

    private static final Codec<RitualStatRequirement> MIN_REQUIREMENT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("eterna", RitualStatRequirement.NO_MIN).forGetter(RitualStatRequirement::eterna),
            Codec.FLOAT.optionalFieldOf("quanta", RitualStatRequirement.NO_MIN).forGetter(RitualStatRequirement::quanta),
            Codec.FLOAT.optionalFieldOf("arcana", RitualStatRequirement.NO_MIN).forGetter(RitualStatRequirement::arcana)
    ).apply(inst, RitualStatRequirement::new));

    private static final Codec<RitualStatRequirement> MAX_REQUIREMENT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("eterna", RitualStatRequirement.NO_MAX).forGetter(RitualStatRequirement::eterna),
            Codec.FLOAT.optionalFieldOf("quanta", RitualStatRequirement.NO_MAX).forGetter(RitualStatRequirement::quanta),
            Codec.FLOAT.optionalFieldOf("arcana", RitualStatRequirement.NO_MAX).forGetter(RitualStatRequirement::arcana)
    ).apply(inst, RitualStatRequirement::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, RitualStatRequirement> REQUIREMENT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RitualStatRequirement::eterna,
            ByteBufCodecs.FLOAT, RitualStatRequirement::quanta,
            ByteBufCodecs.FLOAT, RitualStatRequirement::arcana,
            RitualStatRequirement::new
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INPUTS_STREAM_CODEC =
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<ByteBuf, Optional<String>> OPTIONAL_STRING_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional);

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<RitualStatRequirement>> OPTIONAL_REQUIREMENT_STREAM_CODEC =
            ByteBufCodecs.optional(REQUIREMENT_STREAM_CODEC);

    public static final MapCodec<RitualCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(RitualCraftingRecipe::inputs),
            ItemStack.CODEC.optionalFieldOf("output_item", ItemStack.EMPTY).forGetter(RitualCraftingRecipe::outputItem),
            Codec.STRING.optionalFieldOf("output_fluid").forGetter(RitualCraftingRecipe::outputFluid),
            Codec.STRING.optionalFieldOf("output_entity").forGetter(RitualCraftingRecipe::outputEntity),
            Codec.INT.fieldOf("craft_time").forGetter(RitualCraftingRecipe::craftTime),
            MIN_REQUIREMENT_CODEC.optionalFieldOf("requirements", RitualStatRequirement.NONE).forGetter(RitualCraftingRecipe::requirements),
            MAX_REQUIREMENT_CODEC.optionalFieldOf("max_requirements").forGetter(RitualCraftingRecipe::maxRequirements)
    ).apply(inst, RitualCraftingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RitualCraftingRecipe> STREAM_CODEC =
            StreamCodec.of(RitualCraftingRecipe::write, RitualCraftingRecipe::read);

    private static void write(RegistryFriendlyByteBuf buffer, RitualCraftingRecipe recipe) {
        INPUTS_STREAM_CODEC.encode(buffer, recipe.inputs);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.outputItem);
        OPTIONAL_STRING_STREAM_CODEC.encode(buffer, recipe.outputFluid);
        OPTIONAL_STRING_STREAM_CODEC.encode(buffer, recipe.outputEntity);
        ByteBufCodecs.VAR_INT.encode(buffer, recipe.craftTime);
        REQUIREMENT_STREAM_CODEC.encode(buffer, recipe.requirements);
        OPTIONAL_REQUIREMENT_STREAM_CODEC.encode(buffer, recipe.maxRequirements);
    }

    private static RitualCraftingRecipe read(RegistryFriendlyByteBuf buffer) {
        List<Ingredient> inputs = INPUTS_STREAM_CODEC.decode(buffer);
        ItemStack outputItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        Optional<String> outputFluid = OPTIONAL_STRING_STREAM_CODEC.decode(buffer);
        Optional<String> outputEntity = OPTIONAL_STRING_STREAM_CODEC.decode(buffer);
        int craftTime = ByteBufCodecs.VAR_INT.decode(buffer);
        RitualStatRequirement requirements = REQUIREMENT_STREAM_CODEC.decode(buffer);
        Optional<RitualStatRequirement> maxRequirements = OPTIONAL_REQUIREMENT_STREAM_CODEC.decode(buffer);
        return new RitualCraftingRecipe(inputs, outputItem, outputFluid, outputEntity, craftTime, requirements, maxRequirements);
    }

    @Override
    public boolean matches(RitualRecipeInput input, Level level) {
        // 如果配方本身没有任何输入要求，直接拒绝
        if (inputs.isEmpty() || input.size() == 0) {
            return false;
        }

        if (!RitualStatRequirement.matches(input.stats(), requirements, maxRequirements)) {
            return false;
        }

        // 1. 核心规则：严格匹配第一个物品（法阵中心槽，即 input.getItem(0)）
        if (!inputs.get(0).test(input.getItem(0))) {
            return false;
        }

        // 2. 无序匹配剩余的外圈物品
        // 将配方中剩余的原料要求复制到一个新列表中，用于动态消除
        List<Ingredient> remainingIngredients = new ArrayList<>(inputs.subList(1, inputs.size()));

        // 遍历祭坛的剩余槽位（外圈槽位 1 到 size - 1）
        for (int i = 1; i < input.size(); i++) {
            ItemStack stackInSlot = input.getItem(i);

            // 如果外圈槽位有物品，尝试在剩余的配方原料中寻找匹配项
            if (!stackInSlot.isEmpty()) {
                boolean matched = false;
                for (int j = 0; j < remainingIngredients.size(); j++) {
                    if (remainingIngredients.get(j).test(stackInSlot)) {
                        remainingIngredients.remove(j); // 匹配成功，移除该配方需求
                        matched = true;
                        break;
                    }
                }
                // 防作弊检测：如果祭坛上放了配方不需要的多余物品，则匹配失败
                if (!matched) {
                    return false;
                }
            }
        }

        // 最终检查：如果 remainingIngredients 为空，说明所有辅助材料均已放齐，且没有多余物品
        return remainingIngredients.isEmpty();
    }

    @Override public ItemStack assemble(RitualRecipeInput input, HolderLookup.Provider registries) { return outputItem.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return outputItem; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRegistry.RITUAL_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRegistry.RITUAL_TYPE.get(); }
}
