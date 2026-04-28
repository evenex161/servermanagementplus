package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

public record WMSetTimerPacket(String dimensionId, int seconds, String portalType, long clientTick) implements IPacket {

    public WMSetTimerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256), buf.readInt(), buf.readUtf(32), buf.readLong());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeInt(seconds);
        buf.writeUtf(portalType, 32);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
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
                            "┬ºcA timer is already running for " + activeDimName + "! Only one timer can be active at a time."));
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
        });
        ctx.get().setPacketHandled(true);
    }
}
