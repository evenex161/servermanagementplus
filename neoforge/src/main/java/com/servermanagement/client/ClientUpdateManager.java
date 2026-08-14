package com.servermanagement.client;

import com.servermanagement.network.packet.SyncUpdateInfoPacket;

public class ClientUpdateManager {
    public static SyncUpdateInfoPacket latestUpdateInfo = null;
    
    public static void receiveUpdateInfo(SyncUpdateInfoPacket packet) {
        latestUpdateInfo = packet;
        // if GUI is open, tell it to refresh
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof com.servermanagement.gui.screen.UpdaterScreen screen) {
            screen.onUpdateInfoReceived(packet);
        }
    }
    
    public static void startDownloadHandoff(net.minecraft.client.Minecraft mc, String currentVersion, com.servermanagement.updater.UpdateInfo target, boolean isClient) {
        java.nio.file.Path currentJar = net.neoforged.fml.ModList.get().getModFileById("servermanagement").getFile().getFilePath();
        com.servermanagement.client.OTAUpdateScreen otaScreen = new com.servermanagement.client.OTAUpdateScreen(currentVersion, target.version(), 0);
        mc.setScreen(otaScreen);
        com.servermanagement.updater.UpdateManager.downloadAndHandoff(target.downloadUrl(), isClient, currentJar,
            (progress, status) -> otaScreen.updateProgress(progress, status),
            () -> otaScreen.setComplete(),
            (error) -> otaScreen.setFailed(error)
        );
    }
    
    public static void checkForUpdates(net.minecraft.client.Minecraft mc, String currentVersion, net.minecraft.client.gui.screens.Screen previousScreen) {
        String loader = com.servermanagement.platform.Services.PLATFORM.getPlatformName().toLowerCase();
        String mcVersion = net.minecraft.SharedConstants.getCurrentVersion().getName();
        com.servermanagement.updater.UpdateManager.checkAllUpdates(currentVersion, loader, mcVersion).thenAccept(result -> {
            if (result == null || (result.modrinth() == null && result.curseforge() == null)) {
                mc.execute(() -> mc.setScreen(previousScreen));
                return;
            }
            com.servermanagement.updater.UpdateInfo target = result.resolve(com.servermanagement.updater.UpdatePreferences.getMainSource(), com.servermanagement.updater.UpdatePreferences.isCheckFallback());
            if (target != null) {
                mc.execute(() -> mc.setScreen(new UpdateAvailableScreen(previousScreen, result, target, currentVersion)));
            } else {
                mc.execute(() -> mc.setScreen(previousScreen));
            }
        });
    }
}
