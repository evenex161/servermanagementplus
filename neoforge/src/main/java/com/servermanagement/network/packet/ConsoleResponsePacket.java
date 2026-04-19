package com.servermanagement.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Server-to-client packet that relays console command output
 */
public class ConsoleResponsePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConsoleResponsePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "console_response"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleResponsePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleResponsePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.ConsoleScreen consoleScreen) {
                consoleScreen.addConsoleLine(message);
            }
        });
        // packet handled
    }
}
