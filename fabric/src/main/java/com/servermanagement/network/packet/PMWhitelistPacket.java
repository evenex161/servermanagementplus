package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.regex.Pattern;

public record PMWhitelistPacket(String playerName, boolean add) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "p_m_whitelist_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");// true = add to whitelist, false = remove
    public PMWhitelistPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 32767);
        buf.writeBoolean(add);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
