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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ServerManagementMod.MOD_ID)
public class ServerManagementMod {
    public static final String MOD_ID = "servermanagement";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static String getModVersion() {
        return net.neoforged.fml.ModList.get().getModContainerById(MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
    
    private static ModConfig config;

    public ServerManagementMod(IEventBus modEventBus) {
        // Register setup handlers
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // Register menu types
        ModMenuTypes.register(modEventBus);
        
        // Register ourselves for server and other game events
        NeoForge.EVENT_BUS.register(this);
        
        // Register config
        ModLoadingContext.get().getActiveContainer().registerConfig(Type.COMMON, ModConfig.SPEC);
        
        // Validate and repair config if necessary
        boolean configValid = com.servermanagement.config.ConfigValidator.validateAndRepair();
        if (!configValid) {
            LOGGER.error("Configuration validation failed! Mod may not function correctly.");
        }
        
        // Cleanup old config backups
        com.servermanagement.config.ConfigValidator.cleanupOldBackups();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("ServerManagement mod common setup");
        
        event.enqueueWork(() -> {
            // Load config
            config = new ModConfig();
            
            // Register features
            FeatureRegistry.registerFeatures();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.debug("ServerManagement mod client setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("ServerManagement v{} starting (Data Version: {})", 
            getModVersion(), com.servermanagement.util.DataVersion.CURRENT_VERSION);
        
        try {
            File serverDir = event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
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
            com.servermanagement.util.VersionTracker.loadOrCreate(event.getServer());
        LOGGER.debug("Installation info - First: {}, Last Updated: {}", 
            versionInfo.firstInstalled, versionInfo.lastUpdated);
        
        FeatureRegistry.initializeFeatures(event.getServer());
        
        com.servermanagement.features.economy.TransactionManager.getInstance().initialize(event.getServer());
        com.servermanagement.features.minebay.MineBayManager.getInstance().initialize(event.getServer());
        com.servermanagement.features.economy.OverflowInventoryManager.getInstance().initialize(event.getServer());
        com.servermanagement.features.gambling.GamblingManager.getInstance().initialize(event.getServer());
        com.servermanagement.server.ModFileTransferManager.initialize();
        
        if (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor")) {
            com.servermanagement.features.motd.MotdManager.getInstance().initialize(event.getServer());
        }
        
        LOGGER.info("Subsystems initialized: TransactionManager, MineBay, Overflow, MineStacks, OTA" 
            + (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor") ? ", MOTD" : ""));
        
        com.servermanagement.server.ServerConsoleManager.getInstance().initialize(event.getServer());
        
        LOGGER.info("ServerManagement v{} fully initialized and ready!", getModVersion());
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        com.servermanagement.server.ServerConsoleManager.getInstance().tick();
    }
    
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
            com.servermanagement.server.ServerConsoleManager.getInstance().unsubscribe(event.getEntity().getUUID());
        }
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("ServerManagement shutting down...");
        
        com.servermanagement.server.ServerConsoleManager.getInstance().shutdown();
        com.servermanagement.features.motd.MotdManager.getInstance().saveAndShutdown();
        com.servermanagement.features.gambling.GamblingManager.getInstance().shutdown();
        com.servermanagement.features.economy.OverflowInventoryManager.getInstance().save();
        
        LOGGER.info("ServerManagement shutdown complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MineBayCommand.register(event.getDispatcher());
        MineStacksCommand.register(event.getDispatcher());
        OverflowCommand.register(event.getDispatcher());
        LOGGER.info("Registered GUI commands: /minebay, /minestacks, /casino, /overflow");
    }
    
    public static ModConfig getConfig() {
        if (config == null) {
            config = new ModConfig();
        }
        return config;
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }
}
