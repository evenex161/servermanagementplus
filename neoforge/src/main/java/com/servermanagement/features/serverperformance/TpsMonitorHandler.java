package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class TpsMonitorHandler {

    private static int warningCooldown = 0;
    private static final int WARNING_INTERVAL_TICKS = 600; // 30 seconds between warnings

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;

        ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
        if (!manager.isInitialized()) return;

        // Always record tick timing for TPS calculation
        manager.recordTick();

        if (!ModConfig.TPS_MONITOR_ENABLED.get()) return;

        // Warn admins when TPS is low
        if (warningCooldown > 0) {
            warningCooldown--;
            return;
        }

        ServerPerformanceManager.TpsStatus status = manager.getTpsStatus();
        if (status == ServerPerformanceManager.TpsStatus.HEALTHY) return;

        double tps = manager.getCurrentTps();
        double mspt = manager.getAverageMspt();

        String message;
        if (status == ServerPerformanceManager.TpsStatus.CRITICAL) {
            message = String.format("§c§l[SM] §cServer TPS critical: §f%.1f TPS §7(%.1f mspt)%s",
                    tps, mspt, manager.isAutoOptimizeActive() ? " §e[Auto-Optimize Active]" : "");
        } else {
            message = String.format("§e[SM] §eServer TPS warning: §f%.1f TPS §7(%.1f mspt)", tps, mspt);
        }

        // Send to all ops
        for (ServerPlayer player : manager.getServer().getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal(message));
            }
        }

        warningCooldown = WARNING_INTERVAL_TICKS;
    }
}
