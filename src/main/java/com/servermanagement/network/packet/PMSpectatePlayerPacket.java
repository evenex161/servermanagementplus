package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PMSpectatePlayerPacket implements IPacket {
    private final String playerName;

    public PMSpectatePlayerPacket(String playerName) {
        this.playerName = playerName;
    }

    public PMSpectatePlayerPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf(16);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !playerName.matches("[a-zA-Z0-9_]+")) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.spectatePlayer(player, playerName);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
