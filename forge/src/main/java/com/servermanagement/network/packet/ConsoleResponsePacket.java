package com.servermanagement.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

/**
 * Server-to-client packet that relays console command output
 */
public record ConsoleResponsePacket(String message) implements IPacket {

    public ConsoleResponsePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(4096));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(message, 4096);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.ConsoleScreen consoleScreen) {
                consoleScreen.addConsoleLine(message);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
