package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class WMSetTimerPacket implements IPacket {
    public static final CustomPacketPayload.Type<WMSetTimerPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "w_m_set_timer_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMSetTimerPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMSetTimerPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String dimensionId;
    private final int seconds;
    private final String portalType; // "nether", "end", or "both"
    private final long clientTick;

    public WMSetTimerPacket(String dimensionId, int seconds, String portalType, long clientTick) {
        this.dimensionId = dimensionId;
        this.seconds = seconds;
        this.portalType = portalType;
        this.clientTick = clientTick;
    }

    public WMSetTimerPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
        this.seconds = buf.readInt();
        this.portalType = buf.readUtf(32);
        this.clientTick = buf.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeInt(seconds);
        buf.writeUtf(portalType, 32);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "timer_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    boolean success = com.servermanagement.features.worldmanager.PortalTimerManager.setTimer(dimensionId, seconds, portalType);
                    if (!success) {
                        var worldData = com.servermanagement.features.worldmanager.WorldManager.getInstance().getData();
                        String activeDim = worldData.getActiveTimerDimension();
                        String activeDimName = activeDim != null 
                            ? com.servermanagement.features.worldmanager.WorldManager.getDimensionName(activeDim) 
                            : "unknown";
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cA timer is already running for " + activeDimName + "! Only one timer can be active at a time."));
                    }
                }
            }
        });
        
    }
}
