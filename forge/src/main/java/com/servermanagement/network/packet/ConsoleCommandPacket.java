package com.servermanagement.network.packet;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

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
                    // Use the PLAYER's command source stack — not the server's.
                    // server.createCommandSourceStack() has permission level 4 (console),
                    // which would let OP2 players run /stop, /op, etc. The player's
                    // source stack respects their actual permission level.
                    CommandSourceStack source = player.createCommandSourceStack()
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
