package com.chuan.apothicenchantingaddition.client;

import com.chuan.apothicenchantingaddition.client.renderer.RitualBlockEntityRenderer;
import com.chuan.apothicenchantingaddition.client.screen.StatsBookshelfScreen;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// 自动监听客户端事件
@EventBusSubscriber(modid = ModRegistry.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.STATS_BOOKSHELF_MENU.get(), StatsBookshelfScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.RITUAL_BE.get(), RitualBlockEntityRenderer::new);
    }
}
