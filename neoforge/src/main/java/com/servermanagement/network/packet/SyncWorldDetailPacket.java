package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record SyncWorldDetailPacket(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled, boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncWorldDetailPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_world_detail"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncWorldDetailPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncWorldDetailPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on client
            com.servermanagement.client.ClientPacketHandler.handleWorldDetail(
                dimensionId, netherPortalsEnabled, endPortalsEnabled,
                hasTimer, timerSeconds, chatConnected, timerPortalType
            );
        });
        // packet handled
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
