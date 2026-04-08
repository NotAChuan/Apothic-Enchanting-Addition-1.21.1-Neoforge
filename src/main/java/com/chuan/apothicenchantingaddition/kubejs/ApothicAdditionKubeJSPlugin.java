package com.chuan.apothicenchantingaddition.kubejs;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;

public class ApothicAdditionKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.namespace(ModRegistry.MOD_ID)
                .register("ritual", RitualRecipeSchema.SCHEMA);
        registry.namespace(ModRegistry.MOD_ID)
                .register("drawing", RitualDrawingRecipeSchema.SCHEMA);
        registry.namespace(ModRegistry.MOD_ID)
                .register("spawner", SpawnerRecipeSchema.SCHEMA);
        registry.namespace(ModRegistry.MOD_ID)
                .register("spawner_remove", SpawnerRemoveRecipeSchema.SCHEMA);
    }
}
