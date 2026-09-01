package com.chuan.apothicflux.block.entity;

import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.block.FluxEnchantingTableBlock;
import com.chuan.apothicflux.util.BookAnimationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
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
        if (level.isClientSide) {
            bookAnimationTick(level, pos);
            return;
        }

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

            // 同步方块的电量等级纹理 (0 -> 没电, 1 -> 25%, 2 -> 50%, 3 -> 75%, 4 -> 100%)
            int newLevel = calculateEnergyLevel(currentEnergy, energyStorage.getMaxEnergyStored());
            BlockState currentState = level.getBlockState(pos);
            if (currentState.hasProperty(FluxEnchantingTableBlock.ENERGY_LEVEL)
                    && currentState.getValue(FluxEnchantingTableBlock.ENERGY_LEVEL) != newLevel) {
                level.setBlock(pos, currentState.setValue(FluxEnchantingTableBlock.ENERGY_LEVEL, newLevel), 3);
            }
        }

    }

    private static int calculateEnergyLevel(int energy, int maxEnergy) {
        if (maxEnergy <= 0 || energy <= 0) {
            return 0;
        }
        // 按四分之一电量取整：0 -> 0, 25% -> 1, 50% -> 2, 75% -> 3, 100% -> 4
        long level = ((long) energy * 4 + maxEnergy - 1) / maxEnergy;
        return (int) Math.min(4, Math.max(0, level));
    }

    private void bookAnimationTick(Level level, BlockPos pos) {
        this.oOpen = this.open;
        this.oRot = this.rot;

        Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3.0D, false);
        if (player != null) {
            this.tRot = BookAnimationHelper.targetRotation(pos.getX() + 0.5D, pos.getZ() + 0.5D, player.getX(), player.getZ());
            if (this.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float previousFlipTarget = this.flipT;

                do {
                    this.flipT = this.flipT + (float) (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (previousFlipTarget == this.flipT);
            }
        } else {
            this.tRot += 0.02F;
        }

        this.open = BookAnimationHelper.nextOpen(this.open, player != null);
        this.rot = BookAnimationHelper.nextRotation(this.rot, this.tRot);
        this.time++;
        this.oFlip = this.flip;
        float flipDelta = (this.flipT - this.flip) * 0.4F;
        flipDelta = Mth.clamp(flipDelta, -0.2F, 0.2F);
        this.flipA = this.flipA + (flipDelta - this.flipA) * 0.9F;
        this.flip = this.flip + this.flipA;
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
