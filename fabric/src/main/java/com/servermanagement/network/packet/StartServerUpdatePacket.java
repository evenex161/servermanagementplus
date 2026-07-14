package com.servermanagement.network.packet;

import com.servermanagement.Constants;
import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import com.servermanagement.network.IPacket;

public record StartServerUpdatePacket(String downloadUrl) implements IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "start_server_update_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }
    
    public StartServerUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(512));
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(downloadUrl, 512);
    }
    
    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
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
