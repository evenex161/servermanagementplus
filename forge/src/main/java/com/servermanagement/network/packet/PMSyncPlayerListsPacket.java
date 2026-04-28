package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client packet that syncs the list of banned/whitelisted players.
 */
public record PMSyncPlayerListsPacket(List<String> bannedPlayers, List<String> whitelistedPlayers, boolean whitelistEnabled) implements IPacket {

    public PMSyncPlayerListsPacket(FriendlyByteBuf buf) {
        this(readStringList(buf, 16), readStringList(buf, 16), buf.readBoolean());
    }

    private static List<String> readStringList(FriendlyByteBuf buf, int maxLen) {
        int count = buf.readVarInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readUtf(maxLen));
        }
        return list;
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
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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
        ctx.get().setPacketHandled(true);
    }
}
