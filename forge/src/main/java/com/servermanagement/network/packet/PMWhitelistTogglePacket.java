package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Client-to-server packet to toggle whitelist enforcement on/off.
 */
public record PMWhitelistTogglePacket(boolean enabled) implements IPacket {

    public PMWhitelistTogglePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
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
        ctx.get().setPacketHandled(true);
    }
}
