package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMSetTimerPacket(String dimensionId, int seconds, String portalType, long clientTick) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "w_m_set_timer_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


// "nether", "end", or "both"
    public WMSetTimerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readInt(), buf.readUtf(32767), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
        buf.writeInt(seconds);
        buf.writeUtf(portalType, 32767);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                // Validate seconds to prevent abuse (max 30 days = 2,592,000 seconds)
                if (seconds < 0 || seconds > 2_592_000) return;
                
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
                    } else {
                        // Push a fresh world-detail snapshot so the open screen sees the new
                        // timer (or its cancellation) immediately instead of waiting for a reopen.
                        var data = com.servermanagement.features.worldmanager.WorldManager.getInstance().getData();
                        com.servermanagement.network.ModNetworking.sendToPlayer(
                            new com.servermanagement.network.packet.SyncWorldDetailPacket(
                                dimensionId,
                                data.areNetherPortalsEnabled(dimensionId),
                                data.areEndPortalsEnabled(dimensionId),
                                data.hasActiveTimer(dimensionId),
                                (int) data.getRemainingTime(dimensionId),
                                data.isDimensionChatConnected(dimensionId),
                                data.getTimerPortalType(dimensionId)
                            ),
                            player
                        );
                    }
                }
            }

}
}
