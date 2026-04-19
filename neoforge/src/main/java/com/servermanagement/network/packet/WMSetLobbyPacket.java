package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record WMSetLobbyPacket(BlockPos pos, String dimensionId, long clientTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WMSetLobbyPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_set_lobby"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMSetLobbyPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMSetLobbyPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public WMSetLobbyPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readUtf(256), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(dimensionId, 256);
        buf.writeLong(clientTick);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "lobby_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setLobbySpawn(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, dimensionId,
                        player.getYRot(), player.getXRot());
                }
            }
        });
        // packet handled
    }
}
