package com.chuan.apothicflux.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.function.RecipeFunctionInstance;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.kubejs.util.TinyMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    RecipeKey<TinyMap<String, Integer>> REQUIREMENTS = MapRecipeComponent.of(
            StringComponent.STRING.instance(),
            NumberComponent.INT,
            IntBounds.of(0, 3)
    ).key("requirements", ComponentRole.OTHER)
            .optional(TinyMap.ofMap(Map.of()))
            .noFunctions()
            .exclude();

    RecipeKey<TinyMap<String, Integer>> MAX_REQUIREMENTS = MapRecipeComponent.of(
            StringComponent.STRING.instance(),
            NumberComponent.INT,
            IntBounds.of(0, 3)
    ).key("max_requirements", ComponentRole.OTHER)
            .optional(TinyMap.ofMap(Map.of()))
            .noFunctions()
            .exclude();

    RecipeKey<List<String>> STAT_CALLS = ListRecipeComponent.create(
            StringComponent.STRING.instance(),
            false,
            false,
            IntBounds.of(0, 3),
            Optional.empty()
    ).key("stat_calls", ComponentRole.OTHER)
            .optional(List.of())
            .noFunctions()
            .exclude();

    RecipeSchema SCHEMA = new RecipeSchema(
            INPUTS,
            OUTPUT_ITEM,
            OUTPUT_FLUID,
            OUTPUT_ENTITY,
            CRAFT_TIME,
            REQUIREMENTS,
            MAX_REQUIREMENTS,
            STAT_CALLS
    )
            .function(new RecipeFunctionInstance("eterna", new RitualStatRequirementFunction("eterna", REQUIREMENTS, MAX_REQUIREMENTS, STAT_CALLS)))
            .function(new RecipeFunctionInstance("quanta", new RitualStatRequirementFunction("quanta", REQUIREMENTS, MAX_REQUIREMENTS, STAT_CALLS)))
            .function(new RecipeFunctionInstance("arcana", new RitualStatRequirementFunction("arcana", REQUIREMENTS, MAX_REQUIREMENTS, STAT_CALLS)));
}
