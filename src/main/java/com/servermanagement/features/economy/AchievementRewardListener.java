package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;

/**
 * Listens for achievement/advancement events and rewards players with money.
 * Works with all mods - any advancement from any source.
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class AchievementRewardListener {

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        // Check if Economy feature is enabled
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        Advancement advancement = event.getAdvancement();
        
        // Only reward for advancements that have a display (shown in-game)
        // This filters out recipe unlocks and hidden advancements
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) {
            return;
        }

        // Check if this advancement was already rewarded
        if (wasAlreadyRewarded(player, advancement)) {
            return;
        }

        // Calculate reward amount
        int reward = calculateReward(advancement, displayInfo);
        
        // Give the reward
        EconomyManager.getInstance().deposit(
            player.getUUID(),
            reward,
            TransactionType.ACHIEVEMENT,
            "Achievement: " + displayInfo.getTitle().getString()
        );

        // Mark as rewarded
        markAsRewarded(player, advancement);

        // Notify player
        String tierName = getTierFromAdvancement(advancement, displayInfo).getDisplayName();
        player.sendSystemMessage(Component.literal(
            "§a§l✓ Achievement Reward! §r§a+" +
            "$" + reward + 
            " §7(" + tierName + ")"
        ));

        ServerManagementMod.LOGGER.info(
            "Rewarded player {} with ${} for advancement: {}",
            player.getName().getString(),
            reward,
            advancement.getId()
        );
    }

    /**
     * Calculate reward amount based on advancement properties
     */
    private static int calculateReward(Advancement advancement, DisplayInfo displayInfo) {
        // Determine tier based on frame type (TASK, GOAL, CHALLENGE)
        AchievementRewardTier tierFromFrame = AchievementRewardTier.fromFrameType(displayInfo.getFrame());
        
        // Also consider complexity (number of criteria)
        int criteriaCount = advancement.getCriteria().size();
        boolean hasParent = advancement.getParent() != null;
        AchievementRewardTier tierFromComplexity = AchievementRewardTier.fromComplexity(criteriaCount, hasParent);
        
        // Use the higher tier of the two
        AchievementRewardTier finalTier = tierFromComplexity.ordinal() > tierFromFrame.ordinal() 
            ? tierFromComplexity 
            : tierFromFrame;
        
        // Return average reward for the tier
        return finalTier.getAverageReward();
    }

    /**
     * Get the tier for display purposes
     */
    private static AchievementRewardTier getTierFromAdvancement(Advancement advancement, DisplayInfo displayInfo) {
        AchievementRewardTier tierFromFrame = AchievementRewardTier.fromFrameType(displayInfo.getFrame());
        int criteriaCount = advancement.getCriteria().size();
        boolean hasParent = advancement.getParent() != null;
        AchievementRewardTier tierFromComplexity = AchievementRewardTier.fromComplexity(criteriaCount, hasParent);
        
        return tierFromComplexity.ordinal() > tierFromFrame.ordinal() 
            ? tierFromComplexity 
            : tierFromFrame;
    }

    /**
     * Check if player was already rewarded for this advancement
     */
    private static boolean wasAlreadyRewarded(ServerPlayer player, Advancement advancement) {
        // Use the achievement tracker for reliable duplicate detection
        String achievementId = advancement.getId().toString();
        return EconomyManager.getInstance()
            .getAchievementTracker()
            .hasBeenRewarded(player.getUUID(), achievementId);
    }

    /**
     * Mark advancement as rewarded
     */
    private static void markAsRewarded(ServerPlayer player, Advancement advancement) {
        String achievementId = advancement.getId().toString();
        EconomyManager.getInstance()
            .getAchievementTracker()
            .markAsRewarded(player.getUUID(), achievementId);
        
        // Save the tracker
        EconomyManager.getInstance().save();
    }
}
