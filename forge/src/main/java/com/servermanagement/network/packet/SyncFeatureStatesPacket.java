package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncFeatureStatesPacket(Map<String, Boolean> featureStates) implements IPacket {

    public SyncFeatureStatesPacket(FriendlyByteBuf buf) {
        this(readFeatureStates(buf));
    }

    private static Map<String, Boolean> readFeatureStates(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(32767), buf.readBoolean());
        }
        return map;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(featureStates.size());
        featureStates.forEach((key, value) -> {
            buf.writeUtf(key, 32767);
            buf.writeBoolean(value);
        });
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client thread
            com.servermanagement.features.FeatureManager.syncFeatureStates(featureStates);
        });
        ctx.setPacketHandled(true);
    }
}
