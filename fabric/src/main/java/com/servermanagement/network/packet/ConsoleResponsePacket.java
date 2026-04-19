package com.servermanagement.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Server-to-client packet that relays console command output
 */
public class ConsoleResponsePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ConsoleResponsePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "console_response_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleResponsePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleResponsePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String message;

    public ConsoleResponsePacket(String message) {
        this.message = message;
    }

    public ConsoleResponsePacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf(4096);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(message, 4096);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.ConsoleScreen consoleScreen) {
                consoleScreen.addConsoleLine(message);
            }

}
}
