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

    // ✨ [新增] 能量快照，用于对比能量是否发生了实质性变化
    private int lastEnergy = -1;

    // ✨ [修复] 彻底去除了内部重写的 receiveEnergy 和 extractEnergy。
    // 不要让外部线缆输入或内部每 tick 消耗时疯狂触发 setChanged()。
    public final EnergyStorage energyStorage = new EnergyStorage(1000000000, Integer.MAX_VALUE, Integer.MAX_VALUE);

    // 槽位 0：附魔物品位，槽位 1：刷新耗材位
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            // 物品改变的频率极低（只有玩家手动拿放或自动化管道抽入），这里保留 setChanged() 是完全合理的
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0) {
                return 1;
            }
            return super.getSlotLimit(slot);
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
        }
        // 每秒（20 tick）集中检查一次。如果这 1 秒内（无论是因为每 tick 扣电，还是线缆输入）能量变了，才向硬盘汇报一次！
        if (level.getGameTime() % 20 == 0) {
            int currentEnergy = energyStorage.getEnergyStored();
            if (currentEnergy != lastEnergy) {
                setChanged();
                lastEnergy = currentEnergy;
            }
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
        // 初始化时同步一下能量快照
        this.lastEnergy = energyStorage.getEnergyStored();
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
