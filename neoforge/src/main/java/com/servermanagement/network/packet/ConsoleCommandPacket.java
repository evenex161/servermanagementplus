package com.servermanagement.network.packet;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet for executing console commands from the in-game GUI
 */
public class ConsoleCommandPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConsoleCommandPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "console_command"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleCommandPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleCommandPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final String command;
    
    public ConsoleCommandPacket(String command) {
        this.command = command;
    }
    
    public ConsoleCommandPacket(FriendlyByteBuf buf) {
        this.command = buf.readUtf(256);
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(command, 256);
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
        // packet handled
    }
}
