package com.chuan.apothicflux.jei;

import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record FluxSpawnerModifierView(
        Ingredient mainhand,
        Ingredient offhand,
        boolean hasOffhand,
        boolean consumesOffhand,
        List<FluxSpawnerModifierLine> lines
) {
}
