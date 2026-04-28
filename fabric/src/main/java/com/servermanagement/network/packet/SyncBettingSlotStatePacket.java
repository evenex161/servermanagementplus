package com.servermanagement.network.packet;

import com.servermanagement.gui.gambling.MineStacksMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

/**
 * Packet to sync MineStacks betting slot state from client to server
 */
public record SyncBettingSlotStatePacket(boolean bettingSlotActive) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_betting_slot_state_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncBettingSlotStatePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(bettingSlotActive);
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.containerMenu instanceof MineStacksMenu) {
                MineStacksMenu menu = (MineStacksMenu) player.containerMenu;
                menu.setBettingSlotActive(bettingSlotActive);
            }

}
}
