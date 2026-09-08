package com.chuan.apothicflux;

import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.network.NetworkHandler;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.chuan.apothicflux.client.renderer.FluxEnchantingTableRenderer;
import com.chuan.apothicflux.client.renderer.FluxExpConverterBlockEntityRenderer;
import com.chuan.apothicflux.client.renderer.RitualBlockEntityRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.locating.IModFile;

import java.nio.file.Path;
import java.util.Optional;

import static com.chuan.apothicflux.registry.ModRegistry.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ApothicFlux.MOD_ID)
public class ApothicFlux {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "apothic_flux";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "apothic_flux" namespace
//    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
//    // Create a Deferred Register to hold Items which will all be registered under the "apothic_flux" namespace
//    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "apothic_flux" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // Creates a creative tab with the id "apothic_flux:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> APOTHIN_ENCHANTING_ADDITION_TAB = CREATIVE_MODE_TABS.register("apothic_enchanting_addition", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.apothic_flux.main")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModRegistry.FLUX_ENCHANTING_TABLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FLUX_STATS_BOOKSHELF_TIER_1.get());
                output.accept(FLUX_STATS_BOOKSHELF_TIER_2.get());
                output.accept(FLUX_STATS_BOOKSHELF_TIER_3.get());
                output.accept(FLUX_STATS_BOOKSHELF_TIER_4.get());
                output.accept(FLUX_ENCHANTING_TABLE_ITEM.get());
                output.accept(FLUX_ANVIL_ITEM.get());
                output.accept(FLUX_EXP_CONVERTER_ITEM.get());
                output.accept(EXPERIENCE_BUCKET.get());
                output.accept(SOLIDIFIED_FLUX_EXPERIENCE.get());
                output.accept(COMPRESSED_SOLIDIFIED_FLUX_EXPERIENCE.get());
                output.accept(FLUX_SPAWNER_ITEM.get());
                if (BEEHIVE_SIMULATION_UPGRADE != null) {
                    output.accept(BEEHIVE_SIMULATION_UPGRADE.get());
                }
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ApothicFlux(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ApothicAdditionConfig.SPEC);
        ModRegistry.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPackFinders);
        CREATIVE_MODE_TABS.register(modEventBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onPlayerTick);

        modEventBus.addListener(this::onClientSetup);

    }

    // 注册模组内置资源包 resourcepacks/apothic flux ae。
    // 目录名含空格，无法作为 ResourceLocation 的 path，故用 addRepositorySource 手动构建 Pack。
    private void registerPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        IModFile modFile = ModList.get().getModFileById(MOD_ID).getFile();
        Path packRoot = modFile.findResource("resourcepacks/apothic flux ae");
        event.addRepositorySource(packConsumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo("mod/apothic_flux_ae", Component.literal("Apothic Flux AE"),
                            PackSource.DEFAULT, Optional.empty()),
                    new PathPackResources.PathResourcesSupplier(packRoot),
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(false, Pack.Position.TOP, false)
            );
            if (pack != null) {
                packConsumer.accept(pack);
            }
        });
    }
    // 客户端初始化事件
    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(
                    ModRegistry.FLUX_ENCHANTING_TABLE_BE.get(),
                    FluxEnchantingTableRenderer::new
            );
            BlockEntityRenderers.register(
                    ModRegistry.RITUAL_BE.get(),
                    RitualBlockEntityRenderer::new
            );
            BlockEntityRenderers.register(
                    ModRegistry.FLUX_EXP_CONVERTER_BE.get(),
                    FluxExpConverterBlockEntityRenderer::new
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

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModRegistry.FLUX_EXP_CONVERTER_BE.get(),
                (be, side) -> be.inventory
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModRegistry.FLUX_EXP_CONVERTER_BE.get(),
                (be, side) -> be.getFluidHandler()
        );
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isSpectator()) {
            return;
        }

        if (player.tickCount % 40 != 0) {
            return;
        }

        if (player.isInFluidType((fluidType, height) -> fluidType == ModRegistry.EXPERIENCE_FLUID_TYPE.get())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(dev.shadowsoffire.apothic_attributes.api.ALObjects.MobEffects.KNOWLEDGE, 60, 0));
        }
    }
}
