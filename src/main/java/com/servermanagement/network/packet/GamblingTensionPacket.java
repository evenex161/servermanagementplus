package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to trigger gambling tension animation
 * Sent immediately when bet is placed, before the actual result
 */
public class GamblingTensionPacket implements IPacket {
    public static final CustomPacketPayload.Type<GamblingTensionPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "gambling_tension_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, GamblingTensionPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), GamblingTensionPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final GameType gameType;
    private final String gameOption; // For specific animations (e.g., which roulette bet)
    
    public enum GameType {
        COIN_FLIP,
        DICE_ROLL,
        SLOT_MACHINE,
        ROULETTE
    }
    
    public GamblingTensionPacket(GameType gameType, String gameOption) {
        this.gameType = gameType;
        this.gameOption = gameOption;
    }
    
    public GamblingTensionPacket(FriendlyByteBuf buf) {
        this.gameType = buf.readEnum(GameType.class);
        this.gameOption = buf.readUtf(64);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.gameType);
        buf.writeUtf(this.gameOption, 64);
    }
    
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // This will be handled on the client side
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) minecraft.screen;
                screen.startTension(this.gameType, this.gameOption);
            }
        });
        
    }
}
