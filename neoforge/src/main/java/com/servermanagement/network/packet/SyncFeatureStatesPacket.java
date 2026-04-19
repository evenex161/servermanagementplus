package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record SyncFeatureStatesPacket(Map<String, Boolean> featureStates) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncFeatureStatesPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_feature_states"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncFeatureStatesPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncFeatureStatesPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncFeatureStatesPacket(FriendlyByteBuf buf) {
        this(decodeFeatureStates(buf));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(featureStates.size());
        featureStates.forEach((key, value) -> {
            buf.writeUtf(key, 64);
            buf.writeBoolean(value);
        });
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on client thread
            com.servermanagement.features.FeatureManager.syncFeatureStates(featureStates);
        });
        // packet handled
    }

    public Map<String, Boolean> getFeatureStates() {
        return featureStates;
    }

    private static Map<String, Boolean> decodeFeatureStates(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(64), buf.readBoolean());
        }
        return map;
    }
}
