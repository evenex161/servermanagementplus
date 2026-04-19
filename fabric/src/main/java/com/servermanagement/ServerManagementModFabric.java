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

        // Server starting
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = server;
            LOGGER.info("ServerManagement v{} starting (Data Version: {})",
                    getModVersion(), com.servermanagement.util.DataVersion.CURRENT_VERSION);

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
            com.servermanagement.server.ModFileTransferManager.initialize();

            if (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor")) {
                com.servermanagement.features.motd.MotdManager.getInstance().initialize(server);
            }

            LOGGER.info("Subsystems initialized: TransactionManager, MineBay, Overflow, MineStacks, OTA"
                    + (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor") ? ", MOTD" : ""));

            com.servermanagement.server.ServerConsoleManager.getInstance().initialize(server);

            LOGGER.info("ServerManagement v{} fully initialized and ready!", getModVersion());
        });

        // Server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.servermanagement.server.ServerConsoleManager.getInstance().tick();
        });

        // Player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            com.servermanagement.server.ServerConsoleManager.getInstance().unsubscribe(handler.getPlayer().getUUID());
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
            MineBayCommand.register(dispatcher);
            MineStacksCommand.register(dispatcher);
            OverflowCommand.register(dispatcher);
            LOGGER.info("Registered GUI commands: /minebay, /minestacks, /casino, /overflow");
        });
    }

    public static ModConfig getConfig() {
        if (config == null) {
            config = new ModConfig();
        }
        return config;
    }
}
