package com.chuan.apothicflux.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.crafting.Ingredient;

public interface RitualDrawingRecipeSchema {

    RecipeKey<Ingredient> TOOL = IngredientComponent.INGREDIENT
            .key("tool", ComponentRole.INPUT);

    RecipeKey<Boolean> CONSUME_ITEM = BooleanComponent.BOOLEAN
            .key("consume_item", ComponentRole.OTHER)
            .optional(false);

    RecipeKey<Integer> DURABILITY_COST = NumberComponent.INT
            .key("consume_durability", ComponentRole.OTHER)
            .optional(0);

    // 不手动定义构造器，让 KubeJS 自动生成
    // 会自动生成 drawing(tool)、drawing(tool, consume_item)、drawing(tool, consume_item, consume_durability) 三种调用方式
    RecipeSchema SCHEMA = new RecipeSchema(TOOL, CONSUME_ITEM, DURABILITY_COST);
}
