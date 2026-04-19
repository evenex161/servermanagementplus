package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.regex.Pattern;

public record PMSpectatePlayerPacket(String playerName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PMSpectatePlayerPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "p_m_spectate_player"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMSpectatePlayerPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMSpectatePlayerPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");


    public PMSpectatePlayerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(16));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.spectatePlayer(player, playerName);
            }
        });
        // packet handled
    }
}
