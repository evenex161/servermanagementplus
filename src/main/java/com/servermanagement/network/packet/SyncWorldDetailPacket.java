package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class SyncWorldDetailPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncWorldDetailPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_world_detail_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncWorldDetailPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncWorldDetailPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String dimensionId;
    private final boolean netherPortalsEnabled;
    private final boolean endPortalsEnabled;
    private final boolean hasTimer;
    private final int timerSeconds;
    private final boolean chatConnected;
    private final String timerPortalType;

    public SyncWorldDetailPacket(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled,
                                 boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) {
        this.dimensionId = dimensionId;
        this.netherPortalsEnabled = netherPortalsEnabled;
        this.endPortalsEnabled = endPortalsEnabled;
        this.hasTimer = hasTimer;
        this.timerSeconds = timerSeconds;
        this.chatConnected = chatConnected;
        this.timerPortalType = timerPortalType;
    }

    public SyncWorldDetailPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
        this.netherPortalsEnabled = buf.readBoolean();
        this.endPortalsEnabled = buf.readBoolean();
        this.hasTimer = buf.readBoolean();
        this.timerSeconds = buf.readInt();
        this.chatConnected = buf.readBoolean();
        this.timerPortalType = buf.readUtf(32);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeBoolean(netherPortalsEnabled);
        buf.writeBoolean(endPortalsEnabled);
        buf.writeBoolean(hasTimer);
        buf.writeInt(timerSeconds);
        buf.writeBoolean(chatConnected);
        buf.writeUtf(timerPortalType, 32);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client
            com.servermanagement.client.ClientPacketHandler.handleWorldDetail(
                dimensionId, netherPortalsEnabled, endPortalsEnabled,
                hasTimer, timerSeconds, chatConnected, timerPortalType
            );
        });
        
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
