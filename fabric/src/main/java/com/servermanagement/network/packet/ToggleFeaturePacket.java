package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record ToggleFeaturePacket(String featureId, boolean enabled, long clientTick) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "toggle_feature_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public ToggleFeaturePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(featureId, 32767);
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
