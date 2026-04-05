package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.network.ModNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet for executing console commands from the in-game GUI
 */
public class ConsoleCommandPacket implements IPacket {
    public static final CustomPacketPayload.Type<ConsoleCommandPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "console_command_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleCommandPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleCommandPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String command;
    
    public ConsoleCommandPacket(String command) {
        this.command = command;
    }
    
    public ConsoleCommandPacket(FriendlyByteBuf buf) {
        this.command = buf.readUtf(256);
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(command, 256);
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                var server = player.getServer();
                if (server != null) {
                    // Create a command source that captures output
                    CommandSourceStack source = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withSource(new com.servermanagement.network.ConsoleCommandListener(player));
                    
                    server.getCommands().performPrefixedCommand(source, command);
                    
                    com.servermanagement.ServerManagementMod.LOGGER.info(
                        "Console command executed by {}: {}", player.getName().getString(), 
                        command.replace("\n", "").replace("\r", "")
                    );
                }
            }
        });
        
    }
}
