package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class ToggleFeaturePacket implements IPacket {
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

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(featureId, 64);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on server thread
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "toggle_" + featureId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    // Toggle feature logic will be implemented in FeatureManager
                    com.servermanagement.features.FeatureManager.toggleFeature(featureId, enabled);
                }
            }
        });
        ctx.setPacketHandled(true);
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
