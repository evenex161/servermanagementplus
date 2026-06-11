package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet requesting the banned/whitelisted player lists.
 */
public record PMRequestPlayerListsPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PMRequestPlayerListsPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "p_m_request_player_lists"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMRequestPlayerListsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMRequestPlayerListsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public PMRequestPlayerListsPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {
        // No data needed
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) {
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }
        });
        // packet handled
    }
}
