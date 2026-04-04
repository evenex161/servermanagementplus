package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client with gambling result
 */
public class GamblingResultPacket implements IPacket {
    private final boolean won;
    private final double payout;
    private final String message;
    
    public GamblingResultPacket(boolean won, double payout, String message) {
        this.won = won;
        this.payout = payout;
        this.message = message;
    }
    
    public GamblingResultPacket(FriendlyByteBuf buf) {
        this.won = buf.readBoolean();
        this.payout = buf.readDouble();
        this.message = buf.readUtf(256);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.won);
        buf.writeDouble(this.payout);
        buf.writeUtf(this.message, 256);
    }
    
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Update client-side screen
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) mc.screen;
                screen.handleGamblingResult(this.won, this.payout, this.message);
            }
        });
        ctx.setPacketHandled(true);
    }
}
