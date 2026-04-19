package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client-to-server packet requesting the banned/whitelisted player lists.
 */
public class PMRequestPlayerListsPacket implements IPacket {

    public PMRequestPlayerListsPacket() {}

    public PMRequestPlayerListsPacket(FriendlyByteBuf buf) {
        // No data needed
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        // No data needed
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
