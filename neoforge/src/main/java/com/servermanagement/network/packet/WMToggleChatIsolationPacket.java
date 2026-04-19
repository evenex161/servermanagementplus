package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class WMToggleChatIsolationPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WMToggleChatIsolationPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_toggle_chat_isolation"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMToggleChatIsolationPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMToggleChatIsolationPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final String dimensionId;
    private final boolean enabled;
    private final long clientTick;

    public WMToggleChatIsolationPacket(String dimensionId, boolean enabled, long clientTick) {
        this.dimensionId = dimensionId;
        this.enabled = enabled;
        this.clientTick = clientTick;
    }

    public WMToggleChatIsolationPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
        this.enabled = buf.readBoolean();
        this.clientTick = buf.readLong();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
        });
        // packet handled
    }
}
