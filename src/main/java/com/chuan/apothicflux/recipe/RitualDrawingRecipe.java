package com.chuan.apothicflux.recipe;

import com.chuan.apothicflux.registry.ModRegistry;
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

public record RitualDrawingRecipe(Ingredient tool, boolean consumeItem, int durabilityCost) implements Recipe<RecipeInput> {

    public static final MapCodec<RitualDrawingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.fieldOf("tool").forGetter(RitualDrawingRecipe::tool),
            Codec.BOOL.fieldOf("consume_item").forGetter(RitualDrawingRecipe::consumeItem),
            Codec.INT.fieldOf("consume_durability").forGetter(RitualDrawingRecipe::durabilityCost)
    ).apply(inst, RitualDrawingRecipe::new));

    // 使用 composite 显式指定返回类型为 RitualDrawingRecipe
    public static final StreamCodec<RegistryFriendlyByteBuf, RitualDrawingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, RitualDrawingRecipe::tool,
            ByteBufCodecs.BOOL, RitualDrawingRecipe::consumeItem,
            ByteBufCodecs.VAR_INT, RitualDrawingRecipe::durabilityCost,
            RitualDrawingRecipe::new
    );

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRegistry.DRAWING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRegistry.DRAWING_TYPE.get(); }
}
