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
    
    private static boolean clientGcNotified = false;

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        // Client-side GC advisory for all players (shown once per session)
        if (!clientGcNotified) {
            com.servermanagement.features.serverperformance.GCAdvisor.initialize();
            if (com.servermanagement.features.serverperformance.GCAdvisor.isUsingSuboptimalGC()
                    && !com.servermanagement.features.serverperformance.GCAdvisor.isDismissed()) {
                clientGcNotified = true;
                var gcType = com.servermanagement.features.serverperformance.GCAdvisor.getDetectedGC();
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    net.minecraft.network.chat.MutableComponent msg = net.minecraft.network.chat.Component.literal("\n")
                        .append(net.minecraft.network.chat.Component.literal(" [SM+] Performance Tip: ").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD))
                        .append(net.minecraft.network.chat.Component.literal("Your client is using " + gcType.getDisplayName() + "\n").withStyle(net.minecraft.ChatFormatting.YELLOW))
                        .append(net.minecraft.network.chat.Component.literal(" ZGC is recommended for Minecraft -- it reduces\n").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(net.minecraft.network.chat.Component.literal(" lag spikes and stuttering, especially with mods.\n\n").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(net.minecraft.network.chat.Component.literal(" How to switch:\n").withStyle(net.minecraft.ChatFormatting.WHITE))
                        .append(net.minecraft.network.chat.Component.literal(" 1. Open your launcher's JVM Arguments\n").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(net.minecraft.network.chat.Component.literal(" 2. Add: ").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(net.minecraft.network.chat.Component.literal("-XX:+UseZGC -XX:+ZGenerational").withStyle(net.minecraft.ChatFormatting.GREEN))
                        .append(net.minecraft.network.chat.Component.literal("\n 3. Restart your game\n\n").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(net.minecraft.network.chat.Component.literal(" [Got it]").withStyle(net.minecraft.ChatFormatting.AQUA)
                            .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/sm gc client_dismiss"))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    net.minecraft.network.chat.Component.literal("Hide this tip for the rest of this session")))))
                        .append(net.minecraft.network.chat.Component.literal("\n"));
                    mc.player.sendSystemMessage(msg);
                }
            }
        }
    }

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

    private static boolean updateChecked = false;

    @SubscribeEvent
    public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen titleScreen) {
            com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(titleScreen.width, titleScreen.height, false, () -> {
                // Trigger manual update check with visual feedback
                Minecraft mc = Minecraft.getInstance();
                String ver = com.servermanagement.ServerManagementMod.getModVersion();
                com.servermanagement.updater.UpdatePreferences.load();
                ClientUpdateManager.checkForUpdates(mc, ver, titleScreen);
            });
            event.addListener(btn);

            if (!updateChecked) {
                updateChecked = true;
                
                // Check for successful update changelog
                String currentVersion = com.servermanagement.ServerManagementMod.getModVersion();
                String lastRun = ClientConfig.getLastRunVersion();
                if (lastRun.isEmpty()) {
                    ClientConfig.setLastRunVersion(currentVersion);
                } else if (!lastRun.equals(currentVersion)) {
                    ClientConfig.setLastRunVersion(currentVersion);
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().setScreen(new com.servermanagement.gui.screen.ChangelogScreen(titleScreen));
                    });
                    return; // skip update check for this launch
                }
                
                com.servermanagement.updater.UpdatePreferences.load();
                String loader = com.servermanagement.platform.Services.PLATFORM.getPlatformName().toLowerCase();
                String mcVersion = net.minecraft.SharedConstants.getCurrentVersion().getName();
                com.servermanagement.updater.UpdateManager.checkAllUpdates(currentVersion, loader, mcVersion).thenAccept(result -> {
                    if (result == null || (result.modrinth() == null && result.curseforge() == null)) return;
                    com.servermanagement.updater.UpdateInfo target = result.resolve(com.servermanagement.updater.UpdatePreferences.getMainSource(), com.servermanagement.updater.UpdatePreferences.isCheckFallback());
                    if (target != null) {
                        btn.setNotification(true); // Always notify visually if update exists
                        if (!com.servermanagement.updater.UpdatePreferences.isSkipped(target.version())) {
                            Minecraft.getInstance().execute(() -> {
                                Minecraft.getInstance().setScreen(new UpdateAvailableScreen(titleScreen, result, target, currentVersion));
                            });
                        }
                    }
                });
            }
        } else if (event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen pauseScreen) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
                com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(pauseScreen.width, pauseScreen.height, false, () -> {
                    Minecraft.getInstance().setScreen(new com.servermanagement.gui.screen.PerformanceSettingsScreen(
                        new com.servermanagement.gui.menu.PerformanceSettingsMenu(-1, Minecraft.getInstance().player.getInventory()),
                        Minecraft.getInstance().player.getInventory(),
                        net.minecraft.network.chat.Component.translatable("gui.servermanagement.performance_settings")
                    ));
                });
                event.addListener(btn);
            }
        }
    }
}
