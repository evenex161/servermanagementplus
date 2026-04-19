package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class WMTogglePortalsPacket implements IPacket {
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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
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
        ctx.setPacketHandled(true);
    }
}
