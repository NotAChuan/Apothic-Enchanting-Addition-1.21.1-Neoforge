package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FluxEnchantingTableBlockEntity extends BlockEntity {

    // 10 亿容量，无限制输入速率
    public final EnergyStorage energyStorage = new EnergyStorage(1000000000, Integer.MAX_VALUE, Integer.MAX_VALUE) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int ret = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && ret > 0) setChanged();
            return ret;
        }

        // 补上提取能量时的更新标记
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int ret = super.extractEnergy(maxExtract, simulate);
            if (!simulate && ret > 0) setChanged();
            return ret;
        }
    };

    // 槽位 0：附魔物品位，槽位 1：刷新耗材位
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0) {
                return 1; // 槽位 0 (附魔槽) 强制只能放 1 个物品
            }
            return super.getSlotLimit(slot); // 槽位 1 (青金石等耗材) 保持默认的 64 个上限
        }
    };

    public FluxEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FLUX_ENCHANTING_TABLE_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        int tickCost = ApothicAdditionConfig.FLUX_ENCHANTER_TICK_COST.get();
        if (tickCost > 0 && energyStorage.getEnergyStored() >= tickCost) {
            energyStorage.extractEnergy(tickCost, false);
            // 这里我们不需要像书架那样发送状态包，因为断电只需在菜单里表现为按钮变灰即可
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, (CompoundTag) tag.get("Inventory"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
