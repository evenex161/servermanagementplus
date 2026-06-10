package com.servermanagement.network.packet;

import com.servermanagement.security.SessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Client → Server: Authenticates the player session using the cached token.
 */
public record AuthenticateSessionPacket(String token) implements IPacket {
    public AuthenticateSessionPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                boolean authenticated = SessionManager.getInstance().authenticateSession(player.getUUID(), token);
                if (authenticated) {
                    com.mojang.logging.LogUtils.getLogger().debug("Player {} successfully authenticated mod session", player.getName().getString());
                } else {
                    com.mojang.logging.LogUtils.getLogger().warn("Player {} failed mod session authentication", player.getName().getString());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
