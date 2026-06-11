package com.servermanagement.paper;

import com.servermanagement.Constants;
import com.servermanagement.platform.Services;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerManagementPaperPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ServerManagementPaperPlugin enabled! Running on platform: " + Services.PLATFORM.getPlatformName());
    }

    @Override
    public void onDisable() {
        getLogger().info("ServerManagementPaperPlugin disabled!");
    }
}
