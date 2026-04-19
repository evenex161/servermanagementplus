package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Server-to-client packet that syncs global settings (chat/tab isolation)
 */
public class SyncGlobalSettingsPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncGlobalSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_global_settings"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncGlobalSettingsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncGlobalSettingsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final boolean chatIsolationEnabled;
    private final boolean tabIsolationEnabled;

    public SyncGlobalSettingsPacket(boolean chatIsolationEnabled, boolean tabIsolationEnabled) {
        this.chatIsolationEnabled = chatIsolationEnabled;
        this.tabIsolationEnabled = tabIsolationEnabled;
    }

    public SyncGlobalSettingsPacket(FriendlyByteBuf buf) {
        this.chatIsolationEnabled = buf.readBoolean();
        this.tabIsolationEnabled = buf.readBoolean();
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
