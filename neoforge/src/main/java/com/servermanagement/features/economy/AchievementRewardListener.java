package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.network.chat.Component;

/**
 * Listens for achievement/advancement events and rewards players with money.
 * Works with all mods - any advancement from any source.
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class AchievementRewardListener {

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        // Check if Economy feature is enabled
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        AdvancementHolder holder = event.getAdvancement();
        
        // Only reward for advancements that have a display (shown in-game)
        // This filters out recipe unlocks and hidden advancements
        DisplayInfo displayInfo = holder.value().display().orElse(null);
        if (displayInfo == null) {
            return;
        }

        // Check if this advancement was already rewarded
        if (wasAlreadyRewarded(player, holder)) {
            return;
        }

        // Calculate reward tier (single calculation, used for both reward and display)
        AchievementRewardTier tier = calculateTier(holder, displayInfo);
        int reward = tier.getAverageReward();
        
        // Give the reward
        EconomyManager.getInstance().deposit(
            player.getUUID(),
            reward,
            TransactionType.ACHIEVEMENT,
            "Achievement: " + displayInfo.getTitle().getString()
        );

        // Mark as rewarded
        markAsRewarded(player, holder);

        // Notify player
        String tierName = tier.getDisplayName();
        player.sendSystemMessage(Component.literal(
            String.format("§a§l✓ Achievement Reward! §r§a+$%d §7(%s)", reward, tierName)
        ));

        ServerManagementMod.LOGGER.info(
            "Rewarded player {} with ${} for advancement: {}",
            player.getName().getString(),
            reward,
            holder.id()
        );
    }

    /**
     * Calculate the reward tier based on advancement properties
     */
    private static AchievementRewardTier calculateTier(AdvancementHolder holder, DisplayInfo displayInfo) {
        AchievementRewardTier tierFromFrame = AchievementRewardTier.fromFrameType(displayInfo.getType());
        int criteriaCount = holder.value().criteria().size();
        boolean hasParent = holder.value().parent().isPresent();
        AchievementRewardTier tierFromComplexity = AchievementRewardTier.fromComplexity(criteriaCount, hasParent);
        return tierFromComplexity.ordinal() > tierFromFrame.ordinal() 
            ? tierFromComplexity 
            : tierFromFrame;
    }

    /**
     * Check if player was already rewarded for this advancement
     */
    private static boolean wasAlreadyRewarded(ServerPlayer player, AdvancementHolder holder) {
        // Use the achievement tracker for reliable duplicate detection
        String achievementId = holder.id().toString();
        return EconomyManager.getInstance()
            .getAchievementTracker()
            .hasBeenRewarded(player.getUUID(), achievementId);
    }

    /**
     * Mark advancement as rewarded
     */
    private static void markAsRewarded(ServerPlayer player, AdvancementHolder holder) {
        String achievementId = holder.id().toString();
        EconomyManager.getInstance()
            .getAchievementTracker()
            .markAsRewarded(player.getUUID(), achievementId);
        
        // Save the tracker
        EconomyManager.getInstance().save();
    }
}
