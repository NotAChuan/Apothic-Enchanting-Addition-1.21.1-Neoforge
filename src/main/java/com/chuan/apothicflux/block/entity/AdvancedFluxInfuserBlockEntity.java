package com.chuan.apothicflux.block.entity;

import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.menu.AdvancedFluxInfuserMenu;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AdvancedFluxInfuserBlockEntity extends FluxInfuserBlockEntity {
    public AdvancedFluxInfuserBlockEntity(BlockPos pos, BlockState state) { super(ModRegistry.ADVANCED_FLUX_INFUSER_BE.get(), pos, state); }
    @Override protected int getConfiguredEnergyCapacity() { return Math.max(0, ApothicAdditionConfig.ADVANCED_FLUX_INFUSER_CAPACITY.get()); }
    @Override protected int getConfiguredEnergyPerTick() { return Math.max(0, ApothicAdditionConfig.ADVANCED_FLUX_INFUSER_ENERGY_PER_TICK.get()); }
    @Override protected boolean isEnergyEnabled() { return ApothicAdditionConfig.ADVANCED_FLUX_INFUSER_ENERGY_ENABLED.get(); }
    @Override protected int getConfiguredFluidCapacity() { return Math.max(1, ApothicAdditionConfig.ADVANCED_FLUX_INFUSER_FLUID_CAPACITY.get()); }
    @Override protected String getConfiguredFluidId() { return ApothicAdditionConfig.ADVANCED_FLUX_INFUSER_EXPERIENCE_FLUID.get(); }
    @Override public int getCraftTime() { return Math.max(1, ApothicAdditionConfig.ADVANCED_FLUX_INFUSER_CRAFT_TIME.get()); }
    @Override public boolean ignoresRequirements() { return true; }
    @Override public boolean showsStats() { return false; }
    @Override public Component getDisplayName() { return Component.translatable("block.apothic_flux.advanced_flux_infuser"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AdvancedFluxInfuserMenu(id, inventory, this, this.dataAccess);
    }
}
