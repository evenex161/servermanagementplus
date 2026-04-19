package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record ToggleAutoShowPacket(boolean autoShow) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ToggleAutoShowPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "toggle_auto_show_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleAutoShowPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleAutoShowPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public ToggleAutoShowPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                // Save player preference
                com.servermanagement.config.PlayerPreferences.setAutoShow(player.getUUID(), autoShow);
            }

}

    public boolean isAutoShow() {
        return autoShow;
    }
}
