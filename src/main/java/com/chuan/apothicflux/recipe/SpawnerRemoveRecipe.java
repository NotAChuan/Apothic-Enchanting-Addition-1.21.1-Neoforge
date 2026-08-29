package com.chuan.apothicflux.recipe;

import com.chuan.apothicflux.registry.ModRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record SpawnerRemoveRecipe(ResourceLocation entity) implements Recipe<RecipeInput> {

    public static final MapCodec<SpawnerRemoveRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("entity").forGetter(SpawnerRemoveRecipe::entity)
    ).apply(inst, SpawnerRemoveRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerRemoveRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SpawnerRemoveRecipe decode(RegistryFriendlyByteBuf buf) {
            return new SpawnerRemoveRecipe(buf.readResourceLocation());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SpawnerRemoveRecipe recipe) {
            buf.writeResourceLocation(recipe.entity());
        }
    };

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.SPAWNER_REMOVE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRegistry.SPAWNER_REMOVE_TYPE.get();
    }
}
