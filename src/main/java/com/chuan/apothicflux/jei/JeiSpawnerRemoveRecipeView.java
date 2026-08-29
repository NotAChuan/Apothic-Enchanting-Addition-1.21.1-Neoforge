package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.recipe.SpawnerRemoveRecipe;
import net.minecraft.world.item.ItemStack;

public record JeiSpawnerRemoveRecipeView(ItemStack spawnEgg, SpawnerRemoveRecipe recipe) {
}
