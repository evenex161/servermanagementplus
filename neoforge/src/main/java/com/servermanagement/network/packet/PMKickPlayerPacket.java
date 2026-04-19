package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.regex.Pattern;

public class PMKickPlayerPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PMKickPlayerPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "p_m_kick_player"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMKickPlayerPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMKickPlayerPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
    private final String playerName;
    private final String reason;

    public PMKickPlayerPacket(String playerName, String reason) {
        this.playerName = playerName;
        this.reason = reason;
    }

    public PMKickPlayerPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf(16);
        this.reason = buf.readUtf(256);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
        buf.writeUtf(reason, 256);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.kickPlayer(player, playerName, reason);
            }
        });
        // packet handled
    }
}
