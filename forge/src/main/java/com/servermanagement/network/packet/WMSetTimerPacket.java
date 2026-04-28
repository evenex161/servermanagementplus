package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WMSetTimerPacket implements IPacket {
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
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
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
        ctx.get().setPacketHandled(true);
    }
}
