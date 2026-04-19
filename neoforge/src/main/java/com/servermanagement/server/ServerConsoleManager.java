package com.servermanagement.server;

import com.mojang.logging.LogUtils;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.ConsoleResponsePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Captures server log output via Log4j and streams it to players with the console screen open.
 */
public class ServerConsoleManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RECENT_LINES = 50;
    private static final int MAX_LINE_LENGTH = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static ServerConsoleManager instance;

    private final Set<UUID> subscribedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<String> pendingLines = new ConcurrentLinkedQueue<>();
    private final List<String> recentLines = new ArrayList<>();
    private AbstractAppender appender;
    private MinecraftServer server;

    private ServerConsoleManager() {}

    public static ServerConsoleManager getInstance() {
        if (instance == null) {
            instance = new ServerConsoleManager();
        }
        return instance;
    }

    /**
     * Initialize the log capturing appender. Called on server start.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        subscribedPlayers.clear();
        pendingLines.clear();
        recentLines.clear();

        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            appender = new AbstractAppender(
                    "ServerConsoleStreamAppender",
                    null,
                    PatternLayout.createDefaultLayout(),
                    true,
                    Property.EMPTY_ARRAY
            ) {
                @Override
                public void append(LogEvent event) {
                    if (subscribedPlayers.isEmpty()) return;

                    String time = TIME_FMT.format(Instant.ofEpochMilli(event.getTimeMillis()));
                    String level = event.getLevel().name();
                    String loggerName = event.getLoggerName();
                    // Shorten logger name to last segment
                    int lastDot = loggerName.lastIndexOf('.');
                    if (lastDot >= 0) {
                        loggerName = loggerName.substring(lastDot + 1);
                    }
                    String message = event.getMessage().getFormattedMessage();

                    String formatted = "[" + time + "] [" + level + "] [" + loggerName + "] " + message;
                    if (formatted.length() > MAX_LINE_LENGTH) {
                        formatted = formatted.substring(0, MAX_LINE_LENGTH) + "...";
                    }
                    pendingLines.add(formatted);
                }
            };
            appender.start();
            ctx.getConfiguration().getRootLogger().addAppender(appender, org.apache.logging.log4j.Level.INFO, null);
            ctx.updateLoggers();

            LOGGER.debug("Server console log streaming initialized");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize console log streaming", e);
        }
    }

    /**
     * Subscribe a player to receive log output. Sends recent log buffer immediately.
     */
    public void subscribe(ServerPlayer player) {
        subscribedPlayers.add(player.getUUID());

        // Send recent log history
        synchronized (recentLines) {
            for (String line : recentLines) {
                ModNetworking.sendToPlayer(new ConsoleResponsePacket(line), player);
            }
        }
    }

    /**
     * Unsubscribe a player from log output.
     */
    public void unsubscribe(UUID playerId) {
        subscribedPlayers.remove(playerId);
    }

    /**
     * Called on server tick to flush buffered log lines to subscribed players.
     */
    public void tick() {
        if (server == null || subscribedPlayers.isEmpty()) {
            // Still drain the queue to avoid unbounded growth
            pendingLines.clear();
            return;
        }

        String line;
        List<String> batch = new ArrayList<>();
        while ((line = pendingLines.poll()) != null) {
            batch.add(line);
            if (batch.size() >= 20) break; // Cap per tick to avoid lag spikes
        }

        if (batch.isEmpty()) return;

        synchronized (recentLines) {
            recentLines.addAll(batch);
            while (recentLines.size() > MAX_RECENT_LINES) {
                recentLines.remove(0);
            }
        }

        // Send to all subscribed players
        for (UUID uuid : subscribedPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                for (String msg : batch) {
                    ModNetworking.sendToPlayer(new ConsoleResponsePacket(msg), player);
                }
            } else {
                // Player disconnected, remove subscription
                subscribedPlayers.remove(uuid);
            }
        }
    }

    /**
     * Shutdown: remove the appender and clear state.
     */
    public void shutdown() {
        subscribedPlayers.clear();
        pendingLines.clear();

        if (appender != null) {
            try {
                LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
                ctx.getConfiguration().getRootLogger().removeAppender(appender.getName());
                appender.stop();
                ctx.updateLoggers();
            } catch (Exception e) {
                LOGGER.error("Failed to remove console log appender", e);
            }
            appender = null;
        }
        server = null;
    }
}
