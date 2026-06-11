package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class WorldManagerEvents {
    private static WorldManagerEvents instance;

    public static void register() {
        if (instance == null) {
            instance = new WorldManagerEvents();
            NeoForge.EVENT_BUS.register(instance);
            ServerManagementMod.LOGGER.debug("Registered WorldManager events");
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {{
            WorldManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Handle lobby spawn on join if configured
        var lobbySpawn = WorldManager.getInstance().getData().getLobbySpawn();
        if (lobbySpawn != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            // Teleport to lobby spawn
        }
    }

    @SubscribeEvent
    public void onPlayerUseBlock(PlayerInteractEvent.RightClickBlock event) {
        // Block portal usage if portals are disabled
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            var block = event.getLevel().getBlockState(event.getPos()).getBlock();
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
            String dimensionId = player.level().dimension().identifier().toString();
            
            if (blockId.equals("minecraft:nether_portal")) {
                if (!WorldManager.getInstance().canUseNetherPortal(dimensionId)) {
                    event.setCanceled(true);
                }
            } else if (blockId.equals("minecraft:end_portal")) {
                if (!WorldManager.getInstance().canUseEndPortal(dimensionId)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
