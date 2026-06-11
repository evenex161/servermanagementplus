package com.servermanagement.network.packet;

import com.servermanagement.gui.gambling.MineStacksMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

/**
 * Packet to sync MineStacks betting slot state from client to server
 */
public record SyncBettingSlotStatePacket(boolean bettingSlotActive) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncBettingSlotStatePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "sync_betting_slot_state_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBettingSlotStatePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBettingSlotStatePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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
