package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class WMToggleTabIsolationPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WMToggleTabIsolationPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_toggle_tab_isolation"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMToggleTabIsolationPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMToggleTabIsolationPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
        });
        // packet handled
    }
}
