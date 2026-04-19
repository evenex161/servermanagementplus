package com.servermanagement.features.economy;

/**
 * Status of a money request
 */
public enum RequestStatus {
    PENDING("Pending", "§e"),
    ACCEPTED("Accepted", "§a"),
    DENIED("Denied", "§c"),
    EXPIRED("Expired", "§7"),
    CANCELLED("Cancelled", "§7");

    private final String displayName;
    private final String colorCode;

    RequestStatus(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }
}
