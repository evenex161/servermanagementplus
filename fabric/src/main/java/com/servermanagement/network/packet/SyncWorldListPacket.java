package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncWorldListPacket(List<WorldInfo> worlds) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_world_list_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncWorldListPacket(FriendlyByteBuf buf) {
        this(decodeWorlds(buf));
    }

    private static List<WorldInfo> decodeWorlds(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<WorldInfo> worlds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            worlds.add(new WorldInfo(
                buf.readUtf(32767), buf.readUtf(32767),
                buf.readBoolean(), buf.readBoolean(), buf.readInt()));
        }
        return worlds;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(worlds.size());
        for (WorldInfo world : worlds) {
            buf.writeUtf(world.dimensionId, 32767);
            buf.writeUtf(world.name, 32767);
            buf.writeBoolean(world.netherPortalsEnabled);
            buf.writeBoolean(world.endPortalsEnabled);
            buf.writeInt(world.playerCount);
        }
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on client - update GUI
            com.servermanagement.client.ClientPacketHandler.handleWorldList(worlds);

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
