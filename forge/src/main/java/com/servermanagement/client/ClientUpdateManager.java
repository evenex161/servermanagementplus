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
}
