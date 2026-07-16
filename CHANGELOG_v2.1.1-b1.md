# Changelog — ServerManagement+ v2.1.1-b1 (Minecraft 1.20.1)

**Release Date:** Unreleased (Dev)
**Latest Changes:** Jul 16, 2026
**Minecraft:** 1.20.1 | **Forge:** 47.4.0 | **Fabric:** 0.92.8+1.20.1 (loader 0.19.2) | **Branch:** `mc/1.20.1`

---

## Overview

v2.1.1-b1 introduces a completely overhauled, bifurcated updater architecture. It brings independent dual-query support for Modrinth and CurseForge, an intelligent server-side smart start script generator, and strips out the obsolete legacy OTA networking framework to provide a cleaner, more robust update experience.

---

## Key Features & Improvements

### Updater Architecture Overhaul
- **Dual-Query Support**: The updater now queries both Modrinth and CurseForge independently for updates.
- **Standalone Handoff**: Extracts and runs a standalone `updater.jar` that can safely overwrite the mod file while the server is offline.
- **Graceful Shutdown**: The server gracefully shuts down prior to applying updates, ensuring world data is safely saved and standard ports are freed.
- **Smart Build Number Comparison**: The updater's version comparison logic now accurately handles build numbers (e.g., `2.1.0-b1` vs `2.1.0-b2`) for more granular patch detection.

### Smart Start Integration
- **Script Generator**: A new `StartScriptGenerator` scans the server root for existing launch scripts (`.bat` / `.sh`) and intelligently injects the `-Dservermanagement.smartstart=true` flag.
- **Auto-Override & Fallbacks**: Admins can now opt to automatically override their existing run scripts (safely backing up the originals as `.bak`). If no override is chosen, standalone `smart_start.bat` / `.sh` fallbacks are generated.
- **Self-Cleaning System**: If the mod detects that the smart-start flag is successfully integrated into the main `run.bat` / `run.sh`, it will automatically clean up the redundant `smart_start` fallback files from the server root.
- **Flag Management**: `update_in_progress.flag` and `update_finished.flag` are dynamically created to handle the update loop and are aggressively cleaned up during server startup to prevent false update loops after manual overrides.

### Legacy OTA Cleanup
- **Removed Obsolete Architecture**: The `com.servermanagement.ota` packages (including `PlayerJoinListener`, `CurseForgeUpdateChecker`, `OTAVersion`) have been completely deleted from both Forge and Fabric.
- **Network De-clutter**: Deleted the `ModFileTransferManager` and its corresponding packets (`VersionCheckPacket`, `ModFileRequestPacket`, `ModFileChunkPacket`, `ModFileCompletePacket`).
- **Config Cleanup**: The server automatically deletes any leftover `curseforge.properties` file from previous installations to prevent console warnings on startup.
- **Log Polish**: Replaced the global `Constants.LOG` with a dedicated `ServerManagementUpdater` logger so updater checks properly log as `[ServerManagementUpdater/INFO]`.

### General Tweaks
- **Standardized Mod Name**: Adjusted the mod name to `ServerManagement+` and aligned the author name across branches.
