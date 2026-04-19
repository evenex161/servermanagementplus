package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to trigger gambling tension animation
 * Sent immediately when bet is placed, before the actual result
 */
public record GamblingTensionPacket(GameType gameType, String gameOption) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<GamblingTensionPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "gambling_tension_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, GamblingTensionPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), GamblingTensionPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }// For specific animations (e.g., which roulette bet)
    
    public enum GameType {
        COIN_FLIP,
        DICE_ROLL,
        SLOT_MACHINE,
        ROULETTE
    }
    public GamblingTensionPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(GameType.class), buf.readUtf(64));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.gameType);
        buf.writeUtf(this.gameOption, 64);
    }
    
    public void handle(net.minecraft.server.level.ServerPlayer player) {
            // This will be handled on the client side
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) minecraft.screen;
                screen.startTension(this.gameType, this.gameOption);
            }

}
}
