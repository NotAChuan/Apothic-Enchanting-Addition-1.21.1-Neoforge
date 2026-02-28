package com.chuan.apothicenchantingaddition.client;

import com.chuan.apothicenchantingaddition.client.renderer.RitualBlockEntityRenderer;
import com.chuan.apothicenchantingaddition.client.screen.StatsBookshelfScreen;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import com.chuan.apothicenchantingaddition.client.screen.FluxEnchantingScreen;
import com.chuan.apothicenchantingaddition.client.screen.FluxAnvilScreen;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// 隐形 Bug 修复：必须加上 bus = EventBusSubscriber.Bus.MOD，否则界面注册事件根本不会触发！
@EventBusSubscriber(modid = ModRegistry.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.STATS_BOOKSHELF_MENU.get(), StatsBookshelfScreen::new);
        event.register(ModRegistry.FLUX_ENCHANTING_MENU.get(), FluxEnchantingScreen::new);

        // 泛型报错修复：先将我们的 MenuType 强转为原版的 MenuType<AnvilMenu> 骗过编译器！
        MenuType<AnvilMenu> fluxAnvilType = (MenuType<AnvilMenu>) (Object) ModRegistry.FLUX_ANVIL_MENU.get();
        event.register(fluxAnvilType, FluxAnvilScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.RITUAL_BE.get(), RitualBlockEntityRenderer::new);
    }
}
