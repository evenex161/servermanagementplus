package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public record PMSpectatePlayerPacket(String playerName) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "p_m_spectate_player_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
    public PMSpectatePlayerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 32767);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.spectatePlayer(player, playerName);
            }

}
}
