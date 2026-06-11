package com.servermanagement.client;

public class ClientSessionRegistry {
    private static String sessionToken = "";

    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    public static String getSessionToken() {
        return sessionToken != null ? sessionToken : "";
    }
}
