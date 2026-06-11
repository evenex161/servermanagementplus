package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class TabListIsolationHandler {
    
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20; // Update every second
    
    // Heartbeat: every 100 ticks (5 seconds), force a full resync to catch drift
    private static int heartbeatCounter = 0;
    private static final int HEARTBEAT_INTERVAL = 100;
    
    // Track which players each player currently sees in their tab list
    private static final Map<UUID, Set<UUID>> previousVisiblePlayers = new HashMap<>();
    
    // Track whether isolation was active last cycle (for restore on disable)
    private static boolean wasIsolationActive = false;
    
    // Reuse collections to avoid per-tick allocation
    private static final Map<String, List<ServerPlayer>> dimensionPlayers = new HashMap<>();
    
    // Pending dimension change: use tick counter instead of server.execute()
    // because server.execute() can run in the same tick (during waitUntilNextTick),
    // which is too early — the client hasn't processed vanilla's dimension-change
    // packets yet, causing our PlayerInfoUpdate to be silently dropped.
    private static boolean pendingDimensionUpdate = false;
    private static int dimensionChangeCountdown = 0;
    private static final int DIMENSION_CHANGE_DELAY = 3; // Wait 3 full ticks
    
    private static boolean isIsolationActive() {
        return ModConfig.WORLD_MANAGER_ENABLED.get() 
            && ModConfig.TAB_ISOLATION_ENABLED.get()
            && WorldManager.getInstance().getData().isTabIsolationEnabled();
    }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        
        MinecraftServer server = event.getServer();
        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        boolean isolationActive = isIsolationActive();
        
        // Handle pending dimension change with multi-tick delay
        if (pendingDimensionUpdate && isolationActive) {
            dimensionChangeCountdown--;
            if (dimensionChangeCountdown <= 0) {
                pendingDimensionUpdate = false;
                applyIsolation(server, allPlayers);
                // Reset tick counter so the regular 20-tick check doesn't fire too soon
                tickCounter = 0;
            }
            // Don't run regular cycle while settling
            return;
        }
        
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        // If isolation was just disabled, restore full tab list for everyone
        if (wasIsolationActive && !isolationActive) {
            restoreFullTabList(server, allPlayers);
            previousVisiblePlayers.clear();
            wasIsolationActive = false;
            return;
        }
        
        wasIsolationActive = isolationActive;
        
        if (!isolationActive) {
            return;
        }
        
        // Heartbeat: periodically clear cached state to force full resync,
        // catching any drift from missed packets or edge cases
        heartbeatCounter++;
        if (heartbeatCounter >= HEARTBEAT_INTERVAL) {
            heartbeatCounter = 0;
            previousVisiblePlayers.clear();
            ServerManagementMod.LOGGER.debug("Tab isolation heartbeat: forcing full resync");
        }
        
        applyIsolation(server, allPlayers);
    }
    
    /**
     * When a player joins, vanilla broadcasts their info to ALL players.
     * We need to immediately remove them from tab lists of players in other dimensions,
     * and remove other-dimension players from the joining player's tab list.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joiningPlayer)) return;
        if (!isIsolationActive()) return;
        
        MinecraftServer server = joiningPlayer.level().getServer();
        if (server == null) return;
        
        // Delay execution by 1 tick so vanilla finishes sending the initial player info
        server.execute(() -> {
            if (!isIsolationActive()) return;
            
            List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
            applyIsolation(server, allPlayers);
            
            ServerManagementMod.LOGGER.debug("Tab isolation applied after player {} joined", 
                joiningPlayer.getName().getString());
        });
    }
    
    /**
     * When a player changes dimension, schedule a tab list update after multiple ticks.
     * We use a tick-counter delay instead of server.execute() because server.execute()
     * can run in the same tick (during waitUntilNextTick), before the network flush.
     * A 3-tick delay ensures the client has fully processed vanilla's respawn/teleport
     * packets and entity tracking updates before we modify the tab list.
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (!isIsolationActive()) return;
        
        pendingDimensionUpdate = true;
        dimensionChangeCountdown = DIMENSION_CHANGE_DELAY;
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        previousVisiblePlayers.remove(event.getEntity().getUUID());
    }
    
    /**
     * Core isolation logic: compute who each player should see, then send
     * differential add/remove packets.
     */
    private static void applyIsolation(MinecraftServer server, List<ServerPlayer> allPlayers) {
        // Group players by dimension (reuse static map)
        dimensionPlayers.values().forEach(List::clear);
        for (ServerPlayer player : allPlayers) {
            String dimension = player.level().dimension().identifier().toString();
            dimensionPlayers.computeIfAbsent(dimension, k -> new ArrayList<>()).add(player);
        }
        
        // Build a set of all online UUIDs
        Set<UUID> onlineUUIDs = new HashSet<>();
        for (ServerPlayer player : allPlayers) {
            onlineUUIDs.add(player.getUUID());
        }
        
        // Update tab list for each player
        for (ServerPlayer player : allPlayers) {
            String playerDimension = player.level().dimension().identifier().toString();
            Set<UUID> shouldSee = computeVisibleUUIDs(playerDimension, dimensionPlayers);
            
            // KEY FIX: If no previous state exists (first evaluation for this player),
            // assume they can see ALL online players (vanilla default), so the diff
            // correctly computes which players to REMOVE.
            Set<UUID> previouslySaw = previousVisiblePlayers.get(player.getUUID());
            boolean isFirstEvaluation = (previouslySaw == null);
            if (isFirstEvaluation) {
                previouslySaw = new HashSet<>(onlineUUIDs);
            }
            
            // Players to UNLIST from tab (were listed, now shouldn't be)
            List<UUID> toUnlist = new ArrayList<>();
            for (UUID id : previouslySaw) {
                if (!shouldSee.contains(id) && onlineUUIDs.contains(id)) {
                    toUnlist.add(id);
                }
            }
            
            // Players to re-LIST in tab (weren't listed, now should be)
            List<UUID> toList = new ArrayList<>();
            for (UUID id : shouldSee) {
                if (!previouslySaw.contains(id)) {
                    toList.add(id);
                }
            }
            
            // Send UNLIST packet — hides from tab overlay WITHOUT removing PlayerInfo.
            // Uses UPDATE_LISTED(false) instead of ClientboundPlayerInfoRemovePacket
            // because REMOVE deletes from playerInfoMap, which breaks entity tracking
            // (handleAddEntity rejects players not in playerInfoMap, making models invisible).
            if (!toUnlist.isEmpty()) {
                player.connection.send(createUpdateListedPacket(server, toUnlist, false));
                if (isFirstEvaluation) {
                    ServerManagementMod.LOGGER.debug("Initial tab isolation for {}: unlisted {} players", 
                        player.getName().getString(), toUnlist.size());
                }
            }
            
            // Send LIST packet — shows in tab overlay
            if (!toList.isEmpty()) {
                player.connection.send(createUpdateListedPacket(server, toList, true));
            }
            
            // Store current state
            previousVisiblePlayers.put(player.getUUID(), shouldSee);
        }
        
        // Cleanup entries for disconnected players
        previousVisiblePlayers.keySet().retainAll(onlineUUIDs);
    }
    
    private static Set<UUID> computeVisibleUUIDs(
        String dimension,
        Map<String, List<ServerPlayer>> dimensionPlayers
    ) {
        Set<UUID> visible = new HashSet<>();
        
        // Only show players from the same dimension — pure dimension-based isolation
        for (ServerPlayer player : dimensionPlayers.getOrDefault(dimension, Collections.emptyList())) {
            visible.add(player.getUUID());
        }
        
        return visible;
    }
    
    /**
     * Creates a ClientboundPlayerInfoUpdatePacket with UPDATE_LISTED action.
     * The standard Entry(ServerPlayer) constructor hardcodes listed=true, so we
     * manually construct the packet via buffer to control the listed flag.
     * This preserves playerInfoMap entries (unlike REMOVE packets) so entity
     * tracking continues to work — players remain visible as 3D models.
     */
    private static ClientboundPlayerInfoUpdatePacket createUpdateListedPacket(
            MinecraftServer server, List<UUID> playerUUIDs, boolean listed) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
            Unpooled.buffer(), server.registryAccess());
        try {
            buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                ClientboundPlayerInfoUpdatePacket.Action.class);
            buf.writeVarInt(playerUUIDs.size());
            for (UUID uuid : playerUUIDs) {
                buf.writeUUID(uuid);
                buf.writeBoolean(listed);
            }
            return ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(buf);
        } finally {
            buf.release();
        }
    }
    
    private static void restoreFullTabList(MinecraftServer server, List<ServerPlayer> allPlayers) {
        if (allPlayers.isEmpty()) return;
        
        List<UUID> allUUIDs = new ArrayList<>();
        for (ServerPlayer player : allPlayers) {
            allUUIDs.add(player.getUUID());
        }
        
        ClientboundPlayerInfoUpdatePacket fullPacket = createUpdateListedPacket(server, allUUIDs, true);
        
        for (ServerPlayer player : allPlayers) {
            player.connection.send(fullPacket);
        }
        
        ServerManagementMod.LOGGER.debug("Tab isolation disabled — restored full tab list for {} players", allPlayers.size());
    }
    
    /**
     * Called externally (e.g. from toggle packet handler) to immediately restore
     * the full tab list when isolation is disabled, instead of waiting for the next tick cycle.
     */
    public static void onIsolationToggled(MinecraftServer server) {
        boolean active = isIsolationActive();
        if (!active && wasIsolationActive) {
            restoreFullTabList(server, server.getPlayerList().getPlayers());
            previousVisiblePlayers.clear();
            wasIsolationActive = false;
        } else if (active && !wasIsolationActive) {
            wasIsolationActive = true;
            applyIsolation(server, server.getPlayerList().getPlayers());
        }
    }
}
