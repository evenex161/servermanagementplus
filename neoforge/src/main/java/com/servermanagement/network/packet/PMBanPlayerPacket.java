package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.regex.Pattern;

public record PMBanPlayerPacket(String playerName, String reason, boolean banIP) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PMBanPlayerPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "p_m_ban_player"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMBanPlayerPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMBanPlayerPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");


    public PMBanPlayerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(16), buf.readUtf(256), buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
        buf.writeUtf(reason, 256);
        buf.writeBoolean(banIP);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.banPlayer(player, playerName, reason, banIP);
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }
        });
        // packet handled
    }
}
