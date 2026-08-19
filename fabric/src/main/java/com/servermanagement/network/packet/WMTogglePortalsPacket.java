package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMTogglePortalsPacket(String dimensionId, boolean enabled, String portalType, long clientTick) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "w_m_toggle_portals_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


// "nether", "end", or "both"
    public WMTogglePortalsPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readUtf(32767), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
        buf.writeBoolean(enabled);
        buf.writeUtf(portalType, 32767);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
