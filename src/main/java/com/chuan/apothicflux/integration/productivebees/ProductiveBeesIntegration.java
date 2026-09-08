package com.chuan.apothicflux.integration.productivebees;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Lightweight Productive Bees identification helpers.
 *
 * <p>This class intentionally has no compile-time references to Productive Bees
 * classes. It can therefore be loaded and called safely even when Productive
 * Bees is not installed.</p>
 */
public final class ProductiveBeesIntegration {

    public static final String MOD_ID = "productivebees";

    private static final ResourceLocation CONFIGURABLE_SPAWN_EGG =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "spawn_egg_configurable_bee");
    private static final String ENTITY_DATA_BEE_TYPE_KEY = "type";

    private ProductiveBeesIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isProductiveBeeSpawnEgg(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem)) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && MOD_ID.equals(itemId.getNamespace());
    }

    /**
     * Extracts the bee type that Productive Bees uses in its recipes.
     *
     * <p>Configurable bees store the type in {@code DataComponents.ENTITY_DATA}
     * under the {@code type} key. Non-configurable Productive Bees spawn eggs map
     * their recipe bee type to the entity ID, so the entity ID is returned for
     * those.</p>
     */
    public static Optional<ResourceLocation> getBeeType(ItemStack stack) {
        if (!isLoaded() || !isProductiveBeeSpawnEgg(stack)) {
            return Optional.empty();
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }

        if (CONFIGURABLE_SPAWN_EGG.equals(itemId)) {
            var entityData = stack.get(DataComponents.ENTITY_DATA);
            if (entityData != null) {
                CompoundTag tag = entityData.copyTag();
                ResourceLocation beeType = ResourceLocation.tryParse(tag.getString(ENTITY_DATA_BEE_TYPE_KEY));
                if (beeType != null) {
                    return Optional.of(beeType);
                }
            }
            return Optional.empty();
        }

        SpawnEggItem spawnEgg = (SpawnEggItem) stack.getItem();
        EntityType<?> entityType = spawnEgg.getType(stack);
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null && MOD_ID.equals(entityId.getNamespace())) {
            return Optional.of(entityId);
        }

        return Optional.empty();
    }
}
