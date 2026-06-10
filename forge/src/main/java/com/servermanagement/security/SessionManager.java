package com.servermanagement.security;

import com.servermanagement.ServerManagementMod;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages secure sessions between server and clients.
 * Each client gets a unique session token that must be included in all packets.
 */
public class SessionManager {
    private static SessionManager instance;
    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Create a new session for a player
     */
    public PlayerSession createSession(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        String token = EncryptionManager.getInstance().generateSecureToken();
        
        PlayerSession session = new PlayerSession(playerUUID, token);
        sessions.put(playerUUID, session);
        
        ServerManagementMod.LOGGER.debug("Created session for player: {}", player.getName().getString());
        return session;
    }

    /**
     * Get a player's session
     */
    public PlayerSession getSession(UUID playerUUID) {
        return sessions.get(playerUUID);
    }

    /**
     * Validate a session token
     */
    public boolean validateSession(UUID playerUUID, String token) {
        PlayerSession session = sessions.get(playerUUID);
        if (session == null) {
            return false;
        }

        // Check if session expired
        if (session.isExpired()) {
            sessions.remove(playerUUID);
            return false;
        }

        // Validate token
        boolean valid = session.getToken().equals(token);
        if (valid) {
            session.updateLastActivity();
        }

        return valid;
    }

    /**
     * Invalidate a player's session (on logout)
     */
    public void invalidateSession(UUID playerUUID) {
        sessions.remove(playerUUID);
        ServerManagementMod.LOGGER.debug("Invalidated session for player: {}", playerUUID);
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Check if player has admin permissions
     */
    public boolean hasAdminPermission(ServerPlayer player) {
        return player.hasPermissions(2); // Operator level 2+
    }

    /**
     * Authenticate a session using a client-provided token
     */
    public boolean authenticateSession(UUID playerUUID, String token) {
        PlayerSession session = sessions.get(playerUUID);
        if (session != null && session.getToken().equals(token) && !session.isExpired()) {
            session.setAuthenticated(true);
            session.updateLastActivity();
            return true;
        }
        return false;
    }

    /**
     * Check if a session has been successfully authenticated
     */
    public boolean isSessionAuthenticated(UUID playerUUID) {
        PlayerSession session = sessions.get(playerUUID);
        return session != null && session.isAuthenticated() && !session.isExpired();
    }

    /**
     * Represents a player's session
     */
    public static class PlayerSession {
        private final UUID playerUUID;
        private final String token;
        private final long createdAt;
        private long lastActivity;
        private boolean authenticated = false;

        public PlayerSession(UUID playerUUID, String token) {
            this.playerUUID = playerUUID;
            this.token = token;
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = createdAt;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public String getToken() {
            return token;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT;
        }
    }
}
