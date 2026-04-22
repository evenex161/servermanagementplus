package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class PortalEventHandler {
    
    /**
     * Prevents portal travel when disabled.
     * @return true to allow travel, false to block
     */
    public static boolean onEntityTravelToDimension(Entity entity, ResourceKey<Level> dimension) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get()) {
            return true;
        }
        
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        
        if (player.hasPermissions(2)) {
            return true;
        }
        
        ResourceKey<Level> from = player.level().dimension();
        ResourceKey<Level> to = dimension;
        
        boolean isNetherTravel = (from == Level.OVERWORLD && to == Level.NETHER) || 
                                 (from == Level.NETHER && to == Level.OVERWORLD);
        
        boolean isEndTravel = (from == Level.OVERWORLD && to == Level.END) ||
                             (from == Level.END && to == Level.OVERWORLD);
        
        if (!isNetherTravel && !isEndTravel) {
            return true;
        }
        
        String dimensionId = from.location().toString();
        WorldManager worldManager = WorldManager.getInstance();
        
        if (worldManager == null) {
            ServerManagementMod.LOGGER.error("WorldManager instance is null!");
            return true;
        }
        
        WorldManagerData worldData = worldManager.getData();
        if (worldData == null) {
            ServerManagementMod.LOGGER.error("WorldManagerData is null!");
            return true;
        }
        
        boolean portalAllowed;
        String portalTypeName;
        if (isNetherTravel) {
            portalAllowed = worldData.areNetherPortalsEnabled(dimensionId);
            portalTypeName = "Nether";
        } else {
            portalAllowed = worldData.areEndPortalsEnabled(dimensionId);
            portalTypeName = "End";
        }
        
        if (!portalAllowed) {
            String dimName = WorldManager.getDimensionName(dimensionId);
            player.sendSystemMessage(Component.literal("§c" + portalTypeName + " portals are disabled in " + dimName + "!"));
            return false;
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
        return true;
    }
    
    /**
     * Prevents portal frame creation when portals are disabled.
     * @return InteractionResult.PASS to allow, InteractionResult.FAIL to block
     */
    public static InteractionResult onRightClickBlock(net.minecraft.world.entity.player.Player player, Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get()) {
            return InteractionResult.PASS;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        var item = player.getItemInHand(hand).getItem();
        boolean isIgniter = item == net.minecraft.world.item.Items.FLINT_AND_STEEL ||
                           item == net.minecraft.world.item.Items.FIRE_CHARGE;
        
        if (!isIgniter) {
            return InteractionResult.PASS;
        }
        
        if (serverPlayer.hasPermissions(2)) {
            return InteractionResult.PASS;
        }
        
        BlockPos clickedPos = hitResult.getBlockPos();
        var clickedBlock = level.getBlockState(clickedPos).getBlock();
        
        if (clickedBlock == net.minecraft.world.level.block.Blocks.OBSIDIAN) {
            String dimensionId = serverPlayer.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areNetherPortalsEnabled(dimensionId)) {
                serverPlayer.sendSystemMessage(Component.literal("§cNether portals are disabled in this dimension!"));
                return InteractionResult.FAIL;
            }
        }
        
        if (item == net.minecraft.world.item.Items.ENDER_EYE && 
            clickedBlock == net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME) {
            
            String dimensionId = serverPlayer.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areEndPortalsEnabled(dimensionId)) {
                serverPlayer.sendSystemMessage(Component.literal("§cEnd portals are disabled in this dimension!"));
                return InteractionResult.FAIL;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Prevents portal blocks from being placed when disabled.
     * @return true to allow, false to block
     */
    public static boolean onBlockPlace(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, Entity entity) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get()) {
            return true;
        }
        
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        
        if (player.hasPermissions(2)) {
            return true;
        }
        
        var placedBlock = state.getBlock();
        
        if (placedBlock == net.minecraft.world.level.block.Blocks.NETHER_PORTAL) {
            String dimensionId = player.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areNetherPortalsEnabled(dimensionId)) {
                player.sendSystemMessage(Component.literal("§cNether portals are disabled in this dimension!"));
                return false;
            }
        }
        
        if (placedBlock == net.minecraft.world.level.block.Blocks.END_PORTAL) {
            String dimensionId = player.level().dimension().location().toString();
            WorldManagerData worldData = WorldManager.getInstance().getData();
            
            if (!worldData.areEndPortalsEnabled(dimensionId)) {
                player.sendSystemMessage(Component.literal("§cEnd portals are disabled in this dimension!"));
                return false;
            }
        }
        
        return true;
    }
}
