package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class SyncWorldListPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncWorldListPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_world_list"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncWorldListPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncWorldListPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final List<WorldInfo> worlds;

    public SyncWorldListPacket(List<WorldInfo> worlds) {
        this.worlds = worlds;
    }

    public SyncWorldListPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.worlds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            worlds.add(new WorldInfo(
                buf.readUtf(256),
                buf.readUtf(128),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt()
            ));
        }
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(worlds.size());
        for (WorldInfo world : worlds) {
            buf.writeUtf(world.dimensionId, 256);
            buf.writeUtf(world.name, 128);
            buf.writeBoolean(world.netherPortalsEnabled);
            buf.writeBoolean(world.endPortalsEnabled);
            buf.writeInt(world.playerCount);
        }
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on client - update GUI
            com.servermanagement.client.ClientPacketHandler.handleWorldList(worlds);
        });
        // packet handled
    }

    public List<WorldInfo> getWorlds() {
        return worlds;
    }

    public static class WorldInfo {
        public final String dimensionId;
        public final String name;
        public final boolean netherPortalsEnabled;
        public final boolean endPortalsEnabled;
        public final int playerCount;

        public WorldInfo(String dimensionId, String name, boolean netherPortalsEnabled, boolean endPortalsEnabled, int playerCount) {
            this.dimensionId = dimensionId;
            this.name = name;
            this.netherPortalsEnabled = netherPortalsEnabled;
            this.endPortalsEnabled = endPortalsEnabled;
            this.playerCount = playerCount;
        }

        public boolean areAllPortalsEnabled() {
            return netherPortalsEnabled && endPortalsEnabled;
        }
    }
}
