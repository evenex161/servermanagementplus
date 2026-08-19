package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client with gambling result
 */
public record GamblingResultPacket(boolean won, double payout, String message) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "gambling_result_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public GamblingResultPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readDouble(), buf.readUtf(32767));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.won);
        buf.writeDouble(this.payout);
        buf.writeUtf(this.message, 32767);
    }
    
    public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Update client-side screen
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) mc.screen;
                screen.handleGamblingResult(this.won, this.payout, this.message);
            }

}
}
