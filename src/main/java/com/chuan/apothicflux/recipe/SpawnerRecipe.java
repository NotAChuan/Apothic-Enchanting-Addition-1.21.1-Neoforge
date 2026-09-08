package com.chuan.apothicflux.recipe;

import com.chuan.apothicflux.registry.ModRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

import java.util.ArrayList;
import java.util.List;

public record SpawnerRecipe(ResourceLocation entity, List<SpawnerDrop> drops) implements Recipe<RecipeInput> {

    public static final MapCodec<SpawnerRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("entity").forGetter(SpawnerRecipe::entity),
            ConditionalOps.decodeListWithElementConditions(SpawnerDrop.CODEC.codec())
                    .optionalFieldOf("drops", List.of())
                    .forGetter(SpawnerRecipe::drops)
    ).apply(inst, SpawnerRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SpawnerRecipe decode(RegistryFriendlyByteBuf buf) {
            ResourceLocation entity = buf.readResourceLocation();
            int size = buf.readVarInt();
            List<SpawnerDrop> drops = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                drops.add(SpawnerDrop.STREAM_CODEC.decode(buf));
            }
            return new SpawnerRecipe(entity, drops);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SpawnerRecipe recipe) {
            buf.writeResourceLocation(recipe.entity());
            buf.writeVarInt(recipe.drops().size());
            for (SpawnerDrop drop : recipe.drops()) {
                SpawnerDrop.STREAM_CODEC.encode(buf, drop);
            }
        }
    };

    public List<ItemStack> rollDrops(RandomSource random) {
        List<ItemStack> rolled = new ArrayList<>();
        for (SpawnerDrop drop : this.drops) {
            if (drop.shouldDrop(random)) {
                rolled.add(drop.stack().copy());
            }
        }
        return rolled;
    }

    public boolean hasDrops() {
        return !this.drops.isEmpty();
    }

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
        return ModRegistry.SPAWNER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRegistry.SPAWNER_TYPE.get();
    }

    public record SpawnerDrop(ItemStack stack, float chance) {
        public static final MapCodec<SpawnerDrop> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(SpawnerDrop::stack),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(SpawnerDrop::chance)
        ).apply(inst, SpawnerDrop::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerDrop> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SpawnerDrop decode(RegistryFriendlyByteBuf buf) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                float chance = buf.readFloat();
                return new SpawnerDrop(stack, chance);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SpawnerDrop drop) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, drop.stack());
                buf.writeFloat(drop.chance());
            }
        };

        public boolean shouldDrop(RandomSource random) {
            if (this.stack.isEmpty()) {
                return false;
            }
            if (this.chance >= 1.0F) {
                return true;
            }
            if (this.chance <= 0.0F) {
                return false;
            }
            return random.nextFloat() < this.chance;
        }
    }
}
