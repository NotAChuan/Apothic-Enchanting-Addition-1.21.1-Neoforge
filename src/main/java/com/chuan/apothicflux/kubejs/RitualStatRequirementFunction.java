package com.chuan.apothicflux.kubejs;

import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.schema.function.ResolvedRecipeSchemaFunction;
import dev.latvian.mods.kubejs.util.TinyMap;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.type.TypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder function for one ritual stat. Supports no arguments (no
 * requirement), a single integer (minimum), or a two-element integer list
 * (minimum and maximum).
 */
public final class RitualStatRequirementFunction implements ResolvedRecipeSchemaFunction {

    private static final RecipeComponent<Object> RAW_ARG = new RecipeComponent<>() {
        @Override
        public RecipeComponentType<?> type() {
            return null;
        }

        @Override
        public Codec<Object> codec() {
            return null;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.NONE;
        }

        @Override
        public String toString() {
            return "int | [int, int]";
        }
    };

    private final String stat;
    private final RecipeKey<TinyMap<String, Integer>> minKey;
    private final RecipeKey<TinyMap<String, Integer>> maxKey;
    private final RecipeKey<List<String>> callsKey;

    public RitualStatRequirementFunction(
            String stat,
            RecipeKey<TinyMap<String, Integer>> minKey,
            RecipeKey<TinyMap<String, Integer>> maxKey,
            RecipeKey<List<String>> callsKey
    ) {
        this.stat = stat;
        this.minKey = minKey;
        this.maxKey = maxKey;
        this.callsKey = callsKey;
    }

    @Override
    public List<RecipeComponent<?>> arguments() {
        return List.of(RAW_ARG);
    }

    @Override
    public void execute(RecipeScriptContext cx, List<Object> args) {
        KubeRecipe recipe = cx.recipe();

        if (args.isEmpty()) {
            apply(recipe, null, null);
            return;
        }

        if (args.size() != 1) {
            throw new IllegalArgumentException("Expected no argument, an integer, or [min, max]");
        }

        Context rhino = cx.cx();
        Object raw = args.get(0);

        if (raw instanceof NativeArray array) {
            if (array.getLength() != 2) {
                throw new IllegalArgumentException("Expected [min, max] with exactly two integers");
            }

            int min = parseInteger(rhino, array.get(0, rhino), "minimum");
            int max = parseInteger(rhino, array.get(1, rhino), "maximum");
            applyRange(recipe, min, max);
            return;
        }

        if (raw instanceof List<?> || raw instanceof Object[] || raw != null && raw.getClass().isArray()) {
            List<?> values = (List<?>) rhino.listOf(raw, TypeInfo.NONE);

            if (values.size() != 2) {
                throw new IllegalArgumentException("Expected [min, max] with exactly two integers");
            }

            int min = parseInteger(rhino, values.get(0), "minimum");
            int max = parseInteger(rhino, values.get(1), "maximum");
            applyRange(recipe, min, max);
            return;
        }

        int min = parseInteger(rhino, raw, "minimum");
        apply(recipe, min, null);
    }

    private void apply(KubeRecipe recipe, Integer min, Integer max) {
        RecipeComponentValue<?>[] holders = recipe.getRecipeComponentValues();
        RecipeComponentValue<?> minHolder = find(holders, minKey);
        RecipeComponentValue<?> maxHolder = find(holders, maxKey);
        RecipeComponentValue<?> callsHolder = find(holders, callsKey);

        List<String> calls = new ArrayList<>(readCalls(callsHolder));
        if (calls.contains(stat)) {
            throw new IllegalArgumentException("Stat '" + stat + "' has already been set");
        }

        calls.add(stat);
        writeCalls(callsHolder, calls);

        writeMap(minHolder, stat, min);
        writeMap(maxHolder, stat, max);
        recipe.save();
    }

    private void applyRange(KubeRecipe recipe, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum must not be greater than maximum for '" + stat + "'");
        }

        apply(recipe, min, max);
    }

    private static int parseInteger(Context cx, Object value, String label) {
        Object converted = cx.jsToJava(value, TypeInfo.DOUBLE);

        if (!(converted instanceof Number number)) {
            throw new IllegalArgumentException("Expected an integer for " + label);
        }

        double d = number.doubleValue();
        if (!Double.isFinite(d) || d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(label + " must be a finite integer");
        }

        return (int) d;
    }

    private static RecipeComponentValue<?> find(RecipeComponentValue<?>[] holders, RecipeKey<?> key) {
        for (RecipeComponentValue<?> holder : holders) {
            if (holder.key == key) {
                return holder;
            }
        }

        throw new IllegalStateException("Recipe key not found: " + key);
    }

    @SuppressWarnings("unchecked")
    private static List<String> readCalls(RecipeComponentValue<?> holder) {
        Object value = holder.value;
        return value instanceof List<?> list ? new ArrayList<>((List<String>) list) : new ArrayList<>();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writeCalls(RecipeComponentValue<?> holder, List<String> calls) {
        RecipeComponentValue rawHolder = holder;
        rawHolder.value = List.copyOf(calls);
        rawHolder.write = false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> readMap(RecipeComponentValue<?> holder) {
        Object value = holder.value;
        return value instanceof TinyMap<?, ?> map ? new HashMap<>(((TinyMap<String, Integer>) map).toMap()) : new HashMap<>();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writeMap(RecipeComponentValue<?> holder, String stat, Integer value) {
        Map<String, Integer> map = readMap(holder);

        if (value == null) {
            map.remove(stat);
        } else {
            map.put(stat, value);
        }

        boolean empty = map.isEmpty();
        RecipeComponentValue rawHolder = holder;
        rawHolder.value = empty ? TinyMap.ofMap(Map.of()) : TinyMap.ofMap(map);
        rawHolder.write = !empty;
    }
}
