package com.chuan.apothicenchantingaddition;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.network.NetworkHandler;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.chuan.apothicenchantingaddition.client.renderer.FluxEnchantingTableRenderer;
import net.minecraft.world.item.*;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.chuan.apothicenchantingaddition.registry.ModRegistry.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ApothicEnchantingAddition.MOD_ID)
public class ApothicEnchantingAddition {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "apothicenchantingaddition";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "apothicenchantingaddition" namespace
//    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
//    // Create a Deferred Register to hold Items which will all be registered under the "apothicenchantingaddition" namespace
//    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "apothicenchantingaddition" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // Creates a creative tab with the id "apothicenchantingaddition:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> APOTHIN_ENCHANTING_ADDITION_TAB = CREATIVE_MODE_TABS.register("apothic_enchanting_addition", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.apothicenchantingaddition.main")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModRegistry.FLUX_ENCHANTING_TABLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FLUX_STATS_BOOKSHELF_TIER_1.get());
                output.accept(FLUX_STATS_BOOKSHELF_TIER_2.get());
                output.accept(FLUX_STATS_BOOKSHELF_TIER_3.get());
                output.accept(FLUX_STATS_BOOKSHELF_TIER_4.get());
                output.accept(FLUX_ENCHANTING_TABLE_ITEM.get());
                output.accept(FLUX_ANVIL_ITEM.get());
                output.accept(SOLIDIFIED_FLUX_EXPERIENCE.get());
                output.accept(COMPRESSED_SOLIDIFIED_FLUX_EXPERIENCE.get());
                output.accept(FLUX_SPAWNER_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ApothicEnchantingAddition(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ApothicAdditionConfig.SPEC);
        ModRegistry.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);
        modEventBus.addListener(this::registerCapabilities);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::onClientSetup);

    }

    // 客户端初始化事件
    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(
                    ModRegistry.FLUX_ENCHANTING_TABLE_BE.get(),
                    FluxEnchantingTableRenderer::new
            );
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModRegistry.STATS_BOOKSHELF_BE.get(),
                (be, side) -> be.energyStorage
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModRegistry.FLUX_ENCHANTING_TABLE_BE.get(),
                (be, side) -> be.energyStorage
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModRegistry.FLUX_ANVIL_BE.get(),
                (be, side) -> be.getEnergyStorage()
        );

        // 注册通量刷怪笼的能量接收能力
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModRegistry.FLUX_SPAWNER_BE.get(),
                (be, side) -> be.energyStorage
        );

        // 注册通量刷怪笼的物品交互能力 (只暴露输出槽，允许从所有面抽取)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModRegistry.FLUX_SPAWNER_BE.get(),
                (be, side) -> be.outputItemHandler
        );
    }
}
