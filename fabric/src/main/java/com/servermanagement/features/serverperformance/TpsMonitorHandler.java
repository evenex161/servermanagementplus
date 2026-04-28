package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
public class TpsMonitorHandler {

    private static int warningCooldown = 0;
    private static final int WARNING_INTERVAL_TICKS = 600; // 30 seconds between warnings

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        // Fabric calls at END of tick
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
            message = String.format("┬ºc┬ºl[SM] ┬ºcServer TPS critical: ┬ºf%.1f TPS ┬º7(%.1f mspt)%s",
                    tps, mspt, manager.isAutoOptimizeActive() ? " ┬ºe[Auto-Optimize Active]" : "");
        } else {
            message = String.format("┬ºe[SM] ┬ºeServer TPS warning: ┬ºf%.1f TPS ┬º7(%.1f mspt)", tps, mspt);
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
