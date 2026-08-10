package com.servermanagement.updater;

import com.servermanagement.Constants;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerUpdateScheduler {
    private static ScheduledExecutorService scheduler;
    private static int currentIntervalHours = 12; // default
    
    public static UpdateInfo pendingUpdate = null;
    
    private static String lastVersion = "2.1.0-b01";
    
    public static void start(String loader, String currentVersion) {
        lastVersion = currentVersion;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        
        // Check interval (e.g. from config, but for now we use the field)
        if (currentIntervalHours <= 0) return;
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ServerManagement-UpdateScheduler");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                UpdatePreferences.load();
                UpdateManager.checkForUpdates(currentVersion, loader, "1.21.1").thenAccept(optInfo -> {
                    optInfo.ifPresent(info -> {
                        if (!UpdatePreferences.isSkipped(info.version())) {
                            pendingUpdate = info;
                            Constants.LOG.info("Automatic update check: New version available for ServerManagement+ ({})!", info.version());
                        }
                    });
                });
            } catch (Exception e) {
                Constants.LOG.warn("Failed to perform automatic update check", e);
            }
        }, 0, currentIntervalHours, TimeUnit.HOURS);
    }
    
    public static void setInterval(int hours, String loader) {
        currentIntervalHours = hours;
        start(loader, lastVersion);
    }
    
    public static int getInterval() {
        return currentIntervalHours;
    }
}
