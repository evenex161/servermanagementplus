package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record SyncAutoShowPacket(boolean autoShow) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncAutoShowPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_auto_show"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAutoShowPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAutoShowPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncAutoShowPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on client - update UI
        });
        // packet handled
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
