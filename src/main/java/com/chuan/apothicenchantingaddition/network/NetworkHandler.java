package com.chuan.apothicenchantingaddition.network;

import com.chuan.apothicenchantingaddition.block.entity.StatsBookshelfBlockEntity;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

    // 这个方法将由主类调用，绝对不会注册失败
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ModRegistry.MOD_ID).versioned("1.0.0");

        registrar.playToServer(
                UpdateStatsPayload.TYPE,
                UpdateStatsPayload.CODEC,
                NetworkHandler::handleData
        );
    }

    public static void handleData(final UpdateStatsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.isLoaded(payload.pos())) {
                    if (level.getBlockEntity(payload.pos()) instanceof StatsBookshelfBlockEntity be) {
                        // 扩大判定距离到 256（16格），防止稍微离远一点就存不上
                        if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) <= 256.0) {
                            be.setStats(payload.eterna(), payload.quanta(), payload.arcana(), payload.clues(), payload.treasure(), payload.stable());
                        }
                    }
                }
            }
        });
    }
}
