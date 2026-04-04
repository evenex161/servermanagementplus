package com.servermanagement.network.packet;

import com.servermanagement.gui.gambling.MineStacksMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet to sync MineStacks betting slot state from client to server
 */
public class SyncBettingSlotStatePacket implements IPacket {
    private final boolean bettingSlotActive;
    
    public SyncBettingSlotStatePacket(boolean bettingSlotActive) {
        this.bettingSlotActive = bettingSlotActive;
    }
    
    public SyncBettingSlotStatePacket(FriendlyByteBuf buf) {
        this.bettingSlotActive = buf.readBoolean();
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(bettingSlotActive);
    }
    
    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof MineStacksMenu) {
                MineStacksMenu menu = (MineStacksMenu) player.containerMenu;
                menu.setBettingSlotActive(bettingSlotActive);
            }
        });
        ctx.setPacketHandled(true);
    }
}
