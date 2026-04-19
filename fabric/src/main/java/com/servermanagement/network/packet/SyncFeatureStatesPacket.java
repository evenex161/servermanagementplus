package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncFeatureStatesPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncFeatureStatesPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_feature_states_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncFeatureStatesPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncFeatureStatesPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(featureStates.size());
        featureStates.forEach((key, value) -> {
            buf.writeUtf(key, 64);
            buf.writeBoolean(value);
        });
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on client thread
            com.servermanagement.features.FeatureManager.syncFeatureStates(featureStates);

}

    public Map<String, Boolean> getFeatureStates() {
        return featureStates;
    }
}
