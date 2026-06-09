package com.servermanagement.features.playermanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.PMSyncPlayerListsPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.level.GameType;

import java.util.*;

public class PlayerManagerSingleton {
    private static PlayerManagerSingleton instance;
    private MinecraftServer server;
    private final Map<UUID, SpectateData> spectating = new HashMap<>();

    private PlayerManagerSingleton() {}

    public static synchronized PlayerManagerSingleton getInstance() {
        if (instance == null) {
            instance = new PlayerManagerSingleton();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public static void spectatePlayer(ServerPlayer spectator, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            spectator.sendSystemMessage(Component.literal("§cError: Server not initialized"));
            return;
        }
        
        ServerPlayer target = instance.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            spectator.sendSystemMessage(Component.literal(
                String.format("§cPlayer not found: %s", targetName)));
            return;
        }

        if (spectator.getUUID().equals(target.getUUID())) {
            spectator.sendSystemMessage(Component.literal("§cYou cannot spectate yourself!"));
            return;
        }

        boolean crossDimension = !spectator.level().dimension().equals(target.level().dimension());
        
        // Stealth mode: same-dimension, body stays at original position
        // Non-stealth: cross-dimension, body teleports to target
        boolean stealth = !crossDimension;
        
        // Save spectator's original state
        SpectateData data = new SpectateData(
            spectator.getX(), spectator.getY(), spectator.getZ(),
            spectator.level().dimension().location().toString(),
            spectator.gameMode.getGameModeForPlayer(),
            target.getUUID(),
            stealth
        );
        instance.spectating.put(spectator.getUUID(), data);

        // Make invulnerable (prevents damage while in spectate view)
        spectator.setInvulnerable(true);
        ServerManagementMod.LOGGER.debug("Set invulnerable=true for {} (spectate start)", spectator.getName().getString());
        
        // Switch to SPECTATOR game mode for BOTH modes. This:
        //   - Hides first-person hands/hotbar
        //   - Locks view direction to the camera target (no independent head movement)
        //   - Makes body non-collidable (noPhysics = true)
        //   - Makes body invisible to non-spectators (broadcastToPlayer returns false)
        spectator.setGameMode(GameType.SPECTATOR);
        
        // STEALTH TAB LIST FIX: setGameMode() broadcasts UPDATE_GAME_MODE(SPECTATOR)
        // to ALL clients, which makes the spectator's name italic+gray in the tab list.
        // Send a fake UPDATE_GAME_MODE packet to all OTHER clients with the original
        // game mode so they see the player as survival/creative. The spectator's own
        // client already received the real SPECTATOR mode via ClientboundGameEventPacket.
        broadcastFakeGameMode(spectator, data.gameMode);
        
        if (stealth) {
            // Same-dimension stealth: body stays at original position, only camera changes.
            // DON'T use setCamera() on the server — it calls absMoveTo() every tick,
            // which would drag the body to the target. Instead, send the camera packet
            // directly to the client.
            spectator.connection.send(new ClientboundSetCameraPacket(target));
            
            ServerManagementMod.LOGGER.debug("Stealth spectate: {} watching {} (same dimension)", 
                spectator.getName().getString(), targetName);
        } else {
            
            spectator.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), 
                target.getYRot(), target.getXRot());
            
            // Use a multi-tick delay before calling setCamera.
            // After cross-dimension teleport, the client receives a respawn packet
            // which resets cameraEntity. Entity tracking for the target hasn't been
            // established yet on the new level. We need to wait for the client to:
            //   1. Process the respawn packet (new ClientLevel)
            //   2. Receive entity tracking data for the target
            // Only then can ClientboundSetCameraPacket find the target entity.
            data.pendingReattachTicks = 10;
            
