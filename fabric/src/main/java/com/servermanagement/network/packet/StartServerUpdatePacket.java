package com.servermanagement.network.packet;

import com.servermanagement.Constants;
import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record StartServerUpdatePacket(String downloadUrl, boolean overrideScripts) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<StartServerUpdatePacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "start_server_update_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, StartServerUpdatePacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), StartServerUpdatePacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public StartServerUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean());
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(downloadUrl, 32767);
        buf.writeBoolean(overrideScripts);
    }
    
    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            if (overrideScripts) {
                java.nio.file.Path serverRoot = java.nio.file.Paths.get("").toAbsolutePath();
                com.servermanagement.updater.StartScriptGenerator.overrideRunScripts(serverRoot);
            }
            
            // Kick all players
            player.server.getPlayerList().getPlayers().forEach(p -> 
                p.connection.disconnect(Component.literal("Server restarting for OTA update!"))
            );
            
            Path currentJar = FabricLoader.getInstance().getModContainer(Constants.MOD_ID).get().getOrigin().getPaths().get(0);
            UpdateManager.downloadAndHandoff(downloadUrl, false, currentJar, null, () -> {
                player.server.halt(false);
            }, null);
        }
    }
}

