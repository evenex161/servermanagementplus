package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Server-to-client packet that syncs global settings (chat/tab isolation)
 */
public record SyncGlobalSettingsPacket(boolean chatIsolationEnabled, boolean tabIsolationEnabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncGlobalSettingsPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "sync_global_settings"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncGlobalSettingsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncGlobalSettingsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncGlobalSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(chatIsolationEnabled);
        buf.writeBoolean(tabIsolationEnabled);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketHandler.handleGlobalSettings(chatIsolationEnabled, tabIsolationEnabled);
        });
        // packet handled
    }
}
