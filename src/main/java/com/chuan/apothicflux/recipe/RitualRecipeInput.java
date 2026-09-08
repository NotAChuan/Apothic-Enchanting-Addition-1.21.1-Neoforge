package com.chuan.apothicflux.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Recipe input for the ritual: the 17 item slots plus the currently gathered
 * enchanting stats.
 */
public final class RitualRecipeInput implements RecipeInput {

    private final ItemStackHandler inventory;
    private final RitualStats stats;

    public RitualRecipeInput(ItemStackHandler inventory, RitualStats stats) {
        this.inventory = inventory;
        this.stats = stats;
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.getStackInSlot(index);
    }

    @Override
    public int size() {
        return inventory.getSlots();
    }

    public RitualStats stats() {
        return stats;
    }
}
