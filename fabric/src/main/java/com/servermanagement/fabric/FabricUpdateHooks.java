package com.servermanagement.fabric;

import com.servermanagement.updater.UpdateInfo;
import com.servermanagement.updater.UpdateManager;
import com.servermanagement.updater.UpdatePreferences;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;

public class FabricUpdateHooks {

    public static void register() {

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            UpdateManager.checkUpdateSuccessState("2.1.1-b01");
            com.servermanagement.updater.ServerUpdateScheduler.start("fabric");
        });
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            if (player.hasPermissions(2)) {
                if (UpdateManager.justUpdated) {
                    player.sendSystemMessage(Component.literal("§a[ServerManagement] Successfully updated to version 2.1.1-b01!"));
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
