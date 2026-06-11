package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMToggleTabIsolationPacket(boolean enabled, long clientTick) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<WMToggleTabIsolationPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "w_m_toggle_tab_isolation_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, WMToggleTabIsolationPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMToggleTabIsolationPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public WMToggleTabIsolationPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "tab_isolation";
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setTabIsolationEnabled(enabled);
                    // Immediately apply or restore tab isolation
                    var server = player.level().getServer();
                    if (server != null) {
                        com.servermanagement.features.worldmanager.TabListIsolationHandler.onIsolationToggled(server);
                    }
                }
            }

}
}
