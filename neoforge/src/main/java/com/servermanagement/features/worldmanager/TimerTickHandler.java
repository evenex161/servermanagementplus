package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.util.PerformanceMetrics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class TimerTickHandler {
    
    private static int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int SAVE_INTERVAL = 10; // Save every 10 seconds instead of every second
    private static int saveCounter = 0;
    private static boolean needsSave = false;
    
    // Track which warning times have been announced for each dimension
    private static final Set<String> announced60s = new HashSet<>();
    private static final Set<String> announced30s = new HashSet<>();
    private static final Set<String> announced10s = new HashSet<>();
    private static final Set<String> announced5s = new HashSet<>();
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        
        if (!ModConfig.WORLD_MANAGER_ENABLED.get() || !ModConfig.WORLD_TIMERS_ENABLED.get()) {
            return;
        }
        
        tickCounter++;
        if (tickCounter < TICKS_PER_SECOND) {
            return; // Only run once per second
        }
        
        tickCounter = 0;
        saveCounter++;
        PerformanceMetrics.getInstance().recordTimerTick();
        
        MinecraftServer server = event.getServer();
        WorldManagerData worldData = WorldManager.getInstance().getData();
        
        // Process all dimensions with active timers
        for (ServerLevel level : server.getAllLevels()) {
            String dimensionId = level.dimension().location().toString();
            
            if (!worldData.hasActiveTimer(dimensionId)) {
                continue;
            }
            
            long remainingTime = worldData.getRemainingTime(dimensionId);
            
            // When this is the final second, play the "1" beat AND complete the
            // timer in the same tick. hasActiveTimer requires value > 0, so if we
            // only decremented to 0 here the next tick would skip this dimension
            // and handleTimerComplete would never run -> portals would never flip.
            if (remainingTime <= 1) {
                sendTimerWarnings(server, dimensionId, worldData, remainingTime);
                handleTimerComplete(server, level, dimensionId, worldData);
                needsSave = true;
                continue;
            }
            
            // Decrement timer (in memory only)
            worldData.setTimerSeconds(dimensionId, (int) remainingTime - 1);
            needsSave = true;
            
            // Send warnings at specific intervals
            sendTimerWarnings(server, dimensionId, worldData, remainingTime);
        }
        
        // Batch save every SAVE_INTERVAL seconds instead of every second
        if (needsSave && saveCounter >= SAVE_INTERVAL) {
            WorldManager.getInstance().save();
            saveCounter = 0;
            needsSave = false;
        }
    }
    
    private static void handleTimerComplete(MinecraftServer server, ServerLevel level, 
                                           String dimensionId, WorldManagerData worldData) {
        ServerManagementMod.LOGGER.info("Timer completed for dimension: {}", dimensionId);
        
        // Read the target state and portal type from persistent data
        boolean enablePortals = worldData.getTimerEnablesPortal(dimensionId);
        String portalType = worldData.getTimerPortalType(dimensionId);
        
        // Apply the portal state change using the correct target state
        WorldManager.setPortalsByType(dimensionId, portalType, enablePortals);
        worldData.clearTimer(dimensionId);
        WorldManager.getInstance().save();
        
        // Clear announcement tracking
        clearAnnouncementTracking(dimensionId);
        
        // Build contextual messages
        String portalDesc = WorldManager.getPortalDescription(dimensionId, portalType);
        String dimName = WorldManager.getDimensionName(dimensionId);
        String actionWord = enablePortals ? "open" : "closed";
        String colorCode = enablePortals ? "§a" : "§c";
        
        // Always mention the dimension so players in other worlds aren't
        // misled into thinking their own portals just changed state.
        Component message = Component.literal(colorCode + "✔ " + portalDesc + " in " + dimName + " are now " + actionWord + "!");
        Component title = Component.literal(colorCode + portalDesc);
        Component subtitle = Component.literal((enablePortals ? "are now open in " : "are now closed in ") + dimName);
        
        // Notify ALL players on the server (not just the dimension)
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            // Distinct "timer finished" cue: pitch-shifted level-up (high for open,
            // low for close) layered with a decisive anvil land. Both are clearly
            // different from the per-second NOTE_BLOCK_PLING countdown ticks.
            float finalPitch = enablePortals ? 1.2f : 0.8f;
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, finalPitch);
            player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER, 0.6f, enablePortals ? 1.4f : 0.7f);
        }
        
        // Log for ops
        String dimensionName = WorldManager.getDimensionName(dimensionId);
        ServerManagementMod.LOGGER.info("Portal timer completed: {} in {} -> {}", portalDesc, dimensionName, enablePortals ? "enabled" : "disabled");
    }
    
    private static void sendTimerWarnings(MinecraftServer server, String dimensionId,
                                         WorldManagerData worldData, long remainingTime) {
        // Early exit: only process at specific countdown times to avoid per-second overhead
        if (remainingTime > 60 || (remainingTime > 5 && remainingTime != 60 && remainingTime != 30 && remainingTime != 10 && remainingTime != 5)) {
            return;
        }
        
        String portalType = worldData.getTimerPortalType(dimensionId);
        boolean enablePortals = worldData.getTimerEnablesPortal(dimensionId);
        String portalDesc = WorldManager.getPortalDescription(dimensionId, portalType);
        String dimName = WorldManager.getDimensionName(dimensionId);
        String actionWord = enablePortals ? "open" : "close";
        
        Component warning = null;
        Component titleText = null;
        Component subtitleText = null;
        boolean playSound = false;
        
        // Subtitle always mentions the dimension so players in other worlds
        // know exactly which portals are about to change state and don't
        // mistakenly think their own dimension's portals are being toggled.
        String subtitleBase = portalDesc + " in " + dimName + " " + actionWord;
        
        // 60 second warning — chat heads-up so the announcement reaches players
        // who don't have title text on screen.
        if (remainingTime == 60 && !announced60s.contains(dimensionId)) {
            warning = Component.literal("§e⚠ " + portalDesc + " in " + dimName + " " + actionWord + " in 1 minute");
            titleText = Component.literal("§e1 Minute");
            subtitleText = Component.literal(subtitleBase + " soon");
            playSound = true;
            announced60s.add(dimensionId);
        }
        // 30 second warning — title + sound only (no chat to reduce spam)
        else if (remainingTime == 30 && !announced30s.contains(dimensionId)) {
            titleText = Component.literal("§630 Seconds");
            subtitleText = Component.literal(subtitleBase + " soon");
            playSound = true;
            announced30s.add(dimensionId);
        }
        // 10 second warning — title + sound only
        else if (remainingTime == 10 && !announced10s.contains(dimensionId)) {
            titleText = Component.literal("§c10 Seconds");
            subtitleText = Component.literal(portalDesc + " in " + dimName);
            playSound = true;
            announced10s.add(dimensionId);
        }
        // 5 second countdown — title + sound only
        else if (remainingTime == 5 && !announced5s.contains(dimensionId)) {
            titleText = Component.literal("§45");
            subtitleText = Component.literal(subtitleBase + " soon!");
            playSound = true;
            announced5s.add(dimensionId);
        }
        // Final countdown (4, 3, 2, 1) — title + sound only
        else if (remainingTime <= 4 && remainingTime >= 1) {
            titleText = Component.literal("§4" + remainingTime);
            subtitleText = Component.literal(portalDesc + " in " + dimName);
            playSound = true;
        }
        
        // Send notifications to ALL players on the server
        if (warning != null || titleText != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (warning != null) {
                    player.sendSystemMessage(warning);
                }
                if (titleText != null) {
                    player.connection.send(new ClientboundSetTitleTextPacket(titleText));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
                }
                if (playSound) {
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), 
                                          SoundSource.MASTER, 1.0f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Clears announcement tracking for a dimension (called when timer is manually cancelled or changed)
     */
    public static void clearAnnouncementTracking(String dimensionId) {
        announced60s.remove(dimensionId);
        announced30s.remove(dimensionId);
        announced10s.remove(dimensionId);
        announced5s.remove(dimensionId);
    }
}
