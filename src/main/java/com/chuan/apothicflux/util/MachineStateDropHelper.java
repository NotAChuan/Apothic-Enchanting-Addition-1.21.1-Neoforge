package com.chuan.apothicflux.util;

import com.chuan.apothicflux.block.entity.FluxAnvilBlockEntity;
import com.chuan.apothicflux.block.entity.FluxEnchantingTableBlockEntity;
import com.chuan.apothicflux.block.entity.FluxSpawnerBlockEntity;
import com.chuan.apothicflux.block.entity.FluxStatsBookshelfBlockEntity;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * 只保存需要跨挖掘保留的最小状态：
 * - 所有机器统一保留 Energy
 * - 通量刷怪笼额外保留升级参数
 *
 * 不保存内部物品，避免重复掉落与额外序列化开销。
 */
public final class MachineStateDropHelper {

    private MachineStateDropHelper() {}

    public static ItemStack createFluxAnvilDrop(HolderLookup.Provider registries, FluxAnvilBlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModRegistry.FLUX_ANVIL.get());
        int energy = blockEntity.getEnergyStorage().getEnergyStored();
        if (energy <= 0) return stack;

        CompoundTag tag = new CompoundTag();
        tag.put("Energy", blockEntity.getEnergyStorage().serializeNBT(registries));
        BlockItem.setBlockEntityData(stack, ModRegistry.FLUX_ANVIL_BE.get(), tag);
        return stack;
    }

    public static ItemStack createFluxEnchantingTableDrop(HolderLookup.Provider registries, FluxEnchantingTableBlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModRegistry.FLUX_ENCHANTING_TABLE.get());
        int energy = blockEntity.energyStorage.getEnergyStored();
        if (energy <= 0) return stack;

        CompoundTag tag = new CompoundTag();
        tag.put("Energy", blockEntity.energyStorage.serializeNBT(registries));
        BlockItem.setBlockEntityData(stack, ModRegistry.FLUX_ENCHANTING_TABLE_BE.get(), tag);
        return stack;
    }

    public static ItemStack createFluxBookshelfDrop(HolderLookup.Provider registries, FluxStatsBookshelfBlockEntity blockEntity) {
        ItemStack stack = blockEntity.getBlockState().getBlock().asItem().getDefaultInstance();
        int energy = blockEntity.energyStorage.getEnergyStored();
        if (energy <= 0) return stack;

        CompoundTag tag = new CompoundTag();
        tag.put("Energy", blockEntity.energyStorage.serializeNBT(registries));
        BlockItem.setBlockEntityData(stack, ModRegistry.STATS_BOOKSHELF_BE.get(), tag);
        return stack;
    }

    public static ItemStack createFluxSpawnerDrop(HolderLookup.Provider registries, FluxSpawnerBlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModRegistry.FLUX_SPAWNER.get());
        int energy = blockEntity.energyStorage.getEnergyStored();
        boolean hasUpgradeState = blockEntity.getMinDelay() != 200
                || blockEntity.getMaxDelay() != 800
                || blockEntity.getSpawnCount() != 4
                || blockEntity.isRedstoneControl()
                || blockEntity.getEchoing() != 0;

        if (energy <= 0 && !hasUpgradeState) return stack;

        CompoundTag tag = new CompoundTag();
        if (energy > 0) {
            tag.put("Energy", blockEntity.energyStorage.serializeNBT(registries));
        }
        tag.putInt("MinDelay", blockEntity.getMinDelay());
        tag.putInt("MaxDelay", blockEntity.getMaxDelay());
        tag.putInt("SpawnCount", blockEntity.getSpawnCount());
        tag.putBoolean("RedstoneControl", blockEntity.isRedstoneControl());
        tag.putInt("Echoing", blockEntity.getEchoing());

        BlockItem.setBlockEntityData(stack, ModRegistry.FLUX_SPAWNER_BE.get(), tag);
        return stack;
    }
}
