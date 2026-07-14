package com.servermanagement.fabric;

import com.servermanagement.updater.UpdateInfo;
import com.servermanagement.updater.UpdateManager;
import com.servermanagement.updater.UpdatePreferences;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class FabricUpdateHooks {
    private static boolean updateChecked = false;
    private static UpdateInfo pendingUpdate = null;

    public static void register() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            UpdatePreferences.load();
            UpdateManager.checkForUpdates("2.0.0", "fabric", "1.20.1")
                .thenAccept(optInfo -> optInfo.ifPresent(info -> {
                    if (!UpdatePreferences.isSkipped(info.version())) {
                        pendingUpdate = info;
                    }
                }));
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen && pendingUpdate != null && !updateChecked) {
                updateChecked = true;
                UpdateInfo info = pendingUpdate;
                pendingUpdate = null;
                client.tell(() -> {
                    client.setScreen(new com.servermanagement.client.UpdateAvailableScreen(screen, info, "2.1.0"));
                });
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            UpdateManager.checkUpdateSuccessState("2.1.0");
            com.servermanagement.updater.ServerUpdateScheduler.start("fabric");
        });
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            if (player.hasPermissions(2)) {
                if (UpdateManager.justUpdated) {
                    player.sendSystemMessage(Component.literal("§a[ServerManagement] Successfully updated to version 2.1.0!"));
                    UpdateManager.justUpdated = false;
                }
                
                if (com.servermanagement.updater.ServerUpdateScheduler.pendingUpdate != null) {
                    var info = com.servermanagement.updater.ServerUpdateScheduler.pendingUpdate;
                    net.minecraft.network.chat.MutableComponent msg = Component.literal("§e[ServerManagement] A new update (" + info.version() + ") is available! ")
                        .append(Component.literal("§a[Update Now]")
                            .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/sm update gui"))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Open Updater GUI")))
                            ))
                        .append(" ")
                        .append(Component.literal("§c[Skip Version]")
                            .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/sm update skip"))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Skip this version")))
                            ));
                    player.sendSystemMessage(msg);
                }
            }
        });
    }
}
