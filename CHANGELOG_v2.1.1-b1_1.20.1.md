# Changelog — ServerManagement+ v2.1.1-b1 (Minecraft 1.20.1)

**Release Date:** Jul 16, 2026
**Latest Changes:** Jul 16, 2026
**Minecraft:** 1.20.1 | **Forge:** 47.4.0 | **Fabric:** 0.92.8+1.20.1 (loader 0.19.2) | **Branch:** `mc/1.20.1`

---

## Overview

v2.1.1-b1 introduces a completely overhauled, bifurcated updater architecture. It brings independent dual-query support for Modrinth and CurseForge, an intelligent server-side smart start script generator, and strips out the obsolete legacy OTA networking framework to provide a cleaner, more robust update experience.

---

## Key Features & Improvements

### Updater Architecture Overhaul
- **⚠️ Important Notice**: This specific update (v2.1.1-b1) must be downloaded and installed manually on both the server and the client one last time. Once this version is installed, the new automatic updater will take over seamlessly for all future releases.
- **Dual-Query Support**: The updater now queries both Modrinth and CurseForge independently for updates.
- **Standalone Handoff**: Extracts and runs a standalone `updater.jar` that can safely overwrite the mod file while the server is offline.
- **Graceful Shutdown**: The server gracefully shuts down prior to applying updates, ensuring world data is safely saved and standard ports are freed.
- **Smart Build Number Comparison**: The updater's version comparison logic now accurately handles build numbers (e.g., `2.1.0-b1` vs `2.1.0-b2`) for more granular patch detection.
- **Update Check Logic Fix**: Updated the hardcoded base version string across the updater logic (e.g., in `CheckForUpdatesPacket` and `ServerUpdateScheduler`) from `"2.0.0"` to `"2.1.1-b01"`, fixing false-positive update notifications when admins manually clicked "Check for Updates" or upon joining the server.

### Smart Start Integration
- **Script Generator**: A new `StartScriptGenerator` scans the server root for existing launch scripts (`.bat` / `.sh`) and intelligently injects the `-Dservermanagement.smartstart=true` flag.
- **Auto-Override & Fallbacks**: Admins can now opt to automatically override their existing run scripts (safely backing up the originals as `.bak`). If no override is chosen, standalone `smart_start.bat` / `.sh` fallbacks are generated.
- **Self-Cleaning System (`cleanupIfNeeded`)**: The generator was optimized to cleanly bypass startup generation if manual scripts are present. If the mod detects that the smart-start flag is successfully integrated into the main `run.bat` / `run.sh`, it will automatically clean up the redundant `smart_start` fallback files from the server root.
- **Flag Management**: `update_in_progress.flag` and `update_finished.flag` are dynamically created to handle the update loop and are aggressively cleaned up during server startup to prevent false update loops after manual overrides.

### Legacy OTA Cleanup
- **Removed Obsolete Architecture**: The `com.servermanagement.ota` packages (including `PlayerJoinListener`, `CurseForgeUpdateChecker`, `OTAVersion`) have been completely deleted from both Forge and Fabric.
- **Network De-clutter**: Deleted the `ModFileTransferManager` and its corresponding packets (`VersionCheckPacket`, `ModFileRequestPacket`, `ModFileChunkPacket`, `ModFileCompletePacket`).
- **Config Cleanup**: The server automatically deletes any leftover `curseforge.properties` file from previous installations to prevent console warnings on startup.
- **Log Polish**: Replaced the global `Constants.LOG` with a dedicated `ServerManagementUpdater` logger so updater checks properly log as `[ServerManagementUpdater/INFO]`.

### General Tweaks & Bug Fixes
- **Handshake Parity Fix**: 
  - Enforced strict client-side mod requirement during network handshakes (Fabric). Vanilla clients or clients missing the mod are now gracefully rejected with a helpful disconnect message (`§cThis server requires the ServerManagement+ mod to be installed on your client!`), preventing server-side errors and breaking functionality.
  - Fixed a subsequent issue where valid Fabric clients were also being rejected by switching the handshake validation from the Forge-specific `main` channel to the cross-loader `SyncSessionTokenPacket.ID`.
- **Free Reward Settings Fix**: Fixed an issue where the Free Reward Tab in `EconomyManagementScreen` would always prompt for "Unsaved Changes" and fail to display or persist the configured `ItemStack` properly. The missing `freeRewardItem` is now correctly synced to the client via `SyncEconomyTemplatesPacket` and initialized in the widget.
- **Updater GUI Polish & Fixes**: 
  - Fixed `UpdaterScreen` and `PerformanceSettingsScreen` going off-screen upon window resizing. This was caused by overriding `Screen.rebuildWidgets()` in 1.20.1 without a super call, which bypassed the standard GUI initialization and scaling update matrix. Now properly uses `refreshWidgets()`.
  - Fixed update screen layouts (`UpdateAvailableScreen` and `OTAUpdateScreen`) going off-screen on narrow window sizes by implementing responsive vertical stacking and dynamic text width constraints.
  - Added a visual `Checking for updates...` loading animation in `UpdaterScreen` utilizing the new container tick loop, and ensured the GUI automatically sends a check packet when opened empty.
- **Background Blur Shader Fix**: Resolved custom GUI background blur shader black rendering issues on modern GPU drivers by pivoting to GLSL 1.50 (`#version 150`) under the `servermanagement` namespace.
- **Network Architecture Refactor (Fabric)**: Rewrote Fabric networking for 1.20.1 using the legacy `ServerPlayNetworking` / `ClientPlayNetworking` API, establishing uniform `IPacket` registries to mirror the Forge implementation.
- **Build & Artifact Fixes**:
  - Cleaned up Gradle cache file locks and eliminated duplicate project compilation definitions in `settings.gradle`.
  - Mod version output double 'b' (`2.1.1-bb01`) in JAR outputs and logs has been normalized against `multiloader-common.gradle`.
  - Fixed broken em-dash formatting (`â€”`) on the Forge mods screen by stripping special characters in `gradle.properties`.
- **Repository & Documentation Cleanup**:
  - Removed obsolete `build-full.txt` output log from the repository and explicitly untracked `description.md` from version control.
  - Added a comprehensive "How the Updater Works" section to `README.md` and `description.md` for full transparency with manual reviewers.
  - Provided explicit instructions for code reviewers on how to safely simulate an outdated mod and test the updater architecture using a cloned workspace.
- **Standardized Mod Name**: Adjusted the mod name to `ServerManagement+` and aligned the author name across branches.
- **Chat Formatting Fix**: The `ServerManagement Dashboard` welcome notification in chat no longer wraps text aggressively on standard 320px chat widths (formatting dividers reduced from 39 to 30 characters).