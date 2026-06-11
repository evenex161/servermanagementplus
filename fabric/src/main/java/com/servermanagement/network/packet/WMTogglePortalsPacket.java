package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMTogglePortalsPacket(String dimensionId, boolean enabled, String portalType, long clientTick) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<WMTogglePortalsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "w_m_toggle_portals_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, WMTogglePortalsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMTogglePortalsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }// "nether", "end", or "both"
    public WMTogglePortalsPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256), buf.readBoolean(), buf.readUtf(32), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeBoolean(enabled);
        buf.writeUtf(portalType, 32);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) {
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
                    if (player.level().getServer() != null) {
                        com.servermanagement.features.worldmanager.WorldManager.broadcastPortalChange(
                            player.level().getServer(), dimensionId, portalType, enabled);
                    }
                }
            }

}
}
