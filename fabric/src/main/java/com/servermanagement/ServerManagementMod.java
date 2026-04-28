package com.servermanagement;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Shim class for Fabric ÔÇö delegates to ServerManagementModFabric.
 * Exists so that all references to ServerManagementMod.LOGGER, .getModVersion(), etc. compile unchanged.
 */
public class ServerManagementMod {
    public static final String MOD_ID = ServerManagementModFabric.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static String getModVersion() {
        return ServerManagementModFabric.getModVersion();
    }

    public static MinecraftServer getServer() {
        return ServerManagementModFabric.getServer();
    }
}
