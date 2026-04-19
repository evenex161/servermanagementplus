package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record WMSetTimerPacket(String dimensionId, int seconds, String portalType, long clientTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WMSetTimerPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_set_timer"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMSetTimerPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMSetTimerPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
// "nether", "end", or "both"


    public WMSetTimerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256), buf.readInt(), buf.readUtf(32), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeInt(seconds);
        buf.writeUtf(portalType, 32);
        buf.writeLong(clientTick);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
                    }
                }
            }
        });
        // packet handled
    }
}
