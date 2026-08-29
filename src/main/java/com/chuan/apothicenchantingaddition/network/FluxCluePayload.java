package com.chuan.apothicenchantingaddition.network;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;

public record FluxCluePayload(int slot, List<EnchantmentInstance> clues, boolean allRevealed) implements CustomPacketPayload {

    public static final Type<FluxCluePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("apothic_flux", "flux_clue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluxCluePayload> STREAM_CODEC = StreamCodec.ofMember(
            FluxCluePayload::write, FluxCluePayload::new
    );

    private FluxCluePayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readInt(),
                // 【修复】：显式将 b 强转为 RegistryFriendlyByteBuf
                buf.readList(b -> new EnchantmentInstance(
                        ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT).decode((RegistryFriendlyByteBuf) b),
                        b.readInt()
                )),
                buf.readBoolean()
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeCollection(clues, (b, clue) -> {
            // 【修复】：显式将 b 强转为 RegistryFriendlyByteBuf
            ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT).encode((RegistryFriendlyByteBuf) b, clue.enchantment);
            b.writeInt(clue.level);
        });
        buf.writeBoolean(allRevealed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
