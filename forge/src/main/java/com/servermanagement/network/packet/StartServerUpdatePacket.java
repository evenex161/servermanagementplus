package com.servermanagement.network.packet;

import com.servermanagement.Constants;
import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import java.nio.file.Path;

import java.util.function.Supplier;

public record StartServerUpdatePacket(String downloadUrl, boolean overrideScripts) implements IPacket {
    
    public StartServerUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean());
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(downloadUrl, 32767);
        buf.writeBoolean(overrideScripts);
    }
    
    @Override
    public void handle(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                if (overrideScripts) {
                    java.nio.file.Path serverRoot = java.nio.file.Paths.get("").toAbsolutePath();
                    com.servermanagement.updater.StartScriptGenerator.overrideRunScripts(serverRoot);
                }
                
                // Kick all players
                player.server.getPlayerList().getPlayers().forEach(p -> 
                    p.connection.disconnect(Component.literal("Server restarting for OTA update!"))
                );
                
                Path currentJar = ModList.get().getModFileById(Constants.MOD_ID).getFile().getFilePath();
                UpdateManager.downloadAndHandoff(downloadUrl, false, currentJar, null, () -> {
                    player.server.halt(false);
                }, null);
            }
        });
        ctx.setPacketHandled(true);
    }
}


