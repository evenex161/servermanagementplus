package com.servermanagement.network.packet;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
/**
 * Packet for executing console commands from the in-game GUI
 */
public record ConsoleCommandPacket(String command) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ConsoleCommandPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "console_command_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleCommandPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleCommandPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public ConsoleCommandPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(command, 256);
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
