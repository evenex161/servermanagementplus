package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Packet to sync achievements from server to client
 */
public class SyncAchievementsPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncAchievementsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_achievements_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAchievementsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAchievementsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Store achievements on client side for GUI display
            com.servermanagement.client.ClientAchievementsData.setEarnedAchievements(earnedAchievements);
            com.servermanagement.client.ClientAchievementsData.setTotalRewardsEarned(totalRewards);

}
}
