package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class PortalEventHandler {
    
    /**
     * Prevents portal travel when disabled
     */
    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        // Check if WorldManager feature is enabled
        if (!ModConfig.WORLD_MANAGER_ENABLED.get()) {
            return; // Feature disabled, allow all portal travel
        }
        
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return; // Only check players
        }
        
        // Check permissions - ops can always travel
        if (player.hasPermissions(2)) {
            return; // OPs bypass portal restrictions
        }
        
        ResourceKey<Level> from = player.level().dimension();
        ResourceKey<Level> to = event.getDimension();
        
        // Check if this is a Nether portal
        boolean isNetherTravel = (from == Level.OVERWORLD && to == Level.NETHER) || 
                                 (from == Level.NETHER && to == Level.OVERWORLD);
        
        // Check if this is an End portal
        boolean isEndTravel = (from == Level.OVERWORLD && to == Level.END) ||
                             (from == Level.END && to == Level.OVERWORLD);
        
        // Only check portal types that are configured to be controlled
        if (!isNetherTravel && !isEndTravel) {
            return; // Not a controlled portal type
        }
        
        // Get world-specific portal settings
        String dimensionId = from.location().toString();
        WorldManager worldManager = WorldManager.getInstance();
        
        if (worldManager == null) {
            ServerManagementMod.LOGGER.error("WorldManager instance is null!");
            return;
        }
        
        WorldManagerData worldData = worldManager.getData();
        if (worldData == null) {
            ServerManagementMod.LOGGER.error("WorldManagerData is null!");
            return;
        }
        
        // Check the appropriate portal type
        boolean portalAllowed;
        String portalTypeName;
        if (isNetherTravel) {
            portalAllowed = worldData.areNetherPortalsEnabled(dimensionId);
            portalTypeName = "Nether";
        } else {
            portalAllowed = worldData.areEndPortalsEnabled(dimensionId);
            portalTypeName = "End";
        }
        
        // If portals are disabled for this type in this dimension, block travel
        if (!portalAllowed) {
            event.setCanceled(true);
            String dimName = WorldManager.getDimensionName(dimensionId);
            player.sendSystemMessage(Component.literal("§c" + portalTypeName + " portals are disabled in " + dimName + "!"));
            return;
        }
        
        // NOTE: An active portal timer no longer cancels travel. The portal
        // state itself only flips at timer completion, so during the countdown
        // the portal is in its pre-flip state and travel should respect that:
        //   - enabled → disabled timer: portal is still enabled, so allowing
        //     travel gives players the announced window to escape (the entire
        //     point of the heads-up). Previously this was blocked, which made
        //     the warning system pointless.
        //   - disabled → enabled timer: portal is still disabled, so the
        //     `!portalAllowed` check above already cancels travel with a
        //     clear, dimension-aware message. No additional block needed.
    }
    
    /**
     * Prevents portal frame creation when portals are disabled
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get()) {
            return;
        }
        
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Check if player is using flint and steel or fire charge
        var item = event.getItemStack().getItem();
        boolean isIgniter = item == net.minecraft.world.item.Items.FLINT_AND_STEEL ||
                           item == net.minecraft.world.item.Items.FIRE_CHARGE;
        
        if (!isIgniter) {
            return;
        }
        
        // Check permissions
        if (player.hasPermissions(2)) {
            return;
        }
        
        BlockPos clickedPos = event.getPos();
        var level = event.getLevel();
        var clickedBlock = level.getBlockState(clickedPos).getBlock();
        
        // Check if clicking obsidian (potential nether portal frame)
        if (clickedBlock == net.minecraft.world.level.block.Blocks.OBSIDIAN) {
            String dimensionId = player.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areNetherPortalsEnabled(dimensionId)) {
                event.setUseBlock(Event.Result.DENY);
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cNether portals are disabled in this dimension!"));
            }
        }
        
        // Check if clicking End Portal Frame with Eye of Ender
        if (item == net.minecraft.world.item.Items.ENDER_EYE && 
            clickedBlock == net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME) {
            
            String dimensionId = player.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areEndPortalsEnabled(dimensionId)) {
                event.setUseBlock(Event.Result.DENY);
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cEnd portals are disabled in this dimension!"));
            }
        }
    }
    
    /**
     * Prevents portal blocks from being placed when disabled
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get()) {
            return;
        }
        
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Check permissions
        if (player.hasPermissions(2)) {
            return;
        }
        
        var placedBlock = event.getPlacedBlock().getBlock();
        
        // Prevent direct Nether portal block placement
        if (placedBlock == net.minecraft.world.level.block.Blocks.NETHER_PORTAL) {
            String dimensionId = player.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areNetherPortalsEnabled(dimensionId)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cNether portals are disabled in this dimension!"));
            }
        }
        
        // Prevent direct End portal block placement
        if (placedBlock == net.minecraft.world.level.block.Blocks.END_PORTAL) {
            String dimensionId = player.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areEndPortalsEnabled(dimensionId)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cEnd portals are disabled in this dimension!"));
            }
        }
    }
}
