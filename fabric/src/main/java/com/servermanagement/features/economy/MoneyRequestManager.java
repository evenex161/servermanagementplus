package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.security.SecureDataStorage;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.*;

/**
 * Manages money requests between players
 */
public class MoneyRequestManager {
    private int dataVersion = DataVersion.CURRENT_VERSION;
    private List<MoneyRequest> requests = new ArrayList<>();
    
    private static final int MAX_REQUESTS_PER_PLAYER = 10; // Max pending requests per player
    private static final int MAX_TOTAL_REQUESTS = 5000; // Hard cap on total stored requests

    public MoneyRequestManager() {
        this.requests = new ArrayList<>();
    }

    /**
     * Load money requests from disk with automatic decryption
     */
    public static MoneyRequestManager load(MinecraftServer server) {
        File file = getDataFile(server);
        MoneyRequestManager manager = SecureDataStorage.load(file, MoneyRequestManager.class, 
            new MoneyRequestManager());
        
        if (manager.requests == null) {
            manager.requests = new ArrayList<>();
        }
        
        // Check data version and migrate if needed
        if (manager.dataVersion == 0) {
            ServerManagementMod.LOGGER.debug("Migrating legacy MoneyRequestManager data to version {}", 
                DataVersion.CURRENT_VERSION);
            manager.migrateData(0, DataVersion.CURRENT_VERSION);
        } else if (manager.dataVersion < DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.debug("Migrating MoneyRequestManager data from version {} to {}", 
                manager.dataVersion, DataVersion.CURRENT_VERSION);
            manager.migrateData(manager.dataVersion, DataVersion.CURRENT_VERSION);
        } else if (manager.dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.error("MoneyRequestManager data version {} is newer than supported version {}!", 
                manager.dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        manager.dataVersion = DataVersion.CURRENT_VERSION;
        
        // Clean up expired requests
        manager.cleanupExpiredRequests();
        
        ServerManagementMod.LOGGER.debug("Loaded {} money requests (v{})", manager.requests.size(), manager.dataVersion);
        return manager;
    }
    
    /**
     * Migrate data between versions
     */
    private void migrateData(int fromVersion, int toVersion) {
        // Future migrations will be added here
        ServerManagementMod.LOGGER.debug("Migration from v{} to v{} completed for MoneyRequestManager", 
            fromVersion, toVersion);
    }

    /**
     * Save money requests to disk with encryption
     */
    public void save(MinecraftServer server) {
        File file = getDataFile(server);
        SecureDataStorage.save(this, file, MoneyRequestManager.class);
        ServerManagementMod.LOGGER.debug("Saved encrypted money requests data v{}", dataVersion);
    }
    
    public int getDataVersion() {
        return dataVersion;
    }

    private static File getDataFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, "data/servermanagement/money_requests.json");
    }

    /**
     * Create a new money request
     */
    public MoneyRequest createRequest(UUID requesterUUID, UUID targetUUID, double amount, String message) {
        // Check if requester has too many pending requests
        long pendingCount = getPendingRequestsByRequester(requesterUUID).size();
        if (pendingCount >= MAX_REQUESTS_PER_PLAYER) {
            return null; // Too many pending requests
        }
        
        MoneyRequest request = new MoneyRequest(requesterUUID, targetUUID, amount, message);
        requests.add(request);
        return request;
    }

    /**
     * Accept a money request
     */
    public boolean acceptRequest(UUID requestId, UUID acceptingPlayerUUID) {
        MoneyRequest request = findRequest(requestId);
        if (request == null || request.getStatus() != RequestStatus.PENDING) {
            return false;
        }
        
        // Verify the accepting player is the target
        if (!request.getTargetUUID().equals(acceptingPlayerUUID)) {
            return false;
        }
        
        request.setStatus(RequestStatus.ACCEPTED);
        return true;
    }

    /**
     * Deny a money request
     */
    public boolean denyRequest(UUID requestId, UUID denyingPlayerUUID) {
        MoneyRequest request = findRequest(requestId);
        if (request == null || request.getStatus() != RequestStatus.PENDING) {
            return false;
        }
        
        // Verify the denying player is the target
        if (!request.getTargetUUID().equals(denyingPlayerUUID)) {
            return false;
        }
        
        request.setStatus(RequestStatus.DENIED);
        return true;
    }

    /**
     * Cancel a money request
     */
    public boolean cancelRequest(UUID requestId, UUID cancellingPlayerUUID) {
        MoneyRequest request = findRequest(requestId);
        if (request == null || request.getStatus() != RequestStatus.PENDING) {
            return false;
        }
        
        // Verify the cancelling player is the requester
        if (!request.getRequesterUUID().equals(cancellingPlayerUUID)) {
            return false;
        }
        
        request.setStatus(RequestStatus.CANCELLED);
        return true;
    }

    /**
     * Find a request by ID
     */
    public MoneyRequest findRequest(UUID requestId) {
        for (MoneyRequest r : requests) {
            if (r.getRequestId().equals(requestId)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Get all pending incoming requests for a player
     */
    public List<MoneyRequest> getPendingIncomingRequests(UUID playerUUID) {
        List<MoneyRequest> result = new ArrayList<>();
        for (MoneyRequest r : requests) {
            if (r.isIncoming(playerUUID) && r.getStatus() == RequestStatus.PENDING && !r.isExpired()) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get all pending outgoing requests from a player
     */
    public List<MoneyRequest> getPendingRequestsByRequester(UUID playerUUID) {
        List<MoneyRequest> result = new ArrayList<>();
        for (MoneyRequest r : requests) {
            if (r.isOutgoing(playerUUID) && r.getStatus() == RequestStatus.PENDING && !r.isExpired()) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get all requests involving a player (incoming or outgoing)
     */
    public List<MoneyRequest> getAllRequestsForPlayer(UUID playerUUID) {
        List<MoneyRequest> result = new ArrayList<>();
        for (MoneyRequest r : requests) {
            if (r.isIncoming(playerUUID) || r.isOutgoing(playerUUID)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Clean up expired requests and enforce hard cap
     */
    public void cleanupExpiredRequests() {
        for (MoneyRequest r : requests) {
            if (r.isExpired()) {
                r.setStatus(RequestStatus.EXPIRED);
            }
        }
        
        // Remove old completed/expired requests (keep last 7 days instead of 30)
        long cutoffTime = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        requests.removeIf(r -> 
            r.getStatus() != RequestStatus.PENDING && 
            r.getTimestamp() < cutoffTime
        );

        // Enforce hard cap ÔÇö remove oldest non-pending requests if over limit
        if (requests.size() > MAX_TOTAL_REQUESTS) {
            requests.sort(java.util.Comparator.comparingLong(MoneyRequest::getTimestamp));
            java.util.Iterator<MoneyRequest> it = requests.iterator();
            while (it.hasNext() && requests.size() > MAX_TOTAL_REQUESTS) {
                MoneyRequest r = it.next();
                if (r.getStatus() != RequestStatus.PENDING) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Get count of pending incoming requests for a player
     */
    public int getPendingIncomingCount(UUID playerUUID) {
        return getPendingIncomingRequests(playerUUID).size();
    }
}
