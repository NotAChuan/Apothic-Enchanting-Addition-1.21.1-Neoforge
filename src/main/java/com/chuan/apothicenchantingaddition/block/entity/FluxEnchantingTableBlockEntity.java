package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FluxEnchantingTableBlockEntity extends BlockEntity {

    // 能量快照，用于对比能量是否发生了实质性变化
    private int lastEnergy = -1;

    // 书本动画字段
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    private static final RandomSource RANDOM = RandomSource.create();

    public final EnergyStorage energyStorage = new EnergyStorage(1000000000, Integer.MAX_VALUE, Integer.MAX_VALUE);

    // 槽位 0：附魔物品位，槽位 1：刷新耗材位
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
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

    // 修复：构造函数只需要两个参数
    public FluxEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FLUX_ENCHANTING_TABLE_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // 能量消耗
        int tickCost = ApothicAdditionConfig.FLUX_ENCHANTER_TICK_COST.get();
        if (tickCost > 0 && energyStorage.getEnergyStored() >= tickCost) {
            energyStorage.extractEnergy(tickCost, false);
        }

        // 每秒检查一次能量变化
        if (level.getGameTime() % 20 == 0) {
            int currentEnergy = energyStorage.getEnergyStored();
            if (currentEnergy != lastEnergy) {
                setChanged();
                lastEnergy = currentEnergy;
            }
        }

        // 书本动画更新（修复：使用 this 而不是 be）
        this.time++;
        this.oFlip = this.flip;
        this.oOpen = this.open;
        this.oRot = this.rot;

        // 书本翻页动画
        this.flipT += 0.1F;
        if (this.flipT > 1.0F) {
            this.flipT = 0.0F;
            this.flipA = RANDOM.nextFloat() * 0.4F + 0.8F;
        }

        float targetFlip = (this.flipT - this.flip) * 0.4F;
        this.flip += Mth.clamp(targetFlip, -0.2F, 0.2F);

        // 书本打开/关闭动画（固定为打开状态）
        this.open += (1.0F - this.open) * 0.1F;

        // 书本旋转动画
        this.tRot += 0.02F;
        this.rot += (this.tRot - this.rot) * 0.4F;
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

    public float getFlipAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.oFlip, this.flip);
    }

    public float getOpenAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.oOpen, this.open);
    }

    public float getRotationAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.oRot, this.rot);
    }
}
