package com.chuan.apothicflux.compat.jade;

import com.chuan.apothicflux.block.RitualCoreBlock;
import com.chuan.apothicflux.block.entity.RitualBlockEntity;
import com.chuan.apothicflux.recipe.RitualStats;
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

@WailaPlugin
public class RitualCoreJadePlugin implements IWailaPlugin, IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final String KEY_ETERNA = "Eterna";
    private static final String KEY_QUANTA = "Quanta";
    private static final String KEY_ARCANA = "Arcana";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(this, RitualBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, RitualCoreBlock.class);
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof RitualBlockEntity ritual) {
            RitualStats stats = ritual.computeStats();
            tag.putInt(KEY_ETERNA, Math.round(stats.eterna()));
            tag.putInt(KEY_QUANTA, Math.round(stats.quanta()));
            tag.putInt(KEY_ARCANA, Math.round(stats.arcana()));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        tooltip.add(Component.translatable("jade.apothic_flux.ritual_core.eterna", data.getInt(KEY_ETERNA)));
        tooltip.add(Component.translatable("jade.apothic_flux.ritual_core.quanta", data.getInt(KEY_QUANTA)));
        tooltip.add(Component.translatable("jade.apothic_flux.ritual_core.arcana", data.getInt(KEY_ARCANA)));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.parse("apothic_flux:ritual_core");
    }
}
