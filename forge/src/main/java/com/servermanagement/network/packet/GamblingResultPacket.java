package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

/**
 * Packet sent from server to client with gambling result
 */
public record GamblingResultPacket(boolean won, double payout, String message) implements IPacket {
    
    public GamblingResultPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readDouble(), buf.readUtf(32767));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.won);
        buf.writeDouble(this.payout);
        buf.writeUtf(this.message, 32767);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            // Update client-side screen
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) mc.screen;
                screen.handleGamblingResult(this.won, this.payout, this.message);
            }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
