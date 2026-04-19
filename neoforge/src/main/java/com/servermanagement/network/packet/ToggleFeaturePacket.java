package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record ToggleFeaturePacket(String featureId, boolean enabled, long clientTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleFeaturePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "toggle_feature"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleFeaturePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleFeaturePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public ToggleFeaturePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readBoolean(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(featureId, 64);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on server thread
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "toggle_" + featureId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    // Toggle feature logic will be implemented in FeatureManager
                    com.servermanagement.features.FeatureManager.toggleFeature(featureId, enabled);
                }
            }
        });
        // packet handled
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
