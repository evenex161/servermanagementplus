package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → Client: Transmits the generated session token to the client (Fabric).
 */
public record SyncSessionTokenPacket(String token) implements com.servermanagement.network.IPacket {
    public static final ResourceLocation ID = new ResourceLocation("servermanagement", "sync_session_token_packet");

    @Override
    public ResourceLocation id() { return ID; }

    public SyncSessionTokenPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token);
    }

    public void handle(net.minecraft.server.level.ServerPlayer player) {
        ClientPacketHandler.handleSessionTokenSync(token);
    }
}
