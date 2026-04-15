package com.servermanagement.network.packet;

import com.servermanagement.network.ModNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet for executing console commands from the in-game GUI
 */
public class ConsoleCommandPacket implements IPacket {
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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                var server = player.getServer();
                if (server != null) {
                    // Create a command source that captures output
                    CommandSourceStack source = server.createCommandSourceStack()
                        .withSource(new com.servermanagement.network.ConsoleCommandListener(player));
                    
                    server.getCommands().performPrefixedCommand(source, command);
                    
                    com.servermanagement.ServerManagementMod.LOGGER.info(
                        "Console command executed by {}: {}", player.getName().getString(), 
                        command.replace("\n", "").replace("\r", "")
                    );
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
