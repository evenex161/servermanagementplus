package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.regex.Pattern;

public record PMWhitelistPacket(String playerName, boolean add) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PMWhitelistPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "p_m_whitelist"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMWhitelistPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMWhitelistPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");// true = add to whitelist, false = remove


    public PMWhitelistPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 32767);
        buf.writeBoolean(add);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                if (add) {
                    com.servermanagement.features.playermanager.PlayerManagerSingleton.addToWhitelist(player, playerName);
                } else {
                    com.servermanagement.features.playermanager.PlayerManagerSingleton.removeFromWhitelist(player, playerName);
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }
        });
        // packet handled
    }
}
