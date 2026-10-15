package com.chuan.apothicflux.menu;

import com.chuan.apothicflux.block.entity.FluxInfuserBlockEntity;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AdvancedFluxInfuserMenu extends FluxInfuserMenu {
    public AdvancedFluxInfuserMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, inventory.player.level().getBlockEntity(data.readBlockPos()), new SimpleContainerData(FluxInfuserBlockEntity.DATA_COUNT));
    }
    public AdvancedFluxInfuserMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModRegistry.ADVANCED_FLUX_INFUSER_MENU.get(), id, inventory, entity, data);
    }
}
