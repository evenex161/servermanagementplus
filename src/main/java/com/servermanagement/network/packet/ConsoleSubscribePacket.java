package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet to subscribe/unsubscribe from server log streaming.
 */
public class ConsoleSubscribePacket implements IPacket {
    public static final CustomPacketPayload.Type<ConsoleSubscribePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "console_subscribe"));

    public static final StreamCodec<FriendlyByteBuf, ConsoleSubscribePacket> STREAM_CODEC =
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleSubscribePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final boolean subscribe;

    public ConsoleSubscribePacket(boolean subscribe) {
        this.subscribe = subscribe;
    }

    public ConsoleSubscribePacket(FriendlyByteBuf buf) {
        this.subscribe = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(subscribe);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                var manager = com.servermanagement.server.ServerConsoleManager.getInstance();
                if (subscribe) {
                    manager.subscribe(player);
                } else {
                    manager.unsubscribe(player.getUUID());
                }
            }
        });
    }
}
