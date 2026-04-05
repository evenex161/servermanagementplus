package com.servermanagement.ota;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.VersionCheckPacket;
import com.servermanagement.server.ModFileTransferManager;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles player join events to trigger version checking
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class PlayerJoinListener {
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerManagementMod.LOGGER.debug("{}: {} joined, checking version", 
                ServerManagementMod.MOD_ID, player.getName().getString());
            
            // Check if OTA system is ready
            if (!ModFileTransferManager.isReady()) {
                ServerManagementMod.LOGGER.error("OTA system not ready, skipping version check for {}", player.getName().getString());
                return;
            }
            
            // Load OTA version info
            OTAVersion serverOTAVersion = OTAVersion.loadFromResources();
            
            // Check CurseForge for updates (async, doesn't block player join)
            CurseForgeUpdateChecker.checkForUpdates().thenAccept(updateInfo -> {
                if (updateInfo.updateAvailable) {
                    ServerManagementMod.LOGGER.info("CurseForge update available: {} -> {} ({})",
                        serverOTAVersion.getDisplayVersion(), updateInfo.version.getDisplayVersion(), updateInfo.downloadUrl);
                }
            });
            
            // Send version check packet to client
            String modVersion = serverOTAVersion.getFullVersion(); // Use OTA version with build number
            int dataVersion = DataVersion.CURRENT_VERSION;
            String jarName = ModFileTransferManager.getModJarName();
            String jarHash = ModFileTransferManager.getModJarHash();
            long jarSize = ModFileTransferManager.getModJarSize();
            String mcVersion = serverOTAVersion.getMinecraftVersion();
            String modLoader = serverOTAVersion.getModLoader();
            
            ServerManagementMod.LOGGER.debug("Server OTA: {} (build {}, MC {}, loader {}), JAR: {} ({} bytes)", 
                serverOTAVersion.getVersion(), serverOTAVersion.getBuildNumber(), mcVersion, modLoader, jarName, jarSize);
            
            VersionCheckPacket packet = new VersionCheckPacket(
                modVersion,
                dataVersion,
                jarName,
                jarHash,
                jarSize,
                mcVersion,
                modLoader
            );
            
            ModNetworking.sendToPlayer(packet, player);
            
            ServerManagementMod.LOGGER.debug("Version check packet sent to {}", player.getName().getString());
        }
    }
}
