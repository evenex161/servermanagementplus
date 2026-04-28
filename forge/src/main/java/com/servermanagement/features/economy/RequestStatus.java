package com.servermanagement.features.economy;

/**
 * Status of a money request
 */
public enum RequestStatus {
    PENDING("Pending", "┬ºe"),
    ACCEPTED("Accepted", "┬ºa"),
    DENIED("Denied", "┬ºc"),
    EXPIRED("Expired", "┬º7"),
    CANCELLED("Cancelled", "┬º7");

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
