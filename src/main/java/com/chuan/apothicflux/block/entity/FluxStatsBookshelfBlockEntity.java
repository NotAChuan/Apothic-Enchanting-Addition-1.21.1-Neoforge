package com.chuan.apothicflux.block.entity;

import com.chuan.apothicflux.block.FluxStatsBookshelfBlock;
import com.chuan.apothicflux.block.Tier;
import com.chuan.apothicflux.config.ApothicAdditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public class FluxStatsBookshelfBlockEntity extends BlockEntity {

    private int eterna = 0;
    private int quanta = 0;
    private int arcana = 0;
    private int clues = 0;
    private boolean allowsTreasure = false;
    private boolean stable = false;

    private boolean isActive = false;

    // 缓存上一次的能量等级，用于优化 BlockState 更新
    private int previousEnergyLevel = 1;

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

    public FluxStatsBookshelfBlockEntity(BlockPos pos, BlockState state) {
        super(com.chuan.apothicflux.registry.ModRegistry.STATS_BOOKSHELF_BE.get(), pos, state);
    }

    public Tier getTier() {
        if (this.getBlockState().getBlock() instanceof FluxStatsBookshelfBlock block) {
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

        // ========== 新增：检测能量等级变化并更新 BlockState ==========
        int currentEnergy = energyStorage.getEnergyStored();
        int newEnergyLevel = calculateEnergyLevel(currentEnergy);

        if (newEnergyLevel != this.previousEnergyLevel) {
            this.previousEnergyLevel = newEnergyLevel;
            BlockState currentState = level.getBlockState(pos);
            level.setBlock(pos, currentState.setValue(FluxStatsBookshelfBlock.ENERGY_LEVEL, newEnergyLevel), 3);
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

    public void setEnergyStored(int energy) {
        int oldEnergy = this.energyStorage.getEnergyStored();

        // 直接修改 EnergyStorage 的内部能量值（因为没有 setEnergy 方法）
        // 先提取所有能量，再充入目标能量
        this.energyStorage.extractEnergy(this.energyStorage.getMaxEnergyStored(), false);
        this.energyStorage.receiveEnergy(energy, false);

        // 计算当前能量等级（1-4）
        int newEnergyLevel = calculateEnergyLevel(energy);

        // 只在能量等级跨越阈值时更新 BlockState（性能优化）
        if (newEnergyLevel != this.previousEnergyLevel) {
            this.previousEnergyLevel = newEnergyLevel;

            if (level != null && !level.isClientSide) {
                BlockState state = level.getBlockState(worldPosition);
                level.setBlock(worldPosition, state.setValue(FluxStatsBookshelfBlock.ENERGY_LEVEL, newEnergyLevel), 3);
            }
        }

        // 检查激活状态变化
        boolean wasActive = this.isActive;
        this.isActive = energy >= getEnergyCost();

        if (wasActive != this.isActive) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                notifyEnchantingTables();
            }
        }

        if (oldEnergy != energy) {
            setChanged();
        }
    }

    private int calculateEnergyLevel(int energy) {
        int maxEnergy = this.energyStorage.getMaxEnergyStored();
        if (energy == 0) return 1;
        if (energy >= maxEnergy * 0.66) return 4; // 66%-100%
        if (energy >= maxEnergy * 0.33) return 3; // 33%-66%
        return 2; // 1%-33%
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

    public int getRawEterna() {
        return eterna;
    }

    public int getRawQuanta() {
        return quanta;
    }

    public int getRawArcana() {
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

    public void setStats(int eterna, int quanta, int arcana, int clues, boolean allowsTreasure, boolean stable) {
        Tier currentTier = getTier();
        this.eterna = Math.min(Math.max(eterna, 0), Math.round(currentTier.getMaxEterna()));
        this.quanta = Math.min(Math.max(quanta, Tier.MIN_QUANTA), Math.round(currentTier.getMaxQuanta()));
        this.arcana = Math.min(Math.max(arcana, 0), Math.round(currentTier.getMaxArcana()));
        this.clues = Math.min(Math.max(clues, 0), currentTier.getMaxClues());
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
        tag.putInt("Eterna", eterna);
        tag.putInt("Quanta", quanta);
        tag.putInt("Arcana", arcana);
        tag.putInt("Clues", clues);
        tag.putBoolean("Treasure", allowsTreasure);
        tag.putBoolean("Stable", stable);
        tag.putBoolean("IsActive", isActive);
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.eterna = tag.contains("Eterna", net.minecraft.nbt.Tag.TAG_INT) ? tag.getInt("Eterna") : Math.round(tag.getFloat("Eterna"));
        this.quanta = tag.contains("Quanta", net.minecraft.nbt.Tag.TAG_INT) ? tag.getInt("Quanta") : Math.round(tag.getFloat("Quanta"));
        this.arcana = tag.contains("Arcana", net.minecraft.nbt.Tag.TAG_INT) ? tag.getInt("Arcana") : Math.round(tag.getFloat("Arcana"));
        this.clues = tag.getInt("Clues");
        this.allowsTreasure = tag.getBoolean("Treasure");
        this.stable = tag.getBoolean("Stable");
        this.isActive = tag.getBoolean("IsActive");
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
        this.previousEnergyLevel = calculateEnergyLevel(this.energyStorage.getEnergyStored());
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
