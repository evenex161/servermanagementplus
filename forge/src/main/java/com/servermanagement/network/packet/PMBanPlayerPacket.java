package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.regex.Pattern;

public record PMBanPlayerPacket(String playerName, String reason, boolean banIP) implements IPacket {
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    public PMBanPlayerPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readUtf(32767), buf.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 32767);
        buf.writeUtf(reason, 32767);
        buf.writeBoolean(banIP);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.banPlayer(player, playerName, reason, banIP);
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
