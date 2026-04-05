package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.gui.gambling.MineStacksMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet to sync MineStacks betting slot state from client to server
 */
public class SyncBettingSlotStatePacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncBettingSlotStatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_betting_slot_state_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBettingSlotStatePacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBettingSlotStatePacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.containerMenu instanceof MineStacksMenu) {
                MineStacksMenu menu = (MineStacksMenu) player.containerMenu;
                menu.setBettingSlotActive(bettingSlotActive);
            }
        });
        
    }
}
