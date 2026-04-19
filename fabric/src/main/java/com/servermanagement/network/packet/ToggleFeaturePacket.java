package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public class ToggleFeaturePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ToggleFeaturePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "toggle_feature_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleFeaturePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleFeaturePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String featureId;
    private final boolean enabled;
    private final long clientTick;

    public ToggleFeaturePacket(String featureId, boolean enabled, long clientTick) {
        this.featureId = featureId;
        this.enabled = enabled;
        this.clientTick = clientTick;
    }

    public ToggleFeaturePacket(FriendlyByteBuf buf) {
        this.featureId = buf.readUtf(64);
        this.enabled = buf.readBoolean();
        this.clientTick = buf.readLong();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(featureId, 64);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on server thread
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "toggle_" + featureId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    // Toggle feature logic will be implemented in FeatureManager
                    com.servermanagement.features.FeatureManager.toggleFeature(featureId, enabled);
                }
            }

}

    public String getFeatureId() {
        return featureId;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public long getClientTick() {
        return clientTick;
    }
}
