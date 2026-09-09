package com.servermanagement;

import java.io.File;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.servermanagement.commands.MineBayCommand;
import com.servermanagement.commands.MineStacksCommand;
import com.servermanagement.commands.OverflowCommand;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.FeatureRegistry;
import com.servermanagement.gui.ModMenuTypes;
import com.servermanagement.network.ModNetworking;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class ServerManagementModFabric implements ModInitializer {
    public static final String MOD_ID = "servermanagement";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static MinecraftServer currentServer;
    private static ModConfig config;

    public static String getModVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static MinecraftServer getServer() {
        return currentServer;
    }

    @Override
    public void onInitialize() {
        LOGGER.debug("ServerManagement mod initializing on Fabric");

        // Register menu types
        ModMenuTypes.register();

        // Register networking
        ModNetworking.registerServerPackets();

        // Register config
        config = new ModConfig();

        // Validate and repair config
        boolean configValid = com.servermanagement.config.ConfigValidator.validateAndRepair();
        if (!configValid) {
            LOGGER.error("Configuration validation failed! Mod may not function correctly.");
        }
        com.servermanagement.config.ConfigValidator.cleanupOldBackups();

        // Register features
        FeatureRegistry.registerFeatures();

        // Register updater hooks
        com.servermanagement.fabric.FabricUpdateHooks.register();

        // Server starting
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = server;
            LOGGER.info("ServerManagement v{} starting (Data Version: {})",
                    getModVersion(), com.servermanagement.util.DataVersion.CURRENT_VERSION);

            // Start script generator
            com.servermanagement.updater.StartScriptGenerator.cleanupIfNeeded();

            try {
                File serverDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
                com.servermanagement.security.EncryptionManager.getInstance().initialize(serverDir);
                LOGGER.info("Encryption system initialized");

                File dataDir = new File(serverDir, "data/servermanagement");
                if (dataDir.exists()) {
                    com.servermanagement.security.SecureDataStorage.migrateDirectory(dataDir);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to initialize encryption system", e);
            }

            com.servermanagement.util.VersionTracker.VersionInfo versionInfo =
                    com.servermanagement.util.VersionTracker.loadOrCreate(server);
            LOGGER.debug("Installation info - First: {}, Last Updated: {}",
                    versionInfo.firstInstalled, versionInfo.lastUpdated);

            FeatureRegistry.initializeFeatures(server);

            com.servermanagement.features.economy.TransactionManager.getInstance().initialize(server);
            com.servermanagement.features.minebay.MineBayManager.getInstance().initialize(server);
            com.servermanagement.features.economy.OverflowInventoryManager.getInstance().initialize(server);
            com.servermanagement.features.gambling.GamblingManager.getInstance().initialize(server);

            if (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor")) {
                com.servermanagement.features.motd.MotdManager.getInstance().initialize(server);
            }

            LOGGER.info("Subsystems initialized: TransactionManager, MineBay, Overflow, MineStacks, OTA"
                    + (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor") ? ", MOTD" : ""));

            com.servermanagement.server.ServerConsoleManager.getInstance().initialize(server);

            LOGGER.info("ServerManagement v{} fully initialized and ready!", getModVersion());
        });

        // Server tick — drive subsystems that previously had no wiring on Fabric
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.servermanagement.server.ServerConsoleManager.getInstance().tick();
            try {
                com.servermanagement.features.worldmanager.TabListIsolationHandler.onServerTick(server);
            } catch (Throwable t) {
                LOGGER.error("TabListIsolationHandler tick failed", t);
            }
            try {
                com.servermanagement.features.playermanager.PlayerManagerSingleton.tickSpectators();
            } catch (Throwable t) {
                LOGGER.error("Spectator tick failed", t);
            }
            try {
                com.servermanagement.features.worldmanager.WorldManager.getInstance().tick();
            } catch (Throwable t) {
                LOGGER.error("WorldManager tick failed", t);
            }
            // Bug 2: Portal timers were never ticking on Fabric — Forge wired this
            // via @SubscribeEvent on TickEvent.ServerTickEvent which has no
            // automatic equivalent here.
            try {
                com.servermanagement.features.worldmanager.TimerTickHandler.onServerTick(server);
            } catch (Throwable t) {
                LOGGER.error("TimerTickHandler tick failed", t);
            }
            // Daily task: passive distance-traveled tracking. Forge wires
            // PlayerMovementTracker via @SubscribeEvent on PlayerTickEvent;
            // Fabric needs an explicit dispatch from the server tick loop or
            // TRAVEL_DISTANCE tasks would never accumulate any progress.
            try {
                com.servermanagement.features.economy.PlayerMovementTracker.onPlayerTick(server);
            } catch (Throwable t) {
                LOGGER.error("PlayerMovementTracker tick failed", t);
            }
        });

        // Daily task: block-break tracking. Forge fires DailyTaskProgressListener
        // via @SubscribeEvent on BlockEvent.BreakEvent; Fabric needs explicit
        // wiring through PlayerBlockBreakEvents.AFTER. Without this, BREAK_BLOCKS
        // and MINE_ORES tasks could never make any progress.
        java.util.concurrent.atomic.AtomicBoolean firstBreakLogged = new java.util.concurrent.atomic.AtomicBoolean(false);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register(
            (level, player, pos, state, blockEntity) -> {
                if (firstBreakLogged.compareAndSet(false, true)) {
                    LOGGER.info("[DailyTaskDiag] PlayerBlockBreakEvents.AFTER first fire (player={}, pos={})",
                        player.getName().getString(), pos);
                }
                try {
                    com.servermanagement.features.economy.DailyTaskProgressListener
                        .onBlockBreak(level, player, pos, state);
                } catch (Throwable t) {
                    LOGGER.error("DailyTaskProgressListener.onBlockBreak failed", t);
                }
                // Track block drops for ore-based pricing
                try {
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        com.servermanagement.features.economy.DropRateTracker
                            .onBlockBreak(serverLevel, player, pos, state);
                    }
                } catch (Throwable t) {
                    LOGGER.error("DropRateTracker.onBlockBreak failed", t);
                }
            });

        // Daily task: mob kill tracking. Forge uses @SubscribeEvent on
        // LivingDeathEvent; Fabric needs ServerLivingEntityEvents.AFTER_DEATH.
        // Without this, KILL_MOBS tasks would never advance.
        java.util.concurrent.atomic.AtomicBoolean firstDeathLogged = new java.util.concurrent.atomic.AtomicBoolean(false);
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(
            (entity, source) -> {
                if (firstDeathLogged.compareAndSet(false, true)) {
                    LOGGER.info("[DailyTaskDiag] ServerLivingEntityEvents.AFTER_DEATH first fire (entity={}, source={})",
                        entity.getType().getDescriptionId(),
                        source.getEntity() != null ? source.getEntity().getType().getDescriptionId() : "none");
                }
                try {
                    com.servermanagement.features.economy.DailyTaskProgressListener
                        .onEntityKilled(entity, source);
                } catch (Throwable t) {
                    LOGGER.error("DailyTaskProgressListener.onEntityKilled failed", t);
                }
            });

        // Player join — initialise tab/chat isolation and player-manager state
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer p = handler.getPlayer();
            
            // Enforce mod requirement on the client
            if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, com.servermanagement.network.packet.SyncSessionTokenPacket.ID)) {
                handler.disconnect(net.minecraft.network.chat.Component.literal("§cThis server requires the " + Constants.CHAT_PREFIX + " mod to be installed on your client!"));
                return;
            }
            
            try {
                com.servermanagement.features.worldmanager.TabListIsolationHandler.onPlayerLogin(p);
            } catch (Throwable t) {
                LOGGER.error("TabListIsolationHandler login failed", t);
            }
            try {
                com.servermanagement.features.playermanager.PlayerManagerSingleton.onPlayerJoined(p);
            } catch (Throwable t) {
                LOGGER.error("PlayerManagerSingleton.onPlayerJoined failed", t);
            }
            // Forge ran this via @Mod.EventBusSubscriber on PlayerLoggedInEvent;
            // Fabric needs an explicit dispatch. Without it the player never receives
            // SyncMarketPricesPacket on join, so inventory tooltips fall back to the
            // vanilla rarity-based ItemValuation instead of the recipe-derived prices.
            try {
                com.servermanagement.features.economy.notifications.LoginNotificationHandler.onPlayerLogin(p);
            } catch (Throwable t) {
                LOGGER.error("LoginNotificationHandler.onPlayerLogin failed", t);
            }
            try {
                com.servermanagement.security.SessionEventHandler.onPlayerLogin(p);
            } catch (Throwable t) {
                LOGGER.error("SessionEventHandler login failed", t);
            }
        });

        // Player changed dimension — re-apply tab list isolation
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, dest) -> {
            try {
                com.servermanagement.features.worldmanager.TabListIsolationHandler.onPlayerChangedDimension(player);
            } catch (Throwable t) {
                LOGGER.error("TabListIsolationHandler dimension change failed", t);
            }
            // Bug 1: portal-travel cancellation is now handled at the source via
            // EntityChangeDimensionMixin (HEAD inject on Entity#changeDimension).
            // Nothing to do here.
        });

        // Chat message — apply chat isolation (cancellable via return value)
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            try {
                return com.servermanagement.features.worldmanager.ChatIsolationHandler
                        .onServerChat(sender, message.signedContent());
            } catch (Throwable t) {
                LOGGER.error("ChatIsolationHandler failed", t);
                return true;
            }
        });

        // Right-click block — block portal ignition when disabled
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            try {
                return com.servermanagement.features.worldmanager.PortalEventHandler
                        .onRightClickBlock(player, level, hand, hit);
            } catch (Throwable t) {
                LOGGER.error("PortalEventHandler use-block failed", t);
                return net.minecraft.world.InteractionResult.PASS;
            }
        });

        // Player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            net.minecraft.server.level.ServerPlayer p = handler.getPlayer();
            com.servermanagement.server.ServerConsoleManager.getInstance().unsubscribe(p.getUUID());
            try {
                com.servermanagement.features.worldmanager.TabListIsolationHandler.onPlayerLogout(p);
            } catch (Throwable t) {
                LOGGER.error("TabListIsolationHandler logout failed", t);
            }
            try {
                com.servermanagement.features.playermanager.PlayerManagerSingleton.stopSpectate(p);
                com.servermanagement.network.PacketTimestampTracker.clearPlayer(p.getUUID());
            } catch (Throwable t) {
                LOGGER.error("PlayerManagerSingleton leave cleanup failed", t);
            }
            try {
                com.servermanagement.security.SessionEventHandler.onPlayerLogout(p);
            } catch (Throwable t) {
                LOGGER.error("SessionEventHandler logout failed", t);
            }
        });

        // Server stopping
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("ServerManagement shutting down...");
            com.servermanagement.server.ServerConsoleManager.getInstance().shutdown();
            com.servermanagement.features.motd.MotdManager.getInstance().saveAndShutdown();
            com.servermanagement.features.gambling.GamblingManager.getInstance().shutdown();
            com.servermanagement.features.economy.OverflowInventoryManager.getInstance().save();
            LOGGER.info("ServerManagement shutdown complete");
            currentServer = null;
        });

        // Command registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.servermanagement.commands.ModCommands.onRegisterCommands(dispatcher);
            MineBayCommand.register(dispatcher);
            MineStacksCommand.register(dispatcher);
            OverflowCommand.register(dispatcher);
            LOGGER.info("Registered all commands");
        });
    }

    public static ModConfig getConfig() {
        if (config == null) {
            config = new ModConfig();
        }
        return config;
    }
}
