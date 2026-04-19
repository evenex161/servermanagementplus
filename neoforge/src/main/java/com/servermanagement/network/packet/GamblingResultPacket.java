package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from server to client with gambling result
 */
public class GamblingResultPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GamblingResultPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "gambling_result"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, GamblingResultPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), GamblingResultPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

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
    
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update client-side screen
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) mc.screen;
                screen.handleGamblingResult(this.won, this.payout, this.message);
            }
        });
        // packet handled
    }
}
