package com.chuan.apothicflux;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.client.screen.FluxAnvilScreen;
import com.chuan.apothicflux.client.screen.FluxEnchantingScreen;
import com.chuan.apothicflux.client.screen.FluxExpConverterScreen;
import com.chuan.apothicflux.client.screen.FluxSpawnerScreen;
import com.chuan.apothicflux.client.screen.FluxStatsBookshelfScreen;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ApothicFlux.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ApothicFlux.MOD_ID, value = Dist.CLIENT)
public class ApothicFluxClient {
    public ApothicFluxClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.FLUX_ENCHANTING_MENU.get(), FluxEnchantingScreen::new);
        event.register(ModRegistry.FLUX_ANVIL_MENU.get(), FluxAnvilScreen::new);
        event.register(ModRegistry.FLUX_EXP_CONVERTER_MENU.get(), FluxExpConverterScreen::new);
        event.register(ModRegistry.FLUX_SPAWNER_MENU.get(), FluxSpawnerScreen::new);
        event.register(ModRegistry.STATS_BOOKSHELF_MENU.get(), FluxStatsBookshelfScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ApothicFlux.LOGGER.info("HELLO FROM CLIENT SETUP");
        ApothicFlux.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
