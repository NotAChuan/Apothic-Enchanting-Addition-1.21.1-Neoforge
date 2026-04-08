package com.chuan.apothicenchantingaddition.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface SpawnerRemoveRecipeSchema {

    RecipeKey<String> ENTITY = StringComponent.STRING
            .key("entity", ComponentRole.INPUT);

    RecipeSchema SCHEMA = new RecipeSchema(ENTITY)
            .uniqueId(ENTITY);
}
