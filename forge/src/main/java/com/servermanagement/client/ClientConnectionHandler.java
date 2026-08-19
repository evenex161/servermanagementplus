package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handler for connection events
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientConnectionHandler {
    
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerManagementMod.LOGGER.info("Client disconnecting from server");
        
        // Check if this was due to mod version mismatch
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            String disconnectReason = "";
            
            // Check if the disconnect screen shows mod mismatch
            if (mc.screen.getTitle() != null) {
                String title = mc.screen.getTitle().getString();
                ServerManagementMod.LOGGER.info("Disconnect screen title: {}", title);
                
                // Forge shows "Failed to connect" or "Disconnected" when mods don't match
                if (title.contains("Failed to connect") || title.contains("Disconnected")) {
                    ServerManagementMod.LOGGER.info("Potential mod mismatch disconnect detected");
                    // Note: We can't actually intercept this because Forge blocks at protocol level
                    // The OTA system works when player successfully joins with wrong version
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (com.servermanagement.features.serverperformance.GCAdvisor.isUsingSuboptimalGC() && !com.servermanagement.features.serverperformance.GCAdvisor.isDismissed()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.tell(() -> {
                if (mc.player != null) {
                    net.minecraft.network.chat.MutableComponent msg = net.minecraft.network.chat.Component.literal("§c[ServerManagement] Warning: Suboptimal Client JVM GC detected! (" + com.servermanagement.features.serverperformance.GCAdvisor.getDetectedGC().getDisplayName() + ") ")
                        .append(net.minecraft.network.chat.Component.literal("§e[Copy Optimal Flags]")
                            .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, com.servermanagement.features.serverperformance.GCAdvisor.getRecommendedFlags()))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("Copy flags to clipboard for your Launcher")))
                            ))
                        .append(" ")
                        .append(net.minecraft.network.chat.Component.literal("§7[Dismiss]")
                            .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/sm gc dismiss"))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("Dismiss this warning")))
                            ));
                    mc.player.sendSystemMessage(msg);
                }
            });
        }
    }
}
