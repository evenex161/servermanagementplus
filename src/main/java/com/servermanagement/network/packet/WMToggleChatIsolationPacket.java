package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class WMToggleChatIsolationPacket implements IPacket {
    public static final CustomPacketPayload.Type<WMToggleChatIsolationPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "w_m_toggle_chat_isolation_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMToggleChatIsolationPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMToggleChatIsolationPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final boolean enabled;
    private final long clientTick;

    public WMToggleChatIsolationPacket(boolean enabled, long clientTick) {
        this.enabled = enabled;
        this.clientTick = clientTick;
    }

    public WMToggleChatIsolationPacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
        this.clientTick = buf.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "chat_isolation";
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setChatIsolationEnabled(enabled);
                }
            }
        });
        
    }
}
