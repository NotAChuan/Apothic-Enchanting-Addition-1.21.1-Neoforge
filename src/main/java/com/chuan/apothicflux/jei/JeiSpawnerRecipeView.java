package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.recipe.SpawnerRecipe;
import net.minecraft.world.item.ItemStack;

public record JeiSpawnerRecipeView(ItemStack spawnEgg, SpawnerRecipe recipe) {
}
