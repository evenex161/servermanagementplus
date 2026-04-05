package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Server-to-client packet that relays console command output
 */
public class ConsoleResponsePacket implements IPacket {
    public static final CustomPacketPayload.Type<ConsoleResponsePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "console_response_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleResponsePacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleResponsePacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String message;

    public ConsoleResponsePacket(String message) {
        this.message = message;
    }

    public ConsoleResponsePacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf(4096);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(message, 4096);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.ConsoleScreen consoleScreen) {
                consoleScreen.addConsoleLine(message);
            }
        });
        
    }
}
