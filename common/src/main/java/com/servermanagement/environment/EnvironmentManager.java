package com.servermanagement.environment;

public class EnvironmentManager {

    private static boolean isDedicatedServer = false;
    private static boolean isVanillaClient = false;
    private static boolean isServerManagementClient = false;

    public static void setDedicatedServer(boolean dedicated) {
        isDedicatedServer = dedicated;
    }

    public static boolean isDedicatedServer() {
        return isDedicatedServer;
    }

    public static void setVanillaClient(boolean vanilla) {
        isVanillaClient = vanilla;
    }

    public static boolean isVanillaClient() {
        return isVanillaClient;
    }

    public static void setServerManagementClient(boolean smClient) {
        isServerManagementClient = smClient;
    }

    public static boolean isServerManagementClient() {
        return isServerManagementClient;
    }
}
