package com.chuan.apothicenchantingaddition.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public interface RitualRecipeSchema {

    // 输入物品列表：通过 ListRecipeComponent.create() 包装 FLAT 的 component 实例
    RecipeKey<List<SizedIngredient>> INPUTS = ListRecipeComponent.create(
            SizedIngredientComponent.FLAT.instance(),
            true,
            false
    ).key("inputs", ComponentRole.INPUT);

    // 输出物品（可选，默认空）
    RecipeKey<ItemStack> OUTPUT_ITEM = ItemStackComponent.OPTIONAL_ITEM_STACK
            .key("output_item", ComponentRole.OUTPUT)
            .optional(ItemStack.EMPTY);

    // 输出流体（可选字符串）
    RecipeKey<String> OUTPUT_FLUID = StringComponent.ID
            .key("output_fluid", ComponentRole.OTHER)
            .optional("");

    // 输出实体（可选字符串）
    RecipeKey<String> OUTPUT_ENTITY = StringComponent.ID
            .key("output_entity", ComponentRole.OTHER)
            .optional("");

    // 合成时间（tick，可选，默认200）
    RecipeKey<Integer> CRAFT_TIME = NumberComponent.INT
            .key("craft_time", ComponentRole.OTHER)
            .optional(200);

    RecipeSchema SCHEMA = new RecipeSchema(INPUTS, OUTPUT_ITEM, OUTPUT_FLUID, OUTPUT_ENTITY, CRAFT_TIME);
}
