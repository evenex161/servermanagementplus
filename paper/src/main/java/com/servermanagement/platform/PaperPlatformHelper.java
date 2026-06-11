package com.servermanagement.platform;

import com.servermanagement.platform.services.IPlatformHelper;
import org.bukkit.Bukkit;

public class PaperPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Paper";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Bukkit.getPluginManager().isPluginEnabled(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return System.getProperty("servermanagement.dev") != null;
    }
}
