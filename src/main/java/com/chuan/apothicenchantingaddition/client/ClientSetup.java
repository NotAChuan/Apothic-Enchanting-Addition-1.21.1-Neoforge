package com.chuan.apothicenchantingaddition.client;

import com.chuan.apothicenchantingaddition.client.screen.StatsBookshelfScreen;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ModRegistry.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.STATS_BOOKSHELF_MENU.get(), StatsBookshelfScreen::new);
    }
}
