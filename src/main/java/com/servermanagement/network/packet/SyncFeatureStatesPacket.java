package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class SyncFeatureStatesPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncFeatureStatesPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_feature_states_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncFeatureStatesPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncFeatureStatesPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client thread
            com.servermanagement.features.FeatureManager.syncFeatureStates(featureStates);
        });
        
    }

    public Map<String, Boolean> getFeatureStates() {
        return featureStates;
    }
}
