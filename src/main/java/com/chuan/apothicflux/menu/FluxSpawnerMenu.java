package com.chuan.apothicflux.menu;

import com.chuan.apothicflux.block.entity.FluxSpawnerBlockEntity;
import com.chuan.apothicflux.registry.ModRegistry;
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

public class FluxSpawnerMenu extends AbstractContainerMenu {
    public final FluxSpawnerBlockEntity blockEntity;
    private final ContainerData data;

    // 客户端调用的构造函数
    public FluxSpawnerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    // 服务端调用的构造函数
    public FluxSpawnerMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModRegistry.FLUX_SPAWNER_MENU.get(), containerId);
        checkContainerSize(playerInventory, 36);
        this.blockEntity = (FluxSpawnerBlockEntity) entity;
        this.data = data;

        // 绑定数据槽 (用于同步电量和5个属性)
        this.addDataSlots(data);

        // 1. 刷怪笼的 8 个输入槽
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int index = col + row * 4; // 0 ~ 7
                this.addSlot(new SlotItemHandler(this.blockEntity.inventory, index, 53 + col * 18, 22 + row * 18) {
                    @Override
                    public int getMaxStackSize() {
                        return 1; // 核心限制：GUI 中强制每个槽位堆叠上限为 1
                    }

                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return FluxSpawnerMenu.this.blockEntity.canInsertSpawnerInput(stack);
                    }
                });
            }
        }

        // 2. 玩家背包 (Inventory)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 3. 玩家快捷栏 (Hotbar)
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    // 提供给客户端读取同步数据的方法
    public int getEnergy() {
        return (this.data.get(0) << 16) | this.data.get(1);
    }
    public int getMinDelay() { return this.data.get(2); }
    public int getMaxDelay() { return this.data.get(3); }
    public int getSpawnCount() { return this.data.get(4); }
    public boolean isRedstoneControl() { return this.data.get(5) != 0; }
    public int getEchoing() { return this.data.get(6); }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // 如果点击的是刷怪笼内的槽位 (0-7)，则移动到玩家背包 (8-43)
            if (index < 8) {
                if (!this.moveItemStackTo(stackInSlot, 8, 44, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 如果点击的是玩家背包的物品，尝试移动到刷怪笼的输入槽 (0-7)
            else {
                if (this.blockEntity.canInsertSpawnerInput(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 8, false)) {
                        return ItemStack.EMPTY;
                    }
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

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()), player, ModRegistry.FLUX_SPAWNER.get());
    }
}
