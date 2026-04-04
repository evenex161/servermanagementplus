package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.server.ModFileTransferManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to request the mod JAR file for OTA update.
 */
public class ModFileRequestPacket implements IPacket {
    private final String requestedVersion;
    private final String clientVersion;
    
    public ModFileRequestPacket(String requestedVersion, String clientVersion) {
        this.requestedVersion = requestedVersion;
        this.clientVersion = clientVersion;
    }
    
    public ModFileRequestPacket(FriendlyByteBuf buf) {
        this.requestedVersion = buf.readUtf(64);
        this.clientVersion = buf.readUtf(64);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(requestedVersion, 64);
        buf.writeUtf(clientVersion, 64);
    }
    
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerManagementMod.LOGGER.info("Player {} requested mod update from {} to {}", 
                    player.getName().getString(), clientVersion, requestedVersion);
                
                // Start file transfer on server side
                ModFileTransferManager.startTransfer(player, requestedVersion);
            }
        });
        context.setPacketHandled(true);
    }
    
    public String getRequestedVersion() {
        return requestedVersion;
    }
    
    public String getClientVersion() {
        return clientVersion;
    }
}
