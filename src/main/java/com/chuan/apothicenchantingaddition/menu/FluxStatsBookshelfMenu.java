package com.chuan.apothicenchantingaddition.menu;

import com.chuan.apothicenchantingaddition.block.entity.FluxStatsBookshelfBlockEntity;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class FluxStatsBookshelfMenu extends AbstractContainerMenu {

    private final FluxStatsBookshelfBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    private final DataSlot eternaSlot = DataSlot.standalone();
    private final DataSlot quantaSlot = DataSlot.standalone();
    private final DataSlot arcanaSlot = DataSlot.standalone();
    private final DataSlot cluesSlot = DataSlot.standalone();
    private final DataSlot treasureSlot = DataSlot.standalone();
    private final DataSlot stableSlot = DataSlot.standalone();

    private final DataSlot energyUpper = DataSlot.standalone();
    private final DataSlot energyLower = DataSlot.standalone();

    public FluxStatsBookshelfMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (FluxStatsBookshelfBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FluxStatsBookshelfMenu(int containerId, Inventory inv, FluxStatsBookshelfBlockEntity entity) {
        super(ModRegistry.STATS_BOOKSHELF_MENU.get(), containerId);
        this.blockEntity = entity;
        this.levelAccess = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());

        if (this.blockEntity != null) {
            this.eternaSlot.set((int) (this.blockEntity.getRawEterna() * 10));
            this.quantaSlot.set((int) (this.blockEntity.getRawQuanta() * 10));
            this.arcanaSlot.set((int) (this.blockEntity.getRawArcana() * 10));
            this.cluesSlot.set(this.blockEntity.getRawClues());
            this.treasureSlot.set(this.blockEntity.getRawTreasure() ? 1 : 0);
            this.stableSlot.set(this.blockEntity.getRawStable() ? 1 : 0);

            int energy = this.blockEntity.energyStorage.getEnergyStored();
            this.energyUpper.set(energy >> 16);
            this.energyLower.set(energy & 0xFFFF);
        }

        addDataSlot(eternaSlot);
        addDataSlot(quantaSlot);
        addDataSlot(arcanaSlot);
        addDataSlot(cluesSlot);
        addDataSlot(treasureSlot);
        addDataSlot(stableSlot);
        addDataSlot(energyUpper);
        addDataSlot(energyLower);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null) {
            eternaSlot.set((int) (blockEntity.getRawEterna() * 10));
            quantaSlot.set((int) (blockEntity.getRawQuanta() * 10));
            arcanaSlot.set((int) (blockEntity.getRawArcana() * 10));
            cluesSlot.set(blockEntity.getRawClues());
            treasureSlot.set(blockEntity.getRawTreasure() ? 1 : 0);
            stableSlot.set(blockEntity.getRawStable() ? 1 : 0);

            int energy = blockEntity.energyStorage.getEnergyStored();
            energyUpper.set(energy >> 16);
            energyLower.set(energy & 0xFFFF);
        }
    }

    // 【修改获取方法】将放大的数值缩小回原本的 float
    public float getEterna() { return eternaSlot.get() / 10.0f; }
    public float getQuanta() { return quantaSlot.get() / 10.0f; }
    public float getArcana() { return arcanaSlot.get() / 10.0f; }
    public int getClues() { return cluesSlot.get(); }
    public boolean allowsTreasure() { return treasureSlot.get() == 1; }
    public boolean isStable() { return stableSlot.get() == 1; }

    // 【修改获取方法】将高低位拼回真实能量
    public int getEnergy() { return (energyUpper.get() << 16) | (energyLower.get() & 0xFFFF); }

    public FluxStatsBookshelfBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
