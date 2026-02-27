package com.chuan.apothicenchantingaddition.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public interface RitualDrawingRecipeSchema {

    // 激活工具
    RecipeKey<SizedIngredient> TOOL = SizedIngredientComponent.FLAT
            .key("tool", ComponentRole.INPUT);

    // 是否消耗物品（可选，默认false）
    RecipeKey<Boolean> CONSUME_ITEM = BooleanComponent.BOOLEAN
            .key("consume_item", ComponentRole.OTHER)
            .optional(false);

    // 耐久消耗（可选，默认0）
    RecipeKey<Integer> DURABILITY_COST = NumberComponent.INT
            .key("consume_durability", ComponentRole.OTHER)
            .optional(0);

    RecipeSchema SCHEMA = new RecipeSchema(TOOL, CONSUME_ITEM, DURABILITY_COST);
}
