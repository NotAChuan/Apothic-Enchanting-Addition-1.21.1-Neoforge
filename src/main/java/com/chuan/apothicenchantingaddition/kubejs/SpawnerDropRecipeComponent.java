package com.chuan.apothicenchantingaddition.kubejs;

import com.chuan.apothicenchantingaddition.recipe.SpawnerRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;

public final class SpawnerDropRecipeComponent implements RecipeComponent<SpawnerRecipe.SpawnerDrop> {

    public static final SpawnerDropRecipeComponent INSTANCE = new SpawnerDropRecipeComponent();
    public static final RecipeComponentType TYPE = RecipeComponentType.unit(
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "spawner_drop"),
            INSTANCE
    );

    private SpawnerDropRecipeComponent() {
    }

    @Override
    public RecipeComponentType type() {
        return TYPE;
    }

    @Override
    public Codec<SpawnerRecipe.SpawnerDrop> codec() {
        return SpawnerRecipe.SpawnerDrop.CODEC.codec();
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(SpawnerRecipe.SpawnerDrop.class);
    }

    @Override
    public boolean isEmpty(SpawnerRecipe.SpawnerDrop value) {
        return value == null || value.stack().isEmpty();
    }
}
