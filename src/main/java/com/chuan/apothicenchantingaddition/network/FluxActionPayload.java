package com.chuan.apothicenchantingaddition.network;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FluxActionPayload(BlockPos pos, int actionId) implements CustomPacketPayload {

    // actionId 规范: 0=刷新附魔, 1=附魔选项一, 2=附魔选项二, 3=附魔选项三
    public static final Type<FluxActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "flux_action"));

    public static final StreamCodec<FriendlyByteBuf, FluxActionPayload> CODEC = StreamCodec.ofMember(
            FluxActionPayload::write,
            FluxActionPayload::new
    );

    public FluxActionPayload(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.actionId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
