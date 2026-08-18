package com.servermanagement.updater;

public record UpdateCheckResult(UpdateInfo modrinth, UpdateInfo curseforge, String currentVersion) {
    
    /**
     * Resolves the best update to use based on the user's preferences.
     * 
     * @param mainSource The preferred source ("ask", "modrinth", or "curseforge")
     * @param checkFallback Whether to fallback to the other source if it has a newer version
     * @return The chosen UpdateInfo, or null if no valid update is found.
     */
    public UpdateInfo resolve(String mainSource, boolean checkFallback) {
        UpdateInfo target = null;
        if ("modrinth".equalsIgnoreCase(mainSource)) {
            if (modrinth != null && curseforge != null && checkFallback) {
                target = UpdateManager.isNewerVersion(curseforge.version(), modrinth.version()) ? curseforge : modrinth;
            } else if (modrinth == null && curseforge != null && checkFallback) {
                target = curseforge;
            } else {
                target = modrinth;
            }
        } else if ("curseforge".equalsIgnoreCase(mainSource)) {
            if (curseforge != null && modrinth != null && checkFallback) {
                target = UpdateManager.isNewerVersion(modrinth.version(), curseforge.version()) ? modrinth : curseforge;
            } else if (curseforge == null && modrinth != null && checkFallback) {
                target = modrinth;
            } else {
                target = curseforge;
            }
        } else {
            // "ask" or fallback - return the absolute newest available
            if (modrinth != null && curseforge != null) {
                target = UpdateManager.isNewerVersion(curseforge.version(), modrinth.version()) ? curseforge : modrinth;
            } else {
                target = modrinth != null ? modrinth : curseforge;
            }
        }
        
        if (target != null && UpdateManager.isNewerVersion(target.version(), currentVersion)) {
            return target;
        }
        return null;
    }
}
