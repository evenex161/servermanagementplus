package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client-to-server packet to toggle whitelist enforcement on/off.
 */
public class PMWhitelistTogglePacket implements IPacket {
    private final boolean enabled;

    public PMWhitelistTogglePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public PMWhitelistTogglePacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                var server = player.getServer();
                if (server != null) {
                    server.getPlayerList().setUsingWhiteList(enabled);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        enabled ? "\u00a7aWhitelist enforcement enabled" : "\u00a7cWhitelist enforcement disabled"));
                    com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
