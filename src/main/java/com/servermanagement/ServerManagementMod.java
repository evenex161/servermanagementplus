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

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ServerManagementMod.MOD_ID)
public class ServerManagementMod {
    public static final String MOD_ID = "servermanagement";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static String getModVersion() {
        return net.minecraftforge.fml.ModList.get().getModContainerById(MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
    
    private static ModConfig config;

    public ServerManagementMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        // Register setup handlers
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // Register menu types
        ModMenuTypes.register(modEventBus);
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
        
        // Register config
        context.registerConfig(Type.COMMON, ModConfig.SPEC);
        
        // Validate and repair config if necessary
        LOGGER.info("Validating configuration...");
        boolean configValid = com.servermanagement.config.ConfigValidator.validateAndRepair();
        if (configValid) {
            LOGGER.info("Configuration is valid");
        } else {
            LOGGER.error("Configuration validation failed! Mod may not function correctly.");
        }
        
        // Cleanup old config backups
        com.servermanagement.config.ConfigValidator.cleanupOldBackups();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ServerManagement mod common setup");
        
        event.enqueueWork(() -> {
            // Initialize networking
            ModNetworking.register();
            
            // Load config
            config = new ModConfig();
            
            // Register features
            FeatureRegistry.registerFeatures();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("ServerManagement mod client setup");
        
        event.enqueueWork(() -> {
            // Register client-side networking
            ModNetworking.registerClientPackets();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("ServerManagement v{} starting (Data Version: {})", 
            getModVersion(), com.servermanagement.util.DataVersion.CURRENT_VERSION);
        
        // Initialize encryption system FIRST (before loading any data)
        try {
            File serverDir = event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            com.servermanagement.security.EncryptionManager.getInstance().initialize(serverDir);
            LOGGER.info("Encryption system initialized");
            
            // Migrate any unencrypted data files
            File dataDir = new File(serverDir, "data/servermanagement");
            if (dataDir.exists()) {
                com.servermanagement.security.SecureDataStorage.migrateDirectory(dataDir);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize encryption system", e);
        }
        
        // Load or create version tracking file (after encryption is ready)
        com.servermanagement.util.VersionTracker.VersionInfo versionInfo = 
            com.servermanagement.util.VersionTracker.loadOrCreate(event.getServer());
        LOGGER.info("Installation info - First: {}, Last Updated: {}", 
            versionInfo.firstInstalled, versionInfo.lastUpdated);
        
        // Initialize features on server start
        FeatureRegistry.initializeFeatures(event.getServer());
        
        // Initialize TransactionManager
        com.servermanagement.features.economy.TransactionManager.getInstance().initialize(event.getServer());
        LOGGER.info("Transaction manager initialized");
        
        // Initialize MineBay manager
        com.servermanagement.features.minebay.MineBayManager.getInstance().initialize(event.getServer());
        LOGGER.info("MineBay marketplace initialized");
        
        // Initialize overflow inventory manager
        com.servermanagement.features.economy.OverflowInventoryManager.getInstance().initialize(event.getServer());
        LOGGER.info("Overflow inventory manager initialized");
        
        // Initialize gambling system
        com.servermanagement.features.gambling.GamblingManager.getInstance().initialize(event.getServer());
        LOGGER.info("MineStacks gambling system initialized");
        
        // Initialize OTA update system
        com.servermanagement.server.ModFileTransferManager.initialize();
        LOGGER.info("OTA update system initialized");
        
        // Initialize MOTD Manager
        if (com.servermanagement.features.FeatureManager.isFeatureEnabled("motd_editor")) {
            com.servermanagement.features.motd.MotdManager.getInstance().initialize(event.getServer());
            LOGGER.info("MOTD Manager initialized");
        }
        
        // Initialize Server Console log streaming
        com.servermanagement.server.ServerConsoleManager.getInstance().initialize(event.getServer());
        LOGGER.info("Server console log streaming initialized");
        
        LOGGER.info("ServerManagement v{} fully initialized and ready!", getModVersion());
    }
    
    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            com.servermanagement.server.ServerConsoleManager.getInstance().tick();
        }
    }
    
    @SubscribeEvent
    public void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
            com.servermanagement.server.ServerConsoleManager.getInstance().unsubscribe(event.getEntity().getUUID());
        }
    }
    
    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        com.servermanagement.server.ServerConsoleManager.getInstance().shutdown();
        LOGGER.info("Server console log streaming shut down");
        
        com.servermanagement.features.motd.MotdManager.getInstance().saveAndShutdown();
        LOGGER.info("MOTD Manager saved and shut down");
        
        com.servermanagement.features.gambling.GamblingManager.getInstance().shutdown();
        LOGGER.info("Gambling stats saved and scheduler shut down");
        
        com.servermanagement.features.economy.OverflowInventoryManager.getInstance().save();
        LOGGER.info("Overflow inventory saved");
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

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Client-specific setup
        }
    }
}
