package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public class WMToggleTabIsolationPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<WMToggleTabIsolationPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_toggle_tab_isolation_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, WMToggleTabIsolationPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMToggleTabIsolationPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean enabled;
    private final long clientTick;

    public WMToggleTabIsolationPacket(boolean enabled, long clientTick) {
        this.enabled = enabled;
        this.clientTick = clientTick;
    }

    public WMToggleTabIsolationPacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
        this.clientTick = buf.readLong();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "tab_isolation";
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setTabIsolationEnabled(enabled);
                    // Immediately apply or restore tab isolation
                    var server = player.getServer();
                    if (server != null) {
                        com.servermanagement.features.worldmanager.TabListIsolationHandler.onIsolationToggled(server);
                    }
                }
            }

}
}
