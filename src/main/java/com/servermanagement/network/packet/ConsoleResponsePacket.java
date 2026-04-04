package com.servermanagement.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Server-to-client packet that relays console command output
 */
public class ConsoleResponsePacket implements IPacket {
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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.ConsoleScreen consoleScreen) {
                consoleScreen.addConsoleLine(message);
            }
        });
        ctx.setPacketHandled(true);
    }
}
