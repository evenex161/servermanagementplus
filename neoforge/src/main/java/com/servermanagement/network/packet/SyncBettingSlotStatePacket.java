package com.servermanagement.network.packet;

import com.servermanagement.gui.gambling.MineStacksMenu;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet to sync MineStacks betting slot state from client to server
 */
public record SyncBettingSlotStatePacket(boolean bettingSlotActive) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncBettingSlotStatePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_betting_slot_state"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBettingSlotStatePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBettingSlotStatePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public SyncBettingSlotStatePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(bettingSlotActive);
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.containerMenu instanceof MineStacksMenu) {
                MineStacksMenu menu = (MineStacksMenu) player.containerMenu;
                menu.setBettingSlotActive(bettingSlotActive);
            }
        });
        // packet handled
    }
}
