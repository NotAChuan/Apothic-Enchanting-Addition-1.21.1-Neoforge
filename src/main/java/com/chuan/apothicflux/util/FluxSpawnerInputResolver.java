package com.chuan.apothicflux.util;

import com.chuan.apothicflux.integration.productivebees.ProductiveBeesIntegration;
import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.Optional;

public final class FluxSpawnerInputResolver {

    private static final ResourceLocation OCCULT_ENDER_LEAD = ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "occult_ender_lead");

    private FluxSpawnerInputResolver() {
    }

    public static Optional<EntityType<?>> getEntityType(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        if (stack.getItem() instanceof SpawnEggItem egg) {
            return Optional.of(egg.getType(ItemStack.EMPTY));
        }

        if (isOccultEnderLead(stack)) {
            return Optional.ofNullable(stack.get(Ench.Components.LEASHED_ENTITY_TYPE));
        }

        return Optional.empty();
    }

    /**
     * Returns the Productive Bees bee type carried by a spawn egg.
     * This is only populated for Productive Bees spawn eggs and is intentionally
     * isolated from Productive Bees classes so the mod still loads without it.
     */
    public static Optional<ResourceLocation> getProductiveBeeType(ItemStack stack) {
        return ProductiveBeesIntegration.getBeeType(stack);
    }

    private static boolean isOccultEnderLead(ItemStack stack) {
        return OCCULT_ENDER_LEAD.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
