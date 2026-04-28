package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncFeatureStatesPacket implements IPacket {
    private final Map<String, Boolean> featureStates;

    public SyncFeatureStatesPacket(Map<String, Boolean> featureStates) {
        this.featureStates = featureStates;
    }

    public SyncFeatureStatesPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.featureStates = new HashMap<>();
        for (int i = 0; i < size; i++) {
            featureStates.put(buf.readUtf(64), buf.readBoolean());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(featureStates.size());
        featureStates.forEach((key, value) -> {
            buf.writeUtf(key, 64);
            buf.writeBoolean(value);
        });
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client thread
            com.servermanagement.features.FeatureManager.syncFeatureStates(featureStates);
        });
        ctx.get().setPacketHandled(true);
    }

    public Map<String, Boolean> getFeatureStates() {
        return featureStates;
    }
}
