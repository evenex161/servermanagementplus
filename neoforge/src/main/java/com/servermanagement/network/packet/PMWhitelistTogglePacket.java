package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet to toggle whitelist enforcement on/off.
 */
public record PMWhitelistTogglePacket(boolean enabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PMWhitelistTogglePacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "p_m_whitelist_toggle"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMWhitelistTogglePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMWhitelistTogglePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public PMWhitelistTogglePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) {
                var server = player.level().getServer();
                if (server != null) {
                    server.setUsingWhitelist(enabled);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        enabled ? "\u00a7aWhitelist enforcement enabled" : "\u00a7cWhitelist enforcement disabled"));
                    com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
                }
            }
        });
        // packet handled
    }
}
