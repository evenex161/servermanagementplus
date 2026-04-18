package com.servermanagement.features.slimehead;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.Feature;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class SlimeHeadManager implements Feature {
    
    private static final String FEATURE_ID = "slimehead";
    private static final String SLIME_HEAD_TAG = "SlimeHead";
    
    // Slime head texture (base64 encoded)
    private static final String SLIME_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODk1YWVlYzZiODQyYWRhODY2OWY4NDZkNjViYzQ5NzYyNTk3ODI0YWI5NDRmMjJmNDViZjNiYmI5NDFhYmU2YyJ9fX0=";
    
    @Override
    public String getId() {
        return FEATURE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Slime Head";
    }
    
    @Override
    public String getDescription() {
        return "Decorative unbreakable slime heads";
    }
    
    @Override
    public String getDetailedDescription() {
        return "Gives players decorative slime heads that cannot be broken by non-operators. " +
               "Use /slimehead command to obtain heads. When placed, they are protected from destruction.";
    }
    
    @Override
    public void initialize(MinecraftServer server) {
        ServerManagementMod.LOGGER.debug("SlimeHead feature initialized");
    }
    
    @Override
    public void onEnable() {
        // Logged during feature initialization
    }
    
    @Override
    public void onDisable() {
        ServerManagementMod.LOGGER.debug("SlimeHead feature disabled");
    }
    
    /**
     * Creates a slime head item with custom texture
     */
    public static ItemStack createSlimeHead() {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        
        // Create game profile with slime texture
        GameProfile profile = new GameProfile(UUID.randomUUID(), "Slime");
        profile.getProperties().put("textures", new Property("textures", SLIME_TEXTURE));
        
        // Set profile component
        head.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        
        // Set custom name
        head.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§aSlime Head"));
        
        // Set custom data for slime head identification
        CompoundTag customTag = new CompoundTag();
        customTag.putBoolean(SLIME_HEAD_TAG, true);
        customTag.putBoolean("Unbreakable", true);
        head.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
        
        return head;
    }
    
    /**
     * Checks if an item is a slime head
     */
    public static boolean isSlimeHead(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.PLAYER_HEAD) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBoolean(SLIME_HEAD_TAG);
    }
    
    /**
     * Gives a slime head to a player
     */
    public static void giveSlimeHead(ServerPlayer player) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return;
        }
        
        ItemStack slimeHead = createSlimeHead();
        if (!player.getInventory().add(slimeHead)) {
            player.drop(slimeHead, false);
        }
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aYou received a Slime Head!"));
    }
    
    /**
     * Prevents slime heads from being broken when placed
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return;
        }
        
        // Check if the broken block is a player head (slime head)
        if (event.getState().getBlock() == net.minecraft.world.level.block.Blocks.PLAYER_HEAD ||
            event.getState().getBlock() == net.minecraft.world.level.block.Blocks.PLAYER_WALL_HEAD) {
            
            // Get the block entity
            var level = event.getLevel();
            var pos = event.getPos();
            var blockEntity = level.getBlockEntity(pos);
            
            if (blockEntity instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skullEntity) {
                // Check if it has slime head data
                var owner = skullEntity.getOwnerProfile();
                if (owner != null && owner.name().isPresent() && owner.name().get().equals("Slime")) {
                    // Check if player has permission to break
                    if (event.getPlayer() instanceof ServerPlayer player) {
                        if (!player.hasPermissions(2)) {
                            event.setCanceled(true);
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cSlime Heads cannot be broken!"));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Plays slime sound when noteblock below slime head is played
     */
    @SubscribeEvent
    public static void onNoteBlockPlay(NoteBlockEvent.Play event) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return;
        }
        
        var levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level)) {
            return;
        }
        
        BlockPos noteBlockPos = event.getPos();
        BlockPos abovePos = noteBlockPos.above();
        
        // Check if there's a player head above the noteblock
        var blockAbove = level.getBlockState(abovePos);
        if (blockAbove.getBlock() == net.minecraft.world.level.block.Blocks.PLAYER_HEAD ||
            blockAbove.getBlock() == net.minecraft.world.level.block.Blocks.PLAYER_WALL_HEAD) {
            
            var blockEntity = level.getBlockEntity(abovePos);
            if (blockEntity instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skullEntity) {
                var owner = skullEntity.getOwnerProfile();
                if (owner != null && owner.name().isPresent() && owner.name().get().equals("Slime")) {
                    // Cancel default noteblock sound
                    event.setCanceled(true);
                    
                    // Play slime sound instead
                    if (!level.isClientSide) {
                        level.playSound(null, noteBlockPos, SoundEvents.SLIME_SQUISH, 
                            SoundSource.RECORDS, 3.0F, 1.0F);
                    }
                }
            }
        }
    }
}