            ServerManagementMod.LOGGER.debug("Cross-dim spectate: {} watching {} (pending reattach)", 
                spectator.getName().getString(), targetName);
        }
        
        spectator.sendSystemMessage(Component.literal(
            String.format("§aNow spectating %s", targetName)));
    }

    public static void stopSpectate(ServerPlayer player) {
        SpectateData data = getInstance().spectating.remove(player.getUUID());
        if (data != null) {
            // Restore invulnerability
            player.setInvulnerable(false);
            ServerManagementMod.LOGGER.debug("Set invulnerable=false for {} (spectate stop)", player.getName().getString());
            
            if (data.stealthMode) {
                // Stealth mode: body is still at original position.
                // Restore game mode, reset camera.
                player.setGameMode(data.gameMode);
                player.connection.send(new ClientboundSetCameraPacket(player));
                // Also reset server-side camera field to be safe
                player.setCamera(player);
            } else {
                // Non-stealth: body was teleported. Restore game mode, reset camera, teleport back.
                player.setGameMode(data.gameMode);
                player.setCamera(player);
                
                // Teleport back to original position/dimension
                var dimensionKey = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(data.dimension)
                );
                var level = getInstance().server.getLevel(dimensionKey);
                if (level != null) {
                    player.teleportTo(level, data.x, data.y, data.z, player.getYRot(), player.getXRot());
                }
            }
            
            player.sendSystemMessage(Component.literal("§aSpectate mode ended."));
        }
    }

    public static void viewInventory(ServerPlayer viewer, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cError: Server not initialized"));
            return;
        }
        
        ServerPlayer target = instance.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                String.format("§cPlayer not found: %s", targetName)));
            return;
        }
        
        // Create a menu provider for viewing the target's inventory
        viewer.openMenu(new PlayerInventoryMenuProvider(target));
    }
    
    public static boolean isSpectating(ServerPlayer player) {
        return getInstance().spectating.containsKey(player.getUUID());
    }
    
    /**
     * Called every server tick to maintain spectating state.
     *
     * Stealth mode (same-dimension):
     *   - Freezes player at original position (resets velocity + position every tick)
     *   - Re-sends camera packet to keep client camera locked
     *   - Cancels spectating if target moves beyond entity tracking range (~450 blocks)
     *
     * Non-stealth mode (cross-dimension):
     *   - Uses pendingReattachTicks to wait for entity tracking after dimension change
     *   - After settle period, calls setCamera to lock camera (body follows target)
     *   - Detects dimension mismatches and re-teleports with new settle period
     */
    public static void tickSpectators() {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null || instance.spectating.isEmpty()) return;
        
        List<UUID> toStop = new ArrayList<>();
        
        for (Map.Entry<UUID, SpectateData> entry : instance.spectating.entrySet()) {
            UUID spectatorUUID = entry.getKey();
            SpectateData data = entry.getValue();
            
            ServerPlayer spectator = instance.server.getPlayerList().getPlayer(spectatorUUID);
            if (spectator == null) {
                toStop.add(spectatorUUID);
                continue;
            }
            
            ServerPlayer target = instance.server.getPlayerList().getPlayer(data.targetUUID);
            if (target == null) {
                // Target disconnected — restore and stop spectating
                spectator.setInvulnerable(false);
                ServerManagementMod.LOGGER.debug("Set invulnerable=false for {} (target disconnected)", spectator.getName().getString());
                if (!data.stealthMode) {
                    spectator.setGameMode(data.gameMode);
                    spectator.setCamera(spectator);
                    var dimensionKey = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(data.dimension)
                    );
                    var level = instance.server.getLevel(dimensionKey);
                    if (level != null) {
                        spectator.teleportTo(level, data.x, data.y, data.z, spectator.getYRot(), spectator.getXRot());
                    }
                } else {
                    spectator.setGameMode(data.gameMode);
                    spectator.connection.send(new ClientboundSetCameraPacket(spectator));
                    spectator.setCamera(spectator);
                }
                spectator.sendSystemMessage(Component.literal("§eSpectate ended: target disconnected."));
                toStop.add(spectatorUUID);
                continue;
            }
            
            // === Pending reattach countdown (cross-dimension settle) ===
            if (data.pendingReattachTicks > 0) {
                data.pendingReattachTicks--;
                if (data.pendingReattachTicks == 0) {
                    // Settle period expired — lock camera now
                    ServerPlayer freshS = instance.server.getPlayerList().getPlayer(spectatorUUID);
                    ServerPlayer freshT = instance.server.getPlayerList().getPlayer(data.targetUUID);
                    if (freshS != null && freshT != null) {
                        if (data.stealthMode) {
                            // Stealth: use direct packet (don't use setCamera which drags body)
                            freshS.connection.send(new ClientboundSetCameraPacket(freshT));
                        } else {
                            freshS.setCamera(freshT);
                        }
                        ServerManagementMod.LOGGER.debug("Spectate camera reattached for {} -> {}", 
                            freshS.getName().getString(), freshT.getName().getString());
                    }
                }
                continue; // Skip other checks while settling
            }
            
            if (data.stealthMode) {
                // === STEALTH MODE: body stays at original position ===
                
                // Safety check: if spectator was somehow moved out of their original dimension,
                // teleport back to saved position
                if (!spectator.level().dimension().location().toString().equals(data.dimension)) {
                    var dimKey = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(data.dimension)
                    );
                    var origLevel = instance.server.getLevel(dimKey);
                    if (origLevel != null) {
                        spectator.teleportTo(origLevel, data.x, data.y, data.z, spectator.getYRot(), spectator.getXRot());
                    }
                }
                
                if (!spectator.level().dimension().equals(target.level().dimension())) {
                    // Target moved to a different dimension. Camera packets won't work
                    // cross-dimension (entity not tracked in spectator's ClientLevel).
                    // Transition to non-stealth mode: teleport body to target's dimension,
                    // keeping the saved original position for when spectating ends.
                    data.stealthMode = false;
                    spectator.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(),
                        target.getYRot(), target.getXRot());
                    data.pendingReattachTicks = 10;
                    // Re-broadcast fake game mode so the teleport doesn't reveal spectator status
                    broadcastFakeGameMode(spectator, data.gameMode);
                    ServerManagementMod.LOGGER.debug("Stealth->non-stealth transition: {} following {} to {}",
                        spectator.getName().getString(), target.getName().getString(),
                        target.level().dimension().location());
                    continue;
                }
                
                // Freeze position: cancel any movement and reset to saved coords
                spectator.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                spectator.absMoveTo(data.x, data.y, data.z, spectator.getYRot(), spectator.getXRot());
                
                // Check if target is still within entity tracking range.
                // Entity tracking is based on the server's view distance. The client
                // loses the target entity when it's beyond viewDistance chunks from the
                // spectator's frozen body position, causing the camera to flicker/freak out.
                // Stop 2 chunks early so the camera never reaches that broken state.
                int viewDistChunks = instance.server.getPlayerList().getViewDistance();
                int safeRange = Math.max((viewDistChunks - 2) * 16, 48);
                double dx = data.x - target.getX();
                double dz = data.z - target.getZ();
                double distSq = dx * dx + dz * dz;
                if (distSq > (double) safeRange * safeRange) {
                    // Target too far — reset camera FIRST for clean exit, then restore state
                    spectator.connection.send(new ClientboundSetCameraPacket(spectator));
                    spectator.setCamera(spectator);
                    spectator.setInvulnerable(false);
                    ServerManagementMod.LOGGER.debug("Set invulnerable=false for {} (target out of range, {}>{} blocks)", 
                        spectator.getName().getString(), (int) Math.sqrt(distSq), safeRange);
                    spectator.setGameMode(data.gameMode);
                    spectator.sendSystemMessage(Component.literal("§eSpectate ended: target moved out of range."));
                    toStop.add(spectatorUUID);
                    continue;
                }
                
                // Re-send camera packet to keep client camera locked
                // (cheap packet: just an entity ID integer)
                spectator.connection.send(new ClientboundSetCameraPacket(target));
                
            } else {
                // === NON-STEALTH MODE: body follows target ===
                
                // Check if target returned to the spectator's original dimension —
                // if so, transition back to stealth mode (teleport body back, freeze in place)
                if (target.level().dimension().location().toString().equals(data.dimension)
                        && !spectator.level().dimension().location().toString().equals(data.dimension)) {
                    // Target is back in original dimension but spectator body is still elsewhere
                    data.stealthMode = true;
                    spectator.setCamera(spectator);
                    var dimKey = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(data.dimension)
                    );
                    var origLevel = instance.server.getLevel(dimKey);
                    if (origLevel != null) {
                        spectator.teleportTo(origLevel, data.x, data.y, data.z, spectator.getYRot(), spectator.getXRot());
                    }
                    // Re-broadcast fake game mode after dimension change
                    broadcastFakeGameMode(spectator, data.gameMode);
                    // Small delay for entity tracking to establish before sending camera packet
                    data.pendingReattachTicks = 5;
                    ServerManagementMod.LOGGER.debug("Non-stealth->stealth transition: {} returning to {} for {}",
                        spectator.getName().getString(), data.dimension, target.getName().getString());
                    continue;
                }
                
                // Check dimension mismatch (target used a portal)
                if (!spectator.level().dimension().equals(target.level().dimension())) {
                    // Reset camera, teleport to target's dimension, start settle period
                    spectator.setCamera(spectator);
                    spectator.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(),
                        target.getYRot(), target.getXRot());
                    data.pendingReattachTicks = 10;
                } else {
                    if (spectator.getCamera() != target) {
                        spectator.setCamera(target);
                    }
                    // Continuously reinforce camera lock via direct packet.
                    // After cross-dimension teleport, the initial setCamera packet may arrive
                    // before the client has entity tracking data for the target. The client
                    // ignores SetCameraPacket if the entity doesn't exist in its level.
                    // Re-sending every tick ensures the camera locks once tracking is established.
                    spectator.connection.send(new ClientboundSetCameraPacket(target));
                }
            }
        }
        
        for (UUID uuid : toStop) {
            instance.spectating.remove(uuid);
        }
    }

    // ===== Kick =====
    
    public static void kickPlayer(ServerPlayer admin, String targetName, String reason) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            admin.sendSystemMessage(Component.literal("§cError: Server not initialized"));
            return;
        }
        
        ServerPlayer target = instance.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            admin.sendSystemMessage(Component.literal("§cPlayer not found or not online: " + targetName));
            return;
        }
        
        String kickMsg = reason.isEmpty() ? "Kicked by " + admin.getName().getString() 
                                           : "Kicked: " + reason;
        target.connection.disconnect(Component.literal(kickMsg));
        admin.sendSystemMessage(Component.literal("§aKicked " + targetName));
        ServerManagementMod.LOGGER.info("{} kicked {} (reason: {})", admin.getName().getString(), targetName, 
            reason.isEmpty() ? "none" : reason);
    }
    
    // ===== Ban =====
    
    public static void banPlayer(ServerPlayer admin, String targetName, String reason, boolean banIP) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            admin.sendSystemMessage(Component.literal("§cError: Server not initialized"));
            return;
        }
        
        // Resolve the player's game profile (they may or may not be online)
        ServerPlayer target = instance.server.getPlayerList().getPlayerByName(targetName);
        com.mojang.authlib.GameProfile profile = null;
        
        if (target != null) {
            profile = target.getGameProfile();
        } else {
            // Try to find from cached profiles
            var cached = instance.server.getProfileCache();
            if (cached != null) {
                var opt = cached.get(targetName);
                if (opt.isPresent()) {
                    profile = opt.get();
                }
            }
        }
        
        if (profile == null) {
            admin.sendSystemMessage(Component.literal("§cCould not find player profile: " + targetName));
            return;
        }
        
        String banReason = reason.isEmpty() ? "Banned by " + admin.getName().getString() : reason;
        
        // Add to ban list
        UserBanListEntry banEntry = new UserBanListEntry(profile, null, admin.getName().getString(), null, banReason);
        instance.server.getPlayerList().getBans().add(banEntry);
        
        // Also ban IP if requested
        if (banIP && target != null) {
            String ip = target.getIpAddress();
            if (ip != null && !ip.isEmpty()) {
                IpBanListEntry ipBan = new IpBanListEntry(ip, null, admin.getName().getString(), null, banReason);
                instance.server.getPlayerList().getIpBans().add(ipBan);
                admin.sendSystemMessage(Component.literal("§aBanned " + targetName + " + IP (" + ip + ")"));
            } else {
                admin.sendSystemMessage(Component.literal("§aBanned " + targetName + " (IP ban failed: could not get IP)"));
            }
        } else if (banIP) {
            admin.sendSystemMessage(Component.literal("§aBanned " + targetName + " (IP ban skipped: player offline)"));
        } else {
            admin.sendSystemMessage(Component.literal("§aBanned " + targetName));
        }
        
        // Kick if online
        if (target != null) {
            target.connection.disconnect(Component.literal("Banned: " + banReason));
        }
        
        ServerManagementMod.LOGGER.info("{} banned {} (reason: {}, ip: {})", 
            admin.getName().getString(), targetName, banReason, banIP);
    }
    
    // ===== Unban =====
    
    public static void unbanPlayer(ServerPlayer admin, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            admin.sendSystemMessage(Component.literal("§cError: Server not initialized"));
            return;
        }
        
        var banList = instance.server.getPlayerList().getBans();
        
        // Look up profile from cache
        var cached = instance.server.getProfileCache();
        if (cached != null) {
            var opt = cached.get(targetName);
            if (opt.isPresent()) {
                com.mojang.authlib.GameProfile profile = opt.get();
                if (banList.isBanned(profile)) {
                    banList.remove(profile);
                    admin.sendSystemMessage(Component.literal("§aUnbanned " + targetName));
                    ServerManagementMod.LOGGER.info("{} unbanned {}", admin.getName().getString(), targetName);
                    return;
                }
            }
        }
        
        // Fallback: try to find by reading banned-players.json
        com.mojang.authlib.GameProfile profile = findBannedProfile(instance.server, targetName);
        if (profile != null) {
            banList.remove(profile);
            admin.sendSystemMessage(Component.literal("§aUnbanned " + targetName));
            ServerManagementMod.LOGGER.info("{} unbanned {}", admin.getName().getString(), targetName);
        } else {
            admin.sendSystemMessage(Component.literal("§c" + targetName + " is not banned"));
        }
    }
    
    // ===== Whitelist =====
    
    public static void addToWhitelist(ServerPlayer admin, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            admin.sendSystemMessage(Component.literal("§cError: Server not initialized"));
            return;
        }
        
        var cached = instance.server.getProfileCache();
        if (cached == null) {
            admin.sendSystemMessage(Component.literal("§cProfile cache not available"));
            return;
        }
        
        var opt = cached.get(targetName);
        if (opt.isEmpty()) {
            admin.sendSystemMessage(Component.literal("§cCould not find player profile: " + targetName));
            return;
        }
        
        com.mojang.authlib.GameProfile profile = opt.get();
        UserWhiteListEntry entry = new UserWhiteListEntry(profile);
        instance.server.getPlayerList().getWhiteList().add(entry);
        admin.sendSystemMessage(Component.literal("§aAdded " + targetName + " to whitelist"));
        ServerManagementMod.LOGGER.info("{} added {} to whitelist", admin.getName().getString(), targetName);
    }
    
    public static void removeFromWhitelist(ServerPlayer admin, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            admin.sendSystemMessage(Component.literal("§cError: Server not initialized"));
            return;
        }
        
        var whiteList = instance.server.getPlayerList().getWhiteList();
        
        // Look up profile from cache
        var cached = instance.server.getProfileCache();
        if (cached != null) {
            var opt = cached.get(targetName);
            if (opt.isPresent()) {
                com.mojang.authlib.GameProfile profile = opt.get();
                if (whiteList.isWhiteListed(profile)) {
                    whiteList.remove(profile);
                    admin.sendSystemMessage(Component.literal("§aRemoved " + targetName + " from whitelist"));
                    ServerManagementMod.LOGGER.info("{} removed {} from whitelist", admin.getName().getString(), targetName);
                    return;
                }
            }
        }
        
        admin.sendSystemMessage(Component.literal("§c" + targetName + " is not on the whitelist"));
    }
    
    // ===== Sync lists to client =====
    
    public static void sendPlayerLists(ServerPlayer admin) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) return;
        
        List<String> banned = readNamesFromJsonFile(instance.server, "banned-players.json");
        List<String> whitelisted = readNamesFromJsonFile(instance.server, "whitelist.json");
        boolean whitelistEnabled = instance.server.getPlayerList().isUsingWhitelist();
        
        ModNetworking.sendToPlayer(new PMSyncPlayerListsPacket(banned, whitelisted, whitelistEnabled), admin);
    }
    
    /**
     * Read player names from a vanilla server JSON file (banned-players.json, whitelist.json).
     * These files contain JSON arrays of objects with a "name" field.
     */
    private static List<String> readNamesFromJsonFile(MinecraftServer server, String fileName) {
        List<String> names = new ArrayList<>();
        try {
            java.io.File file = new java.io.File(fileName);
            if (!file.exists()) return names;
            
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonArray array = com.google.gson.JsonParser.parseString(content).getAsJsonArray();
            for (com.google.gson.JsonElement element : array) {
                com.google.gson.JsonObject obj = element.getAsJsonObject();
                if (obj.has("name")) {
                    names.add(obj.get("name").getAsString());
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.warn("Failed to read {}: {}", fileName, e.getMessage());
        }
        return names;
    }
    
    /**
     * Find a banned player's profile by reading banned-players.json.
     */
    private static com.mojang.authlib.GameProfile findBannedProfile(MinecraftServer server, String playerName) {
        try {
            java.io.File file = new java.io.File("banned-players.json");
            if (!file.exists()) return null;
            
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonArray array = com.google.gson.JsonParser.parseString(content).getAsJsonArray();
            for (com.google.gson.JsonElement element : array) {
                com.google.gson.JsonObject obj = element.getAsJsonObject();
                if (obj.has("name") && obj.get("name").getAsString().equalsIgnoreCase(playerName) && obj.has("uuid")) {
                    UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                    return new com.mojang.authlib.GameProfile(uuid, obj.get("name").getAsString());
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.warn("Failed to read banned-players.json for unban: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Sends a fake UPDATE_GAME_MODE packet to all clients EXCEPT the spectator,
     * showing them the specified game mode instead of SPECTATOR. This prevents the
     * tab list from showing italic+gray text (spectator styling) for the player.
     * The spectator's own client already knows its real game mode via
     * ClientboundGameEventPacket.CHANGE_GAME_MODE sent by setGameMode().
     */
    private static void broadcastFakeGameMode(ServerPlayer spectator, GameType fakeMode) {
        MinecraftServer server = spectator.getServer();
        if (server == null) return;
        
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),
                ClientboundPlayerInfoUpdatePacket.Action.class);
            buf.writeVarInt(1);
            buf.writeUUID(spectator.getUUID());
            buf.writeVarInt(fakeMode.getId());
            ClientboundPlayerInfoUpdatePacket fakePacket = new ClientboundPlayerInfoUpdatePacket(buf);
            
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.getUUID().equals(spectator.getUUID())) {
                    player.connection.send(fakePacket);
                }
            }
        } finally {
            buf.release();
        }
    }
    
    /**
     * Called when a new player joins. Clears stale invulnerability from previous
     * spectate sessions (e.g. server crash while spectating), and sends fake game
     * mode packets for all active spectators so the joining player's tab list
     * shows the original game mode instead of SPECTATOR (italic+gray).
     */
    public static void onPlayerJoined(ServerPlayer joiningPlayer) {
        PlayerManagerSingleton instance = getInstance();
        
        // Safety net: clear stale invulnerability from abnormal shutdowns.
        // Entity.invulnerable is persisted to NBT. If the server crashed while
        // a player was spectating, their saved data still has Invulnerable:1b.
        // Survival/adventure players should never have this flag outside spectating.
        if (joiningPlayer.isInvulnerable() && !instance.spectating.containsKey(joiningPlayer.getUUID())) {
            joiningPlayer.setInvulnerable(false);
            ServerManagementMod.LOGGER.warn("Cleared stale invulnerability for {} on join (likely from interrupted spectate session)",
                joiningPlayer.getName().getString());
        }
        
        if (instance.server == null || instance.spectating.isEmpty()) return;
        
        for (Map.Entry<UUID, SpectateData> entry : instance.spectating.entrySet()) {
            UUID spectatorUUID = entry.getKey();
            SpectateData data = entry.getValue();
            
            // Don't send fake mode to the spectator themselves
            if (spectatorUUID.equals(joiningPlayer.getUUID())) continue;
            
            ServerPlayer spectator = instance.server.getPlayerList().getPlayer(spectatorUUID);
            if (spectator == null) continue;
            
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),
                    ClientboundPlayerInfoUpdatePacket.Action.class);
                buf.writeVarInt(1);
                buf.writeUUID(spectatorUUID);
                buf.writeVarInt(data.gameMode.getId());
                ClientboundPlayerInfoUpdatePacket fakePacket = new ClientboundPlayerInfoUpdatePacket(buf);
                joiningPlayer.connection.send(fakePacket);
            } finally {
                buf.release();
            }
        }
    }

    private static class SpectateData {
        double x, y, z;
        String dimension;
        GameType gameMode;
        UUID targetUUID;
        boolean stealthMode; // true = same-dimension, body stays at original position
        int pendingReattachTicks; // >0 = waiting for cross-dimension camera settle

        SpectateData(double x, double y, double z, String dimension, GameType gameMode, UUID targetUUID, boolean stealthMode) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.gameMode = gameMode;
            this.targetUUID = targetUUID;
            this.stealthMode = stealthMode;
            this.pendingReattachTicks = 0;
        }
    }
}
