package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Packet to sync achievements from server to client
 */
public record SyncAchievementsPacket(Set<String> earnedAchievements, int totalRewards) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncAchievementsPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "sync_achievements"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAchievementsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAchievementsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncAchievementsPacket(FriendlyByteBuf buf) {
        this(decodeEarnedAchievements(buf), buf.readInt());
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

    private static Set<String> decodeEarnedAchievements(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(buf.readUtf(128));
        }
        return set;
    }
}
