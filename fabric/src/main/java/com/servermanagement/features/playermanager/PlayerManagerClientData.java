package com.servermanagement.features.playermanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side storage for player manager data received from the server.
 */
public class PlayerManagerClientData {
    private static List<String> bannedPlayers = new ArrayList<>();
    private static List<String> whitelistedPlayers = new ArrayList<>();
    private static boolean whitelistEnabled = false;

    public static void setBannedPlayers(List<String> players) {
        bannedPlayers = new ArrayList<>(players);
    }

    public static void setWhitelistedPlayers(List<String> players) {
        whitelistedPlayers = new ArrayList<>(players);
    }

    public static void setWhitelistEnabled(boolean enabled) {
        whitelistEnabled = enabled;
    }

    public static List<String> getBannedPlayers() {
        return Collections.unmodifiableList(bannedPlayers);
    }

    public static List<String> getWhitelistedPlayers() {
        return Collections.unmodifiableList(whitelistedPlayers);
    }

    public static boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }
}
