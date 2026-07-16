package com.servermanagement.updater;

import com.servermanagement.Constants;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerUpdateScheduler {
    private static ScheduledExecutorService scheduler;
    private static int currentIntervalHours = 12; // default
    
    public static UpdateInfo pendingUpdate = null;
    
    public static void start(String loader) {
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
            UpdateManager.checkForUpdates("2.1.1-b01", loader, "1.20.1").thenAccept(optInfo -> {
                optInfo.ifPresent(info -> {
                    if (!UpdatePreferences.isSkipped(info.version())) {
                        pendingUpdate = info;
                    }
                });
            });
        }, 0, currentIntervalHours, TimeUnit.HOURS);
    }
    
    public static void setInterval(int hours, String loader) {
        currentIntervalHours = hours;
        start(loader);
    }
    
    public static int getInterval() {
        return currentIntervalHours;
    }
}
