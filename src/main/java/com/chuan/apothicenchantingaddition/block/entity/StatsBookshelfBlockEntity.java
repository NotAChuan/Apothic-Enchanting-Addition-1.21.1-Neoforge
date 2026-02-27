package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.block.Tier;
import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public class StatsBookshelfBlockEntity extends BlockEntity {

    private float eterna = 0;
    private float quanta = 0;
    private float arcana = 0;
    private int clues = 0;
    private boolean allowsTreasure = false;
    private boolean stable = false;

    private boolean isActive = false;

    public final EnergyStorage energyStorage = new EnergyStorage(1000000, Integer.MAX_VALUE, Integer.MAX_VALUE) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int ret = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && ret > 0) setChanged();
            return ret;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int ret = super.extractEnergy(maxExtract, simulate);
            if (!simulate && ret > 0) setChanged();
            return ret;
        }
    };

    public StatsBookshelfBlockEntity(BlockPos pos, BlockState state) {
        super(com.chuan.apothicenchantingaddition.registry.ModRegistry.STATS_BOOKSHELF_BE.get(), pos, state);
    }

    public Tier getTier() {
        if (this.getBlockState().getBlock() instanceof com.chuan.apothicenchantingaddition.block.StatsBookshelfBlock block) {
            return block.getTier();
        }
        return Tier.TIER_1;
    }

    private int getEnergyCost() {
        return switch (getTier()) {
            case TIER_1 -> ApothicAdditionConfig.TIER_1_ENERGY_COST.get();
            case TIER_2 -> ApothicAdditionConfig.TIER_2_ENERGY_COST.get();
            case TIER_3 -> ApothicAdditionConfig.TIER_3_ENERGY_COST.get();
            case TIER_4 -> ApothicAdditionConfig.TIER_4_ENERGY_COST.get();
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        int cost = getEnergyCost();
        // 只有在配置耗电量大于 0 时才扣电
        if (cost > 0 && energyStorage.getEnergyStored() >= cost) {
            energyStorage.extractEnergy(cost, false);
            if (!isActive) {
                isActive = true;
                setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                notifyEnchantingTables(); // 通电时刷新周围附魔台
            }
        } else if (cost == 0) {
            // 如果在配置文件中把耗电设置为 0，就永久激活
            if (!isActive) {
                isActive = true;
                setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                notifyEnchantingTables();
            }
        } else {
            if (isActive) {
                isActive = false; // 能量耗尽
                setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                notifyEnchantingTables(); // 断电时刷新周围附魔台
            }
        }
    }

    private void notifyEnchantingTables() {
        if (level == null) return;
        // 1. 通知相连的方块
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        // 2. 遍历传统书架的有效判定范围 (半径 2 格内)，强制附魔台刷新
        for (BlockPos offset : BlockPos.betweenClosed(worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2))) {
            BlockState s = level.getBlockState(offset);
            if (s.is(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE)) {
                level.sendBlockUpdated(offset, s, s, 3);
            }
        }
    }

    public float getEterna() {
        return isActive ? eterna : 0;
    }

    public float getQuanta() {
        return isActive ? quanta : 0;
    }

    public float getArcana() {
        return isActive ? arcana : 0;
    }

    public int getClues() {
        return isActive ? clues : 0;
    }

    public boolean allowsTreasure() {
        return isActive && allowsTreasure;
    }

    public boolean isStable() {
        return isActive && stable;
    }

    public float getRawEterna() {
        return eterna;
    }

    public float getRawQuanta() {
        return quanta;
    }

    public float getRawArcana() {
        return arcana;
    }

    public int getRawClues() {
        return clues;
    }

    public boolean getRawTreasure() {
        return allowsTreasure;
    }

    public boolean getRawStable() {
        return stable;
    }

    public void setStats(float eterna, float quanta, float arcana, int clues, boolean allowsTreasure, boolean stable) {
        Tier currentTier = getTier();
        this.eterna = Math.min(eterna, currentTier.getMaxEterna());
        this.quanta = Math.min(quanta, currentTier.getMaxQuanta());
        this.arcana = Math.min(arcana, currentTier.getMaxArcana());
        this.clues = Math.min(clues, currentTier.getMaxClues());
        this.allowsTreasure = allowsTreasure;
        this.stable = stable;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            notifyEnchantingTables(); // 【关键】玩家点击保存时，立刻刷新周围附魔台！
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Eterna", eterna);
        tag.putFloat("Quanta", quanta);
        tag.putFloat("Arcana", arcana);
        tag.putInt("Clues", clues);
        tag.putBoolean("Treasure", allowsTreasure);
        tag.putBoolean("Stable", stable);
        tag.putBoolean("IsActive", isActive);
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.eterna = tag.getFloat("Eterna");
        this.quanta = tag.getFloat("Quanta");
        this.arcana = tag.getFloat("Arcana");
        this.clues = tag.getInt("Clues");
        this.allowsTreasure = tag.getBoolean("Treasure");
        this.stable = tag.getBoolean("Stable");
        this.isActive = tag.getBoolean("IsActive");
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
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
