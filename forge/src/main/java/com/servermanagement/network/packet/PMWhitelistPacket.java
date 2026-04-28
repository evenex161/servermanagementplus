package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.regex.Pattern;

public record PMWhitelistPacket(String playerName, boolean add) implements IPacket {
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    public PMWhitelistPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(16), buf.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
        buf.writeBoolean(add);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
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
        ctx.get().setPacketHandled(true);
    }
}
