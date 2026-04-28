package com.servermanagement.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client-side storage for money request data received from server.
 * Each entry is a simplified representation of a MoneyRequest for display.
 */
public class ClientMoneyRequestData {
    private static List<RequestEntry> incomingRequests = new ArrayList<>();
    private static List<RequestEntry> outgoingRequests = new ArrayList<>();

    public static List<RequestEntry> getIncomingRequests() {
        return new ArrayList<>(incomingRequests);
    }

    public static List<RequestEntry> getOutgoingRequests() {
        return new ArrayList<>(outgoingRequests);
    }

    public static void setIncomingRequests(List<RequestEntry> requests) {
        incomingRequests = new ArrayList<>(requests);
    }

    public static void setOutgoingRequests(List<RequestEntry> requests) {
        outgoingRequests = new ArrayList<>(requests);
    }

    public static void clear() {
        incomingRequests.clear();
        outgoingRequests.clear();
    }

    /**
     * Simplified money request entry for client-side display
     */
    public static class RequestEntry {
        private final UUID requestId;
        private final String playerName; // requester name (for incoming) or target name (for outgoing)
        private final double amount;
        private final String message;
        private final String age; // pre-formatted age string
        private final String status; // PENDING, ACCEPTED, DENIED, etc.

        public RequestEntry(UUID requestId, String playerName, double amount, String message, String age, String status) {
            this.requestId = requestId;
            this.playerName = playerName;
            this.amount = amount;
            this.message = message;
            this.age = age;
            this.status = status;
        }

        public UUID getRequestId() { return requestId; }
        public String getPlayerName() { return playerName; }
        public double getAmount() { return amount; }
        public String getMessage() { return message; }
        public String getAge() { return age; }
        public String getStatus() { return status; }
        public boolean isPending() { return "PENDING".equals(status); }
    }
}
