package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncWorldListPacket(List<WorldInfo> worlds) implements IPacket {

    public SyncWorldListPacket(FriendlyByteBuf buf) {
        this(readWorlds(buf));
    }

    private static List<WorldInfo> readWorlds(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<WorldInfo> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new WorldInfo(
                buf.readUtf(32767),
                buf.readUtf(32767),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt()
            ));
        }
        return list;
    }

    @Override
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

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client - update GUI
            com.servermanagement.client.ClientPacketHandler.handleWorldList(worlds);
        });
        ctx.setPacketHandled(true);
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
