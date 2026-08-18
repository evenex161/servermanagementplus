package com.servermanagement.forge;

import com.servermanagement.Constants;
import com.servermanagement.updater.UpdateInfo;
import com.servermanagement.updater.UpdateManager;
import com.servermanagement.updater.UpdatePreferences;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.network.chat.Component;

public class ForgeUpdateHooks {
    
    private static boolean updateChecked = false;
    private static UpdateInfo pendingUpdate = null;

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                UpdatePreferences.load();
                UpdateManager.checkForUpdates("2.1.2-b1", "forge", "1.20.1")
                    .thenAccept(optInfo -> optInfo.ifPresent(info -> {
                        if (!UpdatePreferences.isSkipped(info.version())) {
                            pendingUpdate = info;
                        }
                    }));
            });
        }
    }

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen && pendingUpdate != null && !updateChecked) {
                updateChecked = true;
                UpdateInfo info = pendingUpdate;
                pendingUpdate = null;
                net.minecraft.client.Minecraft.getInstance().tell(() -> {
                    net.minecraft.client.Minecraft.getInstance().setScreen(new com.servermanagement.client.UpdateAvailableScreen(event.getScreen(), info, "2.1.2-b1"));
                });
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            UpdateManager.checkUpdateSuccessState("2.1.2-b1");
            com.servermanagement.updater.ServerUpdateScheduler.start("forge");
        }
        
        @SubscribeEvent
        public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                if (player.hasPermissions(2)) {
                    if (UpdateManager.justUpdated) {
                        player.sendSystemMessage(Component.literal("§a[ServerManagement] Successfully updated to version 2.1.2-b1!"));
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
            }
        }
    }
}
