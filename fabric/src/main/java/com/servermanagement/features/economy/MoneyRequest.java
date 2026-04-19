package com.servermanagement.features.economy;

import java.util.UUID;

/**
 * Represents a money request from one player to another
 */
public class MoneyRequest {
    private final UUID requestId;
    private final UUID requesterUUID;
    private final UUID targetUUID;
    private final double amount;
    private final String message;
    private final long timestamp;
    private RequestStatus status;
    
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000; // 7 days

    public MoneyRequest(UUID requesterUUID, UUID targetUUID, double amount, String message) {
        this.requestId = UUID.randomUUID();
        this.requesterUUID = requesterUUID;
        this.targetUUID = targetUUID;
        this.amount = amount;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.status = RequestStatus.PENDING;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getRequesterUUID() {
        return requesterUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public double getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    /**
     * Check if the request has expired
     */
    public boolean isExpired() {
        if (status != RequestStatus.PENDING) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return currentTime - timestamp > EXPIRATION_TIME;
    }

    /**
     * Get time since request was made (in milliseconds)
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Get formatted age (e.g., "2 days ago", "3 hours ago")
     */
    public String getFormattedAge() {
        long ageMs = getAge();
        long days = ageMs / (24 * 60 * 60 * 1000);
        long hours = (ageMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (ageMs % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    /**
     * Check if this is an incoming request (user is target)
     */
    public boolean isIncoming(UUID playerUUID) {
        return targetUUID.equals(playerUUID);
    }

    /**
     * Check if this is an outgoing request (user is requester)
     */
    public boolean isOutgoing(UUID playerUUID) {
        return requesterUUID.equals(playerUUID);
    }
}
