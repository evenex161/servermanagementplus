package com.servermanagement.features.worldmanager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldManager {
    private static WorldManager instance;
    private WorldManagerData data;
    private MinecraftServer server;
    private PortalTimerManager timerManager;
    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    
    // Spam protection for broadcast notifications
    private static long lastBroadcastTime = 0;
    private static final long BROADCAST_COOLDOWN_MS = 3000;

    private WorldManager() {}

    public static WorldManager getInstance() {
        if (instance == null) {
            instance = new WorldManager();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        this.data = WorldManagerData.load(server);
        this.timerManager = new PortalTimerManager(server);
    }

    public void tick() {
        if (timerManager != null) {
            timerManager.tick();
        }
    }

    public WorldManagerData getData() {
        return data;
    }

    public void save() {
        if (data != null && server != null) {
            data.save(server);
            com.servermanagement.ServerManagementMod.LOGGER.debug("WorldManager data saved");
        } else {
            com.servermanagement.ServerManagementMod.LOGGER.warn("WorldManager save called but data or server is null!");
        }
    }

    public static List<String> getAllDimensions(MinecraftServer server) {
        List<String> dimensions = new ArrayList<>();
        if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                dimensions.add(level.dimension().location().toString());
            }
        }
        return dimensions;
    }

    public boolean canUseNetherPortal(String dimensionId) {
        return data.areNetherPortalsEnabled(dimensionId);
    }

    public boolean canUseEndPortal(String dimensionId) {
        return data.areEndPortalsEnabled(dimensionId);
    }

    public void toggleNetherPortals(String dimensionId, boolean enabled) {
        data.setNetherPortalsEnabled(dimensionId, enabled);
        save();
    }

    public void toggleEndPortals(String dimensionId, boolean enabled) {
        data.setEndPortalsEnabled(dimensionId, enabled);
        save();
    }

    public static void setNetherPortalsEnabled(String dimensionId, boolean enabled) {
        com.servermanagement.ServerManagementMod.LOGGER.info("setNetherPortalsEnabled: {} = {}", dimensionId, enabled);
        getInstance().data.setNetherPortalsEnabled(dimensionId, enabled);
        getInstance().save();
    }

    public static void setEndPortalsEnabled(String dimensionId, boolean enabled) {
        com.servermanagement.ServerManagementMod.LOGGER.info("setEndPortalsEnabled: {} = {}", dimensionId, enabled);
        getInstance().data.setEndPortalsEnabled(dimensionId, enabled);
        getInstance().save();
    }

    /**
     * Sets portal state for the given portal type ("nether", "end", or "both").
     */
    public static void setPortalsByType(String dimensionId, String portalType, boolean enabled) {
        if ("nether".equals(portalType) || "both".equals(portalType)) {
            getInstance().data.setNetherPortalsEnabled(dimensionId, enabled);
        }
        if ("end".equals(portalType) || "both".equals(portalType)) {
            getInstance().data.setEndPortalsEnabled(dimensionId, enabled);
        }
        getInstance().save();
    }

    /**
     * Gets a human-readable description of the portal type for a given dimension.
     */
    public static String getPortalDescription(String dimensionId, String portalType) {
        if ("minecraft:overworld".equals(dimensionId)) {
            switch (portalType) {
                case "nether": return "Nether portals";
                case "end": return "End portals";
                case "both": return "Nether and End portals";
                default: return "Portals";
            }
        } else if ("minecraft:the_nether".equals(dimensionId)) {
            return "Portals to the Overworld";
        } else if ("minecraft:the_end".equals(dimensionId)) {
            return "Portals to the Overworld";
        }
        // Modded dimensions
        switch (portalType) {
            case "nether": return "Nether portals";
            case "end": return "End portals";
            case "both": return "All portals";
            default: return "Portals";
        }
    }

    /**
     * Broadcasts a portal state change to all players with spam protection.
     */
    public static void broadcastPortalChange(MinecraftServer server, String dimensionId, String portalType, boolean enabled) {
        long now = System.currentTimeMillis();
        if (now - lastBroadcastTime < BROADCAST_COOLDOWN_MS) {
            return;
        }
        lastBroadcastTime = now;

        String dimName = getDimensionName(dimensionId);
        String portalDesc = getPortalDescription(dimensionId, portalType);
        String action = enabled ? "§aenabled" : "§cdisabled";

        Component message = Component.literal("§6[Server] §e" + portalDesc + " in " + dimName + " have been " + action + "§e!");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    public static void setChatIsolationEnabled(boolean enabled) {
        getInstance().data.setChatIsolationEnabled(enabled);
        getInstance().save();
    }
    
    public static void setTabIsolationEnabled(boolean enabled) {
        getInstance().data.setTabIsolationEnabled(enabled);
        getInstance().save();
    }

    public static void setLobbySpawn(BlockPos pos, String dimensionId) {
        getInstance().data.setLobbySpawn(new WorldManagerData.LobbySpawn(
            pos.getX(), pos.getY(), pos.getZ(), dimensionId, 0, 0
        ));
        getInstance().save();
    }

    public static void teleportToDimension(ServerPlayer player, String dimensionId) {
        // Check teleport cooldown
        WorldManager wm = getInstance();
        int cooldownSeconds = com.servermanagement.config.ModConfig.TELEPORT_COOLDOWN.get();
        if (cooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            Long lastTeleport = wm.teleportCooldowns.get(player.getUUID());
            if (lastTeleport != null) {
                long remaining = (lastTeleport + (cooldownSeconds * 1000L)) - now;
                if (remaining > 0) {
                    int secondsLeft = (int) Math.ceil(remaining / 1000.0);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cTeleport on cooldown! Wait " + secondsLeft + " second" + (secondsLeft != 1 ? "s" : "") + "."
                    ));
                    return;
                }
            }
            wm.teleportCooldowns.put(player.getUUID(), now);
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ResourceLocation dimLoc = ResourceLocation.tryParse(dimensionId);
        if (dimLoc == null) return;
        
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
        ServerLevel targetLevel = server.getLevel(dimKey);

        if (targetLevel != null) {
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            player.teleportTo(targetLevel, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
        }
    }

    public MinecraftServer getServer() {
        return server;
    }

    public PortalTimerManager getTimerManager() {
        return timerManager;
    }

    public List<com.servermanagement.network.packet.SyncWorldListPacket.WorldInfo> buildWorldListForClient(MinecraftServer server) {
        List<com.servermanagement.network.packet.SyncWorldListPacket.WorldInfo> worlds = new ArrayList<>();
        
        for (ServerLevel level : server.getAllLevels()) {
            String dimId = level.dimension().location().toString();
            String name = getDimensionName(dimId);
            boolean netherEnabled = data.areNetherPortalsEnabled(dimId);
            boolean endEnabled = data.areEndPortalsEnabled(dimId);
            int playerCount = level.players().size();
            
            worlds.add(new com.servermanagement.network.packet.SyncWorldListPacket.WorldInfo(
                dimId, name, netherEnabled, endEnabled, playerCount
            ));
        }
        
        return worlds;
    }

    public static String getDimensionName(String dimensionId) {
        // Convert dimension ID to friendly name
        if (dimensionId.equals("minecraft:overworld")) return "Overworld";
        if (dimensionId.equals("minecraft:the_nether")) return "The Nether";
        if (dimensionId.equals("minecraft:the_end")) return "The End";
        
        // Extract name from resource location
        String[] parts = dimensionId.split(":");
        if (parts.length == 2) {
            String name = parts[1].replace("_", " ");
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return dimensionId;
    }
}
