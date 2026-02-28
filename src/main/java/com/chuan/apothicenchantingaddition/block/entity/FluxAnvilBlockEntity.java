package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.menu.FluxAnvilMenu;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;

public class FluxAnvilBlockEntity extends BlockEntity implements MenuProvider {

    // 10 亿缓存，无上限输入速率，0 输出速率 (机器只能吃电不能发电)
    public static final int MAX_ENERGY = 1_000_000_000;
    public static final int MAX_RECEIVE = Integer.MAX_VALUE;

    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, MAX_RECEIVE, Integer.MAX_VALUE) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) setChanged();
            return extracted;
        }
    };

    public FluxAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FLUX_ANVIL_BE.get(), pos, state);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // --- 1.21.1 NBT 数据保存与加载 ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // NeoForge 1.21.1 的能量 NBT 序列化方式
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
    }

    // --- MenuProvider 接口实现 ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apothicenchantingaddition.flux_anvil");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 我们将 BlockEntity 作为参数传给 Menu，方便在合成时直接扣除这里的电量
        return new FluxAnvilMenu(containerId, playerInventory, this.getBlockPos());
    }
}
