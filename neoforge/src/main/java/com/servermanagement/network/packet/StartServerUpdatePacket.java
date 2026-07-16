package com.servermanagement.network.packet;

import com.servermanagement.Constants;
import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.nio.file.Path;

import java.util.function.Supplier;

public record StartServerUpdatePacket(String downloadUrl, boolean overrideScripts) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartServerUpdatePacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "startserverupdate_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, StartServerUpdatePacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), StartServerUpdatePacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public StartServerUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(512), buf.readBoolean());
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(downloadUrl, 512);
        buf.writeBoolean(overrideScripts);
    }
    
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((ctx.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) ctx.player() : null);
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
        // packet handled
    }
}





