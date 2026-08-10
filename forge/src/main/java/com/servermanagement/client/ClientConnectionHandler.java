package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handler for connection events
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientConnectionHandler {
    
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerManagementMod.LOGGER.info("Client disconnecting from server");
        
        // Check if this was due to mod version mismatch
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            String disconnectReason = "";
            
            // Check if the disconnect screen shows mod mismatch
            if (mc.screen.getTitle() != null) {
                String title = mc.screen.getTitle().getString();
                ServerManagementMod.LOGGER.info("Disconnect screen title: {}", title);
                
                // Forge shows "Failed to connect" or "Disconnected" when mods don't match
                if (title.contains("Failed to connect") || title.contains("Disconnected")) {
                    ServerManagementMod.LOGGER.info("Potential mod mismatch disconnect detected");
                    // Note: We can't actually intercept this because Forge blocks at protocol level
                    // The OTA system works when player successfully joins with wrong version
                }
            }
        }
    }

    private static boolean updateChecked = false;

    @SubscribeEvent
    public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen titleScreen && !updateChecked) {
            updateChecked = true;
            com.servermanagement.updater.UpdatePreferences.load();
            String version = com.servermanagement.ServerManagementMod.getModVersion();
            com.servermanagement.updater.UpdateManager.checkForUpdates(version, "Forge", "1.21.1").thenAccept(optInfo -> {
                optInfo.ifPresent(info -> {
                    if (!com.servermanagement.updater.UpdatePreferences.isSkipped(info.version())) {
                        Minecraft.getInstance().execute(() -> {
                            Minecraft.getInstance().setScreen(new UpdateAvailableScreen(
                                titleScreen, info, version
                            ));
                        });
                    }
                });
            });
        }
    }
}
