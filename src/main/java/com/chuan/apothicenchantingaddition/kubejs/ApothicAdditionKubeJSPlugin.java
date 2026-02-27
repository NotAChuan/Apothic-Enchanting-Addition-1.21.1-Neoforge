package com.chuan.apothicenchantingaddition.kubejs;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import net.minecraft.resources.ResourceLocation;

public class ApothicAdditionKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry event) {
        event.namespace(ModRegistry.MOD_ID).register(
                "ritual",
                RitualRecipeSchema.SCHEMA
        );
        event.namespace(ModRegistry.MOD_ID).register(
                "drawing",
                RitualDrawingRecipeSchema.SCHEMA
        );
    }
}
