package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMToggleChatIsolationPacket(String dimensionId, boolean enabled, long clientTick) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<WMToggleChatIsolationPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_toggle_chat_isolation_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, WMToggleChatIsolationPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMToggleChatIsolationPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public WMToggleChatIsolationPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                String actionKey = "chat_isolation_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    var worldManager = com.servermanagement.features.worldmanager.WorldManager.getInstance();
                    if (dimensionId.isEmpty()) {
                        // Global master toggle (from Global Settings screen)
                        worldManager.getData().setChatIsolationEnabled(enabled);
                    } else {
                        // Per-dimension chat connection toggle (from World Detail screen)
                        worldManager.getData().setDimensionChatConnected(dimensionId, enabled);
                    }
                    worldManager.save();
                }
            }

}
}
