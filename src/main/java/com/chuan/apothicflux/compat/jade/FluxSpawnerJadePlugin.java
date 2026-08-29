package com.chuan.apothicflux.compat.jade;

import com.chuan.apothicflux.block.FluxSpawnerBlock;
import com.chuan.apothicflux.block.entity.FluxSpawnerBlockEntity;
import net.minecraft.client.gui.screens.Screen;
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
public class FluxSpawnerJadePlugin implements IWailaPlugin, IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final String KEY_MIN_DELAY = "MinDelay";
    private static final String KEY_MAX_DELAY = "MaxDelay";
    private static final String KEY_SPAWN_COUNT = "SpawnCount";
    private static final String KEY_REDSTONE = "RedstoneControl";
    private static final String KEY_ECHOING = "Echoing";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(this, FluxSpawnerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, FluxSpawnerBlock.class);
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof FluxSpawnerBlockEntity be) {
            tag.putInt(KEY_MIN_DELAY, be.getMinDelay());
            tag.putInt(KEY_MAX_DELAY, be.getMaxDelay());
            tag.putInt(KEY_SPAWN_COUNT, be.getSpawnCount());
            tag.putBoolean(KEY_REDSTONE, be.isRedstoneControl());
            tag.putInt(KEY_ECHOING, be.getEchoing());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        tooltip.add(Component.translatable("gui.apothic_flux.flux_spawner.min_delay", data.getInt(KEY_MIN_DELAY)));
        tooltip.add(Component.translatable("gui.apothic_flux.flux_spawner.max_delay", data.getInt(KEY_MAX_DELAY)));
        tooltip.add(Component.translatable("gui.apothic_flux.flux_spawner.spawn_count", data.getInt(KEY_SPAWN_COUNT)));

        if (Screen.hasControlDown()) {
            if (data.getBoolean(KEY_REDSTONE)) {
                tooltip.add(Component.translatable("jade.apothic_flux.flux_spawner.redstone"));
            }

            int echoing = data.getInt(KEY_ECHOING);
            if (echoing > 0) {
                if (echoing == 1) {
                    tooltip.add(Component.translatable("jade.apothic_flux.flux_spawner.echoing"));
                } else {
                    tooltip.add(Component.translatable("jade.apothic_flux.flux_spawner.echoing_level", echoing));
                }
            }
        } else {
            tooltip.add(Component.translatable("jade.apothic_flux.flux_spawner.ctrl_stats"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.parse("apothic_flux:flux_spawner");
    }
}
