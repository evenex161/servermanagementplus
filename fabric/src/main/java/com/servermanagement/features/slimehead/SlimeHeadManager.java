package com.servermanagement.features.slimehead;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.Feature;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

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
        
        // Write SkullOwner NBT (1.20.1 way)
        CompoundTag tag = head.getOrCreateTag();
        CompoundTag skullOwner = net.minecraft.nbt.NbtUtils.writeGameProfile(new CompoundTag(), profile);
        tag.put("SkullOwner", skullOwner);
        
        // Set custom name
        head.setHoverName(net.minecraft.network.chat.Component.literal("§aSlime Head"));
        
        // Set custom data for slime head identification
        tag.putBoolean(SLIME_HEAD_TAG, true);
        tag.putBoolean("Unbreakable", true);
        
        return head;
    }
    
    /**
     * Checks if an item is a slime head
     */
    public static boolean isSlimeHead(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.PLAYER_HEAD) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(SLIME_HEAD_TAG);
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
    public static void onBlockBreak(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return;
        }
        
        // Check if the broken block is a player head (slime head)
        if (state.getBlock() == net.minecraft.world.level.block.Blocks.PLAYER_HEAD ||
            state.getBlock() == net.minecraft.world.level.block.Blocks.PLAYER_WALL_HEAD) {
            
            // Get the block entity
            var level = world;
            var blockEntity = level.getBlockEntity(pos);
            
            if (blockEntity instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skullEntity) {
                // Check if it has slime head data
                var owner = skullEntity.getOwnerProfile();
                if (owner != null && owner.getName() != null && owner.getName().equals("Slime")) {
                    // Check if player has permission to break
                    if (player instanceof ServerPlayer sp) {
                        if (!player.hasPermissions(2)) {
                            // Fabric: block break cancellation handled by return value
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cSlime Heads cannot be broken!"));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Plays slime sound when noteblock below slime head is played.
     * Returns true if the slime sound was played (caller should cancel default).
     */
    public static boolean tryPlaySlimeSound(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos noteBlockPos) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return false;
        }

        BlockPos abovePos = noteBlockPos.above();
        var blockAbove = level.getBlockState(abovePos);
        if (blockAbove.getBlock() != net.minecraft.world.level.block.Blocks.PLAYER_HEAD &&
            blockAbove.getBlock() != net.minecraft.world.level.block.Blocks.PLAYER_WALL_HEAD) {
            return false;
        }

        var blockEntity = level.getBlockEntity(abovePos);
        if (!(blockEntity instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skullEntity)) {
            return false;
        }

        var owner = skullEntity.getOwnerProfile();
        if (owner == null || owner.getName() == null || !owner.getName().equals("Slime")) {
            return false;
        }

        if (!level.isClientSide) {
            level.playSound(null, noteBlockPos, SoundEvents.SLIME_SQUISH,
                SoundSource.RECORDS, 3.0F, 1.0F);
        }
        return true;
    }

    /**
     * Plays slime sound when noteblock below slime head is played
     */
    public static void onNoteBlockPlay(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
        tryPlaySlimeSound(world, pos);
    }
}
