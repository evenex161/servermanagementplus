package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class ToggleFeaturePacket implements IPacket {
    public static final CustomPacketPayload.Type<ToggleFeaturePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "toggle_feature_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleFeaturePacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleFeaturePacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Handle on server thread
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "toggle_" + featureId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    // Toggle feature logic will be implemented in FeatureManager
                    com.servermanagement.features.FeatureManager.toggleFeature(featureId, enabled);
                }
            }
        });
        
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
