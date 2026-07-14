package com.servermanagement.network.packet;

import com.servermanagement.Constants;
import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import java.nio.file.Path;

import java.util.function.Supplier;

public record StartServerUpdatePacket(String downloadUrl) implements IPacket {
    
    public StartServerUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(512));
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(downloadUrl, 512);
    }
    
    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
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
        ctx.get().setPacketHandled(true);
    }
}
