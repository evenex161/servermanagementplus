package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.regex.Pattern;

public class PMBanPlayerPacket implements IPacket {
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

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
        buf.writeUtf(reason, 256);
        buf.writeBoolean(banIP);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.banPlayer(player, playerName, reason, banIP);
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
