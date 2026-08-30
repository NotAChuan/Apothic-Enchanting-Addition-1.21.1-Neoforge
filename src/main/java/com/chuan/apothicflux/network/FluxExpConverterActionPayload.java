package com.chuan.apothicflux.network;

import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FluxExpConverterActionPayload(BlockPos pos, int actionId) implements CustomPacketPayload {
    public enum Action {
        STORE_1,
        STORE_10,
        STORE_ALL,
        TAKE_1,
        TAKE_10,
        TAKE_ALL
    }

    public static final Type<FluxExpConverterActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "flux_exp_converter_action"));

    public static final StreamCodec<FriendlyByteBuf, FluxExpConverterActionPayload> CODEC = StreamCodec.ofMember(
            FluxExpConverterActionPayload::write,
            FluxExpConverterActionPayload::new
    );

    public FluxExpConverterActionPayload(FriendlyByteBuf buffer) {
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
