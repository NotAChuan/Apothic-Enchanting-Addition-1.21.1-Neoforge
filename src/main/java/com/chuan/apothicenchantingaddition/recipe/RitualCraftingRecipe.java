package com.chuan.apothicenchantingaddition.recipe;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
        int craftTime
) implements Recipe<RecipeInput> {

    public static final MapCodec<RitualCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(RitualCraftingRecipe::inputs),
            ItemStack.CODEC.optionalFieldOf("output_item", ItemStack.EMPTY).forGetter(RitualCraftingRecipe::outputItem),
            Codec.STRING.optionalFieldOf("output_fluid").forGetter(RitualCraftingRecipe::outputFluid),
            Codec.STRING.optionalFieldOf("output_entity").forGetter(RitualCraftingRecipe::outputEntity),
            Codec.INT.fieldOf("craft_time").forGetter(RitualCraftingRecipe::craftTime)
    ).apply(inst, RitualCraftingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RitualCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), RitualCraftingRecipe::inputs,
            ItemStack.OPTIONAL_STREAM_CODEC, RitualCraftingRecipe::outputItem,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), RitualCraftingRecipe::outputFluid,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), RitualCraftingRecipe::outputEntity,
            ByteBufCodecs.VAR_INT, RitualCraftingRecipe::craftTime,
            RitualCraftingRecipe::new
    );

    @Override
    public boolean matches(RecipeInput input, Level level) {
        // 如果配方本身没有任何输入要求，直接拒绝
        if (inputs.isEmpty() || input.size() == 0) {
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

    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return outputItem.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return outputItem; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRegistry.RITUAL_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRegistry.RITUAL_TYPE.get(); }
}