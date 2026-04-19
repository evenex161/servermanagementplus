package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public class SyncAutoShowPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncAutoShowPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_auto_show_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAutoShowPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAutoShowPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean autoShow;

    public SyncAutoShowPacket(boolean autoShow) {
        this.autoShow = autoShow;
    }

    public SyncAutoShowPacket(FriendlyByteBuf buf) {
        this.autoShow = buf.readBoolean();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on client - update UI

}

    public boolean isAutoShow() {
        return autoShow;
    }
}
