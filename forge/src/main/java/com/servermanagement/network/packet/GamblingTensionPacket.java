package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to trigger gambling tension animation
 * Sent immediately when bet is placed, before the actual result
 */
public record GamblingTensionPacket(GameType gameType, String gameOption) implements IPacket {
    
    public enum GameType {
        COIN_FLIP,
        DICE_ROLL,
        SLOT_MACHINE,
        ROULETTE
    }
    
    public GamblingTensionPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(GameType.class), buf.readUtf(32767));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.gameType);
        buf.writeUtf(this.gameOption, 32767);
    }
    
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // This will be handled on the client side
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen instanceof com.servermanagement.gui.gambling.MineStacksScreen) {
                com.servermanagement.gui.gambling.MineStacksScreen screen = 
                    (com.servermanagement.gui.gambling.MineStacksScreen) minecraft.screen;
                screen.startTension(this.gameType, this.gameOption);
            }
        });
        ctx.setPacketHandled(true);
    }
}
