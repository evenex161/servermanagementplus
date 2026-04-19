package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Packet to sync achievements from server to client
 */
public class SyncAchievementsPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncAchievementsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_achievements"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAchievementsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAchievementsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final Set<String> earnedAchievements;
    private final int totalRewards;

    public SyncAchievementsPacket(Set<String> earnedAchievements, int totalRewards) {
        this.earnedAchievements = earnedAchievements;
        this.totalRewards = totalRewards;
    }

    public SyncAchievementsPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.earnedAchievements = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            this.earnedAchievements.add(buf.readUtf(128));
        }
        
        this.totalRewards = buf.readInt();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(earnedAchievements.size());
        
        for (String achievementId : earnedAchievements) {
            buf.writeUtf(achievementId, 128);
        }
        
        buf.writeInt(totalRewards);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store achievements on client side for GUI display
            com.servermanagement.client.ClientAchievementsData.setEarnedAchievements(earnedAchievements);
            com.servermanagement.client.ClientAchievementsData.setTotalRewardsEarned(totalRewards);
        });
        // packet handled
    }
}
