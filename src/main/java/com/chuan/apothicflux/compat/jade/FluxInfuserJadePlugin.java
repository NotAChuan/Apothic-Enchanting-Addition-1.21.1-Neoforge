package com.chuan.apothicflux.compat.jade;

import com.chuan.apothicflux.block.FluxInfuserBlock;
import com.chuan.apothicflux.block.entity.FluxInfuserBlockEntity;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * 自动通量灌注器的 Jade 提示：只显示附魔属性与当前进度。
 * <p>
 * 刻意**不显示产物**——一台机器可以同时记录多条不同输入物的灌注配方，
 * 因此"当前产物"这个概念在提示里没有意义。
 */
@WailaPlugin
public class FluxInfuserJadePlugin implements IWailaPlugin, IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final String KEY_ETERNA = "Eterna";
    private static final String KEY_QUANTA = "Quanta";
    private static final String KEY_ARCANA = "Arcana";
    private static final String KEY_ENERGY = "Energy";
    private static final String KEY_CAPACITY = "Capacity";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(this, FluxInfuserBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, FluxInfuserBlock.class);
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof FluxInfuserBlockEntity infuser) {
            tag.putInt(KEY_ETERNA, Math.round(infuser.getEterna()));
            tag.putInt(KEY_QUANTA, Math.round(infuser.getQuanta()));
            tag.putInt(KEY_ARCANA, Math.round(infuser.getArcana()));
            tag.putInt(KEY_ENERGY, infuser.getEnergyStorage().getEnergyStored());
            tag.putInt(KEY_CAPACITY, infuser.getEnergyStorage().getMaxEnergyStored());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        tooltip.add(Component.translatable("jade.apothic_flux.ritual_core.eterna", data.getInt(KEY_ETERNA)));
        tooltip.add(Component.translatable("jade.apothic_flux.ritual_core.quanta", data.getInt(KEY_QUANTA)));
        tooltip.add(Component.translatable("jade.apothic_flux.ritual_core.arcana", data.getInt(KEY_ARCANA)));

        tooltip.add(Component.translatable("jade.apothic_flux.flux_infuser.energy",
                data.getInt(KEY_ENERGY), data.getInt(KEY_CAPACITY)));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "flux_infuser");
    }
}
