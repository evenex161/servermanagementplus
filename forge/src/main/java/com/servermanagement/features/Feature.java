package com.servermanagement.features;

import net.minecraft.server.MinecraftServer;

public interface Feature {
    /**
     * @return Unique identifier for this feature
     */
    String getId();

    /**
     * @return Display name shown in UI
     */
    String getDisplayName();

    /**
     * @return Short description of the feature
     */
    String getDescription();

    /**
     * @return Detailed description with full feature documentation
     */
    String getDetailedDescription();

    /**
     * Initialize the feature when server starts
     */
    void initialize(MinecraftServer server);

    /**
     * Called when feature is enabled
     */
    default void onEnable() {}

    /**
     * Called when feature is disabled
     */
    default void onDisable() {}

    /**
     * @return Whether this feature is currently enabled
     */
    default boolean isEnabled() {
        return FeatureManager.isFeatureEnabled(getId());
    }
}
