package com.chuan.apothicenchantingaddition.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface RitualRecipeSchema {

    RecipeKey<List<Ingredient>> INPUTS = ListRecipeComponent.create(
            IngredientComponent.INGREDIENT.instance(),
            true,
            false
    ).key("inputs", ComponentRole.INPUT);

    RecipeKey<ItemStack> OUTPUT_ITEM = ItemStackComponent.OPTIONAL_ITEM_STACK
            .key("output_item", ComponentRole.OUTPUT)
            .optional(ItemStack.EMPTY);

    // 改为 OPTIONAL_STRING，允许空字符串
    RecipeKey<String> OUTPUT_FLUID = StringComponent.OPTIONAL_STRING
            .key("output_fluid", ComponentRole.OTHER)
            .optional("");

    RecipeKey<String> OUTPUT_ENTITY = StringComponent.OPTIONAL_STRING
            .key("output_entity", ComponentRole.OTHER)
            .optional("");

    RecipeKey<Integer> CRAFT_TIME = NumberComponent.INT
            .key("craft_time", ComponentRole.OTHER)
            .optional(200);

    RecipeSchema SCHEMA = new RecipeSchema(INPUTS, OUTPUT_ITEM, OUTPUT_FLUID, OUTPUT_ENTITY, CRAFT_TIME);
}
