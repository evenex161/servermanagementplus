package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncFeatureStatesPacket(Map<String, Boolean> featureStates) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_feature_states_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncFeatureStatesPacket(FriendlyByteBuf buf) {
        this(decodeFeatureStates(buf));
    }

    private static Map<String, Boolean> decodeFeatureStates(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(32767), buf.readBoolean());
        }
        return map;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(featureStates.size());
        featureStates.forEach((key, value) -> {
            buf.writeUtf(key, 32767);
            buf.writeBoolean(value);
        });
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on client thread
            com.servermanagement.features.FeatureManager.syncFeatureStates(featureStates);
            com.servermanagement.client.ClientPacketHandler.refreshOpenScreen();

}

    public Map<String, Boolean> getFeatureStates() {
        return featureStates;
    }
}
