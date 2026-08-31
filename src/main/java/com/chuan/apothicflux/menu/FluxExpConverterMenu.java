package com.chuan.apothicflux.menu;

import com.chuan.apothicflux.block.entity.FluxExpConverterBlockEntity;
import com.chuan.apothicflux.network.FluxExpConverterActionPayload;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.util.ExperienceMath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class FluxExpConverterMenu extends AbstractContainerMenu {
    private final FluxExpConverterBlockEntity blockEntity;
    private final ContainerData data;

    public FluxExpConverterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public FluxExpConverterMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModRegistry.FLUX_EXP_CONVERTER_MENU.get(), containerId);
        this.blockEntity = (FluxExpConverterBlockEntity) entity;
        this.data = data;
        this.addDataSlots(data);

        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 0, 80, 27) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return FluxExpConverterBlockEntity.isSupportedXpItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    public FluxExpConverterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getStoredXp() {
        long high = ((long) this.data.get(0)) << 32;
        long low = this.data.get(1) & 0xFFFFFFFFL;
        return high | low;
    }

    public int getStoredLevel() {
        return ExperienceMath.levelForXp(getStoredXp());
    }

    public void handleAction(Player player, int actionId) {
        if (blockEntity.getLevel().isClientSide) {
            return;
        }

        if (actionId >= 0 && actionId <= 5) {
            FluxExpConverterActionPayload.Action action = FluxExpConverterActionPayload.Action.values()[actionId];
            blockEntity.handleAction(player, action);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()), player, ModRegistry.FLUX_EXP_CONVERTER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (FluxExpConverterBlockEntity.isSupportedXpItem(stackInSlot)) {
                if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!this.moveItemStackTo(stackInSlot, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37) {
                if (!this.moveItemStackTo(stackInSlot, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }
}
