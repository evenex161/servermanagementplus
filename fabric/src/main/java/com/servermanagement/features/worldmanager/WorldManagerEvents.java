package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
public class WorldManagerEvents {
    private static WorldManagerEvents instance;

    public static void register() {
        if (instance == null) {
            instance = new WorldManagerEvents();
            ServerManagementMod.LOGGER.debug("Registered WorldManager events");
        }
    }

    public void onServerTick(net.minecraft.server.MinecraftServer server) {
        WorldManager.getInstance().tick();
    }

    public void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        // Handle lobby spawn on join if configured
        var lobbySpawn = WorldManager.getInstance().getData().getLobbySpawn();
        if (lobbySpawn != null) {
            // Teleport to lobby spawn
        }
    }

    public void onPlayerUseBlock(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos) {
        // Block portal usage if portals are disabled
        var block = level.getBlockState(pos).getBlock();
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
        String dimensionId = player.level().dimension().identifier().toString();
        
        if (blockId.equals("minecraft:nether_portal")) {
            if (!WorldManager.getInstance().canUseNetherPortal(dimensionId)) {
                // Fabric: handled by return value from callback
            }
        } else if (blockId.equals("minecraft:end_portal")) {
            if (!WorldManager.getInstance().canUseEndPortal(dimensionId)) {
                // Fabric: handled by return value from callback
            }
        }
    }
}
