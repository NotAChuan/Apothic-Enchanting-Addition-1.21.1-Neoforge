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
            this.eternaSlot.set(this.blockEntity.getRawEterna());
            this.quantaSlot.set(this.blockEntity.getRawQuanta());
            this.arcanaSlot.set(this.blockEntity.getRawArcana());
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
            eternaSlot.set(blockEntity.getRawEterna());
            quantaSlot.set(blockEntity.getRawQuanta());
            arcanaSlot.set(blockEntity.getRawArcana());
            cluesSlot.set(blockEntity.getRawClues());
            treasureSlot.set(blockEntity.getRawTreasure() ? 1 : 0);
            stableSlot.set(blockEntity.getRawStable() ? 1 : 0);

            int energy = blockEntity.energyStorage.getEnergyStored();
            energyUpper.set(energy >> 16);
            energyLower.set(energy & 0xFFFF);
        }
    }

    // 直接同步整数属性
    public int getEterna() { return eternaSlot.get(); }
    public int getQuanta() { return quantaSlot.get(); }
    public int getArcana() { return arcanaSlot.get(); }
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
