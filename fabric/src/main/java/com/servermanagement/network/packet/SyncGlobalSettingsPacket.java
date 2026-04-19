package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs global settings (chat/tab isolation)
 */
public class SyncGlobalSettingsPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncGlobalSettingsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_global_settings_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncGlobalSettingsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncGlobalSettingsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientPacketHandler.handleGlobalSettings(chatIsolationEnabled, tabIsolationEnabled);

}
}
