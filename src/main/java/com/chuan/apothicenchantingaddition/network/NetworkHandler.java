package com.chuan.apothicenchantingaddition.network;

import com.chuan.apothicenchantingaddition.block.entity.FluxStatsBookshelfBlockEntity;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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

        registrar.playToServer(
                FluxActionPayload.TYPE,
                FluxActionPayload.CODEC,
                NetworkHandler::handleFluxAction
        );

        // ================= 【新增】 =================
        // 注册服务端到客户端 (S ➔ C) 的线索盲盒包
        registrar.playToClient(
                FluxCluePayload.TYPE,
                FluxCluePayload.STREAM_CODEC,
                NetworkHandler::handleFluxClue
        );
        // ===========================================
    }

    // ================= 【新增】 =================
    // 客户端处理收到的盲盒线索 (S ➔ C)
    public static void handleFluxClue(final FluxCluePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // context.player() 会自动获取当前端的玩家对象（客户端就是 LocalPlayer）
            Player player = context.player();
            if (player != null && player.containerMenu instanceof com.chuan.apothicenchantingaddition.menu.FluxEnchantingMenu menu) {
                menu.setClues(payload.slot(), payload.clues(), payload.allRevealed());
            }
        });
    }
    // ===========================================

    public static void handleFluxAction(final FluxActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 安全校验：玩家距离方块不能太远 (16格内)
                if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) <= 256.0) {
                    // 如果玩家当前打开的正是通量附魔台菜单，直接将动作移交给菜单处理
                    if (player.containerMenu instanceof com.chuan.apothicenchantingaddition.menu.FluxEnchantingMenu menu) {
                        menu.handleAction(player, payload.actionId());
                    }
                }
            }
        });
    }

    public static void handleData(final UpdateStatsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.isLoaded(payload.pos())) {
                    if (level.getBlockEntity(payload.pos()) instanceof FluxStatsBookshelfBlockEntity be) {
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
