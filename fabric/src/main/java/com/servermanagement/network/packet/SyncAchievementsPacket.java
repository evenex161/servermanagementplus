package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Packet to sync achievements from server to client
 */
public record SyncAchievementsPacket(Set<String> earnedAchievements, int totalRewards) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_achievements_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncAchievementsPacket(FriendlyByteBuf buf) {
        this(decodeAchievements(buf), buf.readInt());
    }

    private static Set<String> decodeAchievements(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(buf.readUtf(32767));
        }
        return set;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(earnedAchievements.size());
        
        for (String achievementId : earnedAchievements) {
            buf.writeUtf(achievementId, 32767);
        }
        
        buf.writeInt(totalRewards);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Store achievements on client side for GUI display
            com.servermanagement.client.ClientAchievementsData.setEarnedAchievements(earnedAchievements);
            com.servermanagement.client.ClientAchievementsData.setTotalRewardsEarned(totalRewards);
            com.servermanagement.client.ClientScreenManager.refreshOpenScreen();

}
}
