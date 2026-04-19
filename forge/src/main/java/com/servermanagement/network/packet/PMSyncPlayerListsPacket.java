package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client packet that syncs the list of banned/whitelisted players.
 */
public class PMSyncPlayerListsPacket implements IPacket {
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

    @Override
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

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Client-side handling - store the data for the Player Manager screen
            com.servermanagement.features.playermanager.PlayerManagerClientData.setBannedPlayers(bannedPlayers);
            com.servermanagement.features.playermanager.PlayerManagerClientData.setWhitelistedPlayers(whitelistedPlayers);
            com.servermanagement.features.playermanager.PlayerManagerClientData.setWhitelistEnabled(whitelistEnabled);
            
            // Trigger UI refresh if Player Manager screen is open.
            // Use DistExecutor double-lambda to safely isolate client-only class references
            // (Minecraft, PlayerManagerScreen) from server-side classloading.
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.screen instanceof com.servermanagement.gui.screen.PlayerManagerScreen pms) {
                        pms.refreshFromSync();
                    }
                }
            );
        });
        ctx.setPacketHandled(true);
    }

    public List<String> getBannedPlayers() { return bannedPlayers; }
    public List<String> getWhitelistedPlayers() { return whitelistedPlayers; }
    public boolean isWhitelistEnabled() { return whitelistEnabled; }
}
