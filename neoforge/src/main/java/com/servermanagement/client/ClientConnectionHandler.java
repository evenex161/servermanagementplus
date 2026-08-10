package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-side event handler for connection events
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientConnectionHandler {
    
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerManagementMod.LOGGER.info("Client disconnecting from server");
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            if (mc.screen.getTitle() != null) {
                String title = mc.screen.getTitle().getString();
                ServerManagementMod.LOGGER.info("Disconnect screen title: {}", title);
                
                if (title.contains("Failed to connect") || title.contains("Disconnected")) {
                    ServerManagementMod.LOGGER.info("Potential mod mismatch disconnect detected");
                }
            }
        }
    }

    private static boolean updateChecked = false;

    @SubscribeEvent
    public static void onScreenInit(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen titleScreen && !updateChecked) {
            updateChecked = true;
            com.servermanagement.updater.UpdatePreferences.load();
            String version = com.servermanagement.ServerManagementMod.getModVersion();
            com.servermanagement.updater.UpdateManager.checkForUpdates(version, "NeoForge", "1.21.1").thenAccept(optInfo -> {
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