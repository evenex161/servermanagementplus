package com.servermanagement.fabric;

import com.servermanagement.Constants;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint stub for the mc/1.20.1 branch.
 * The Fabric port will be implemented in Phase 3 of the v2.1.0 backport.
 */
public class ServerManagementModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOG.info("{} (Fabric stub) loaded — full port pending.", Constants.MOD_NAME);
    }
}
