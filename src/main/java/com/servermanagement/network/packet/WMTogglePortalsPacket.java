package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class WMTogglePortalsPacket implements IPacket {
    public static final CustomPacketPayload.Type<WMTogglePortalsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "w_m_toggle_portals_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMTogglePortalsPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMTogglePortalsPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String dimensionId;
    private final boolean enabled;
    private final String portalType; // "nether", "end", or "both"
    private final long clientTick;

    public WMTogglePortalsPacket(String dimensionId, boolean enabled, String portalType, long clientTick) {
        this.dimensionId = dimensionId;
        this.enabled = enabled;
        this.portalType = portalType;
        this.clientTick = clientTick;
    }

    public WMTogglePortalsPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
        this.enabled = buf.readBoolean();
        this.portalType = buf.readUtf(32);
        this.clientTick = buf.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeBoolean(enabled);
        buf.writeUtf(portalType, 32);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "portal_" + dimensionId + "_" + portalType;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    // Lock check: reject if a timer is active for this dimension
                    var worldData = com.servermanagement.features.worldmanager.WorldManager.getInstance().getData();
                    if (worldData.hasActiveTimer(dimensionId)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cCannot change portal state while a timer is active for this dimension!"));
                        return;
                    }

                    // Apply granular portal state change
                    com.servermanagement.features.worldmanager.WorldManager.setPortalsByType(dimensionId, portalType, enabled);

                    // Broadcast the change to all players
                    if (player.getServer() != null) {
                        com.servermanagement.features.worldmanager.WorldManager.broadcastPortalChange(
                            player.getServer(), dimensionId, portalType, enabled);
                    }
                }
            }
        });
        
    }
}
