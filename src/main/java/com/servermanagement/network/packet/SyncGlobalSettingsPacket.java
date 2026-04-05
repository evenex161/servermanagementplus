package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs global settings (chat/tab isolation)
 */
public class SyncGlobalSettingsPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncGlobalSettingsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_global_settings_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncGlobalSettingsPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncGlobalSettingsPacket::new);
    
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

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(chatIsolationEnabled);
        buf.writeBoolean(tabIsolationEnabled);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleGlobalSettings(chatIsolationEnabled, tabIsolationEnabled);
        });
        
    }
}
