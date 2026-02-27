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
            ItemStack.OPTIONAL_STREAM_CODEC, RitualCraftingRecipe::outputItem, // 【修改】这下类型完全匹配了
//            ItemStack.STREAM_CODEC, RitualCraftingRecipe::outputItem, // 【修改】这下类型完全匹配了
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), RitualCraftingRecipe::outputFluid,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), RitualCraftingRecipe::outputEntity,
            ByteBufCodecs.VAR_INT, RitualCraftingRecipe::craftTime,
            RitualCraftingRecipe::new
    );

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return outputItem.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return outputItem; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRegistry.RITUAL_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRegistry.RITUAL_TYPE.get(); }
}
