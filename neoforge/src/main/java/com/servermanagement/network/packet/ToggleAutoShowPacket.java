package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record ToggleAutoShowPacket(boolean autoShow) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleAutoShowPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "toggle_auto_show"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleAutoShowPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleAutoShowPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public ToggleAutoShowPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null) {
                // Save player preference
                com.servermanagement.config.PlayerPreferences.setAutoShow(player.getUUID(), autoShow);
            }
        });
        // packet handled
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
