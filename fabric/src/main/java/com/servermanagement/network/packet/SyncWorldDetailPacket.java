package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record SyncWorldDetailPacket(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled, boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncWorldDetailPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_world_detail_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncWorldDetailPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncWorldDetailPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncWorldDetailPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readUtf(32));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeBoolean(netherPortalsEnabled);
        buf.writeBoolean(endPortalsEnabled);
        buf.writeBoolean(hasTimer);
        buf.writeInt(timerSeconds);
        buf.writeBoolean(chatConnected);
        buf.writeUtf(timerPortalType, 32);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on client
            com.servermanagement.client.ClientPacketHandler.handleWorldDetail(
                dimensionId, netherPortalsEnabled, endPortalsEnabled,
                hasTimer, timerSeconds, chatConnected, timerPortalType
            );

}

    public String getDimensionId() {
        return dimensionId;
    }

    public boolean isNetherPortalsEnabled() {
        return netherPortalsEnabled;
    }

    public boolean isEndPortalsEnabled() {
        return endPortalsEnabled;
    }

    public boolean hasTimer() {
        return hasTimer;
    }

    public int getTimerSeconds() {
        return timerSeconds;
    }

    public boolean isChatConnected() {
        return chatConnected;
    }

    public String getTimerPortalType() {
        return timerPortalType;
    }
}
