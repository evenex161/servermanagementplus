package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

public record ToggleFeaturePacket(String featureId, boolean enabled, long clientTick) implements IPacket {

    public ToggleFeaturePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readLong());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(featureId, 32767);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on server thread
            var player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "toggle_" + featureId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    // Toggle feature logic will be implemented in FeatureManager
                    com.servermanagement.features.FeatureManager.toggleFeature(featureId, enabled);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
