package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PMViewInventoryPacket implements IPacket {
    private final String playerName;

    public PMViewInventoryPacket(String playerName) {
        this.playerName = playerName;
    }

    public PMViewInventoryPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf(16);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !playerName.matches("[a-zA-Z0-9_]+")) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.viewInventory(player, playerName);
            }
        });
        ctx.setPacketHandled(true);
    }
}
