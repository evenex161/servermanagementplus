package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.regex.Pattern;

public class PMBanPlayerPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<PMBanPlayerPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "p_m_ban_player_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, PMBanPlayerPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMBanPlayerPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
    private final String playerName;
    private final String reason;
    private final boolean banIP;

    public PMBanPlayerPacket(String playerName, String reason, boolean banIP) {
        this.playerName = playerName;
        this.reason = reason;
        this.banIP = banIP;
    }

    public PMBanPlayerPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf(16);
        this.reason = buf.readUtf(256);
        this.banIP = buf.readBoolean();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
        buf.writeUtf(reason, 256);
        buf.writeBoolean(banIP);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.banPlayer(player, playerName, reason, banIP);
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }

}
}
