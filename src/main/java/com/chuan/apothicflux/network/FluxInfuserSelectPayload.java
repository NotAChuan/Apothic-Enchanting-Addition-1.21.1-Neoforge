package com.chuan.apothicflux.network;

import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 自动通量灌注器：玩家在 GUI 左侧点选某个候选结果。
 * <p>
 * 传的是「配方 ID」而不是槽位下标——客户端与服务端各自独立计算候选列表，
 * 用 ID 通信可以彻底避免两端顺序不一致导致选错配方。
 */
public record FluxInfuserSelectPayload(BlockPos pos, ResourceLocation recipeId) implements CustomPacketPayload {

    public static final Type<FluxInfuserSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "flux_infuser_select"));

    public static final StreamCodec<FriendlyByteBuf, FluxInfuserSelectPayload> CODEC = StreamCodec.ofMember(
            FluxInfuserSelectPayload::write,
            FluxInfuserSelectPayload::new
    );

    public FluxInfuserSelectPayload(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), ResourceLocation.parse(buffer.readUtf()));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeUtf(this.recipeId.toString());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
