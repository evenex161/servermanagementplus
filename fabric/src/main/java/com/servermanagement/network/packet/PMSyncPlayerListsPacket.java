package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client packet that syncs the list of banned/whitelisted players.
 */
public class PMSyncPlayerListsPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<PMSyncPlayerListsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "p_m_sync_player_lists_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, PMSyncPlayerListsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMSyncPlayerListsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<String> bannedPlayers;
    private final List<String> whitelistedPlayers;
    private final boolean whitelistEnabled;

    public PMSyncPlayerListsPacket(List<String> bannedPlayers, List<String> whitelistedPlayers, boolean whitelistEnabled) {
        this.bannedPlayers = bannedPlayers;
        this.whitelistedPlayers = whitelistedPlayers;
        this.whitelistEnabled = whitelistEnabled;
    }

    public PMSyncPlayerListsPacket(FriendlyByteBuf buf) {
        int banCount = buf.readVarInt();
        this.bannedPlayers = new ArrayList<>(banCount);
        for (int i = 0; i < banCount; i++) {
            this.bannedPlayers.add(buf.readUtf(16));
        }
        int whiteCount = buf.readVarInt();
        this.whitelistedPlayers = new ArrayList<>(whiteCount);
        for (int i = 0; i < whiteCount; i++) {
            this.whitelistedPlayers.add(buf.readUtf(16));
        }
        this.whitelistEnabled = buf.readBoolean();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(bannedPlayers.size());
        for (String name : bannedPlayers) {
            buf.writeUtf(name, 16);
        }
        buf.writeVarInt(whitelistedPlayers.size());
        for (String name : whitelistedPlayers) {
            buf.writeUtf(name, 16);
        }
        buf.writeBoolean(whitelistEnabled);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Client-side handling - store the data for the Player Manager screen
            com.servermanagement.features.playermanager.PlayerManagerClientData.setBannedPlayers(bannedPlayers);
            com.servermanagement.features.playermanager.PlayerManagerClientData.setWhitelistedPlayers(whitelistedPlayers);
            com.servermanagement.features.playermanager.PlayerManagerClientData.setWhitelistEnabled(whitelistEnabled);
            
            // Trigger UI refresh if Player Manager screen is open.
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.PlayerManagerScreen pms) {
                pms.refreshFromSync();
            }
    }

    public List<String> getBannedPlayers() { return bannedPlayers; }
    public List<String> getWhitelistedPlayers() { return whitelistedPlayers; }
    public boolean isWhitelistEnabled() { return whitelistEnabled; }
}
