package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client with gambling result
 */
public class GamblingResultPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<GamblingResultPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "gambling_result_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, GamblingResultPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), GamblingResultPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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
