package com.servermanagement.network.packet;

import com.servermanagement.security.SessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server: Authenticates the player session using the cached token (Fabric).
 */
public record AuthenticateSessionPacket(String token) implements com.servermanagement.network.IPacket {
    public static final ResourceLocation ID = new ResourceLocation("servermanagement", "authenticate_session_packet");

    @Override
    public ResourceLocation id() { return ID; }

    public AuthenticateSessionPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token);
    }

    public void handle(net.minecraft.server.level.ServerPlayer player) {
        if (player != null) {
            boolean authenticated = SessionManager.getInstance().authenticateSession(player.getUUID(), token);
            if (authenticated) {
                com.mojang.logging.LogUtils.getLogger().debug("Player {} successfully authenticated mod session", player.getName().getString());
            } else {
                com.mojang.logging.LogUtils.getLogger().warn("Player {} failed mod session authentication", player.getName().getString());
            }
        }
    }
}
