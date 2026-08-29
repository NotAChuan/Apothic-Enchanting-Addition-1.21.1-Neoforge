package com.chuan.apothicflux.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.function.RecipeFunctionInstance;
import dev.latvian.mods.kubejs.recipe.schema.function.ResolvedRecipeSchemaFunction;
import dev.latvian.mods.kubejs.util.IntBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface SpawnerRecipeSchema {

    RecipeKey<String> ENTITY = StringComponent.STRING
            .key("entity", ComponentRole.INPUT);

    RecipeComponent<?> DROP = RecipeComponent.builder(
            new CustomObjectRecipeComponent.Key("stack", ItemStackComponent.OPTIONAL_ITEM_STACK.instance()),
            new CustomObjectRecipeComponent.Key("chance", NumberComponent.FLOAT, true)
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    RecipeKey<List<?>> DROPS = ((ListRecipeComponent) ListRecipeComponent.create((RecipeComponent) DROP, true, false)
            .withBounds(IntBounds.OPTIONAL))
            .key("drops", ComponentRole.OUTPUT)
            .optional(List.of());

    RecipeSchema SCHEMA = new RecipeSchema(ENTITY, DROPS)
            .uniqueId(ENTITY)
            .function(new RecipeFunctionInstance("addItem", new AddItemFunction()));

    final class AddItemFunction implements ResolvedRecipeSchemaFunction {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public List<RecipeComponent<?>> arguments() {
            return (List) List.of(ItemStackComponent.OPTIONAL_ITEM_STACK.instance(), NumberComponent.FLOAT);
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void execute(RecipeScriptContext cx, List<Object> args) {
            var recipe = cx.recipe();
            var existing = recipe.getValue(DROPS);
            List list = existing == null ? new ArrayList<>() : new ArrayList<>(existing);

            Object stack = args.get(0);
            float chance = 1.0F;
            if (args.size() > 1 && args.get(1) instanceof Number number) {
                chance = number.floatValue();
            }

            var dropObject = Map.of(
                    "stack", stack,
                    "chance", chance
            );

            list.addAll((List) DROPS.component.wrap(cx, dropObject));
            recipe.setValue(DROPS, list);
        }
    }
}
