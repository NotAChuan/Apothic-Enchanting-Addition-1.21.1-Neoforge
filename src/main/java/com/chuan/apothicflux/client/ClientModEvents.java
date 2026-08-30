package com.chuan.apothicflux.client;

import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ModRegistry.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");
    private static final ResourceLocation UNDERWATER_OVERLAY = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    private static final int EXPERIENCE_TINT = 0xFF7DE3A8;

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerFluidTypeExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {
                return UNDERWATER_OVERLAY;
            }

            @Override
            public int getTintColor() {
                return EXPERIENCE_TINT;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return EXPERIENCE_TINT;
            }

            @Override
            public int getTintColor(net.neoforged.neoforge.fluids.FluidStack stack) {
                return EXPERIENCE_TINT;
            }
        }, ModRegistry.EXPERIENCE_FLUID_TYPE.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), ModRegistry.EXPERIENCE_BUCKET.get());
    }
}
