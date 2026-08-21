package com.chuan.apothicenchantingaddition.util;

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

    private static boolean isOccultEnderLead(ItemStack stack) {
        return OCCULT_ENDER_LEAD.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
