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
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen titleScreen) {
            com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(titleScreen.width, titleScreen.height, false, () -> {
                // Trigger manual update check with visual feedback
                Minecraft mc = Minecraft.getInstance();
                String ver = com.servermanagement.ServerManagementMod.getModVersion();
                com.servermanagement.updater.UpdatePreferences.load();
                ClientUpdateManager.checkForUpdates(mc, ver, titleScreen);
            });
            event.addListener(btn);

            if (!updateChecked) {
                updateChecked = true;
                
                // Check for successful update changelog
                String currentVersion = com.servermanagement.ServerManagementMod.getModVersion();
                String lastRun = ClientConfig.getLastRunVersion();
                if (lastRun.isEmpty()) {
                    ClientConfig.setLastRunVersion(currentVersion);
                } else if (!lastRun.equals(currentVersion)) {
                    ClientConfig.setLastRunVersion(currentVersion);
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().setScreen(new com.servermanagement.gui.screen.ChangelogScreen(titleScreen));
                    });
                    return; // skip update check for this launch
                }
                
                com.servermanagement.updater.UpdatePreferences.load();
                String loader = com.servermanagement.platform.Services.PLATFORM.getPlatformName().toLowerCase();
                String mcVersion = net.minecraft.SharedConstants.getCurrentVersion().getName();
                com.servermanagement.updater.UpdateManager.checkAllUpdates(currentVersion, loader, mcVersion).thenAccept(result -> {
                    if (result == null || (result.modrinth() == null && result.curseforge() == null)) return;
                    com.servermanagement.updater.UpdateInfo target = result.resolve(com.servermanagement.updater.UpdatePreferences.getMainSource(), com.servermanagement.updater.UpdatePreferences.isCheckFallback());
                    if (target != null) {
                        btn.setNotification(true);
                        if (!com.servermanagement.updater.UpdatePreferences.isSkipped(target.version())) {
                            Minecraft.getInstance().execute(() -> {
                                Minecraft.getInstance().setScreen(new com.servermanagement.client.UpdateAvailableScreen(titleScreen, result, target, currentVersion));
                            });
                        }
                    }
                });
            }
        } else if (event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen pauseScreen) {
            com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(pauseScreen.width, pauseScreen.height, false, () -> {
                Minecraft.getInstance().setScreen(new com.servermanagement.gui.screen.PerformanceSettingsScreen(
                    new com.servermanagement.gui.menu.PerformanceSettingsMenu(-1, Minecraft.getInstance().player.getInventory()),
                    Minecraft.getInstance().player.getInventory(),
                    net.minecraft.network.chat.Component.translatable("gui.servermanagement.performance_settings")
                ));
            });
            event.addListener(btn);
        }
    }
}