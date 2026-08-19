package com.servermanagement.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Server-to-client packet that relays console command output
 */
public record ConsoleResponsePacket(String message) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "console_response_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public ConsoleResponsePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(message, 32767);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.ConsoleScreen consoleScreen) {
                consoleScreen.addConsoleLine(message);
            }

}
}
