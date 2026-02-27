package com.chuan.apothicenchantingaddition.network;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateStatsPayload(BlockPos pos, float eterna, float quanta, float arcana, int clues, boolean treasure, boolean stable) implements CustomPacketPayload {

    // 1.21.1 标准的 Type 定义
    public static final Type<UpdateStatsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModRegistry.MOD_ID, "update_stats"));

    // 不使用 composite，手动实现 Codec
    public static final StreamCodec<FriendlyByteBuf, UpdateStatsPayload> CODEC = StreamCodec.ofMember(
            UpdateStatsPayload::write,
            UpdateStatsPayload::new
    );

    public UpdateStatsPayload(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeFloat(this.eterna);
        buffer.writeFloat(this.quanta);
        buffer.writeFloat(this.arcana);
        buffer.writeInt(this.clues);
        buffer.writeBoolean(this.treasure);
        buffer.writeBoolean(this.stable);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
