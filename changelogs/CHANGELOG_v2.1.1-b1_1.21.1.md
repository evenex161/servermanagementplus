# ServerManagement+ v2.1.1-b1 (Minecraft 1.21.1)

## Architecture & Up-Porting
- **Up-ported from 1.20.1 to 1.21.1:** Successfully up-ported all architectural changes, UI overhauls, and bug fixes from the `mc/1.20.1` branch to `mc/1.21.1`.
- **MultiLoader Parity:** Verified full compatibility and compilation across all three supported 1.21.1 loaders: **Fabric**, **Forge**, and **NeoForge**.
- **NeoForge Compatibility:** Replaced legacy `net.minecraftforge.fml` hooks with the new `net.neoforged.fml` equivalents in NeoForge specific implementations.

## Networking
- **CustomPacketPayload Migration:** Completely migrated the legacy packet system to the new 1.21.1 `CustomPacketPayload` and `StreamCodec` architecture for `CheckForUpdatesPacket`, `StartServerUpdatePacket`, and `SyncUpdateInfoPacket`.
- **Handshake Security:** Ported the cross-loader handshake parity checks, enforcing vanilla client rejection and verifying `SyncSessionTokenPacket.ID` on connection to prevent version mismatch crashes.

## GUI & Visuals
- **1.21.1 GUI Adaptation:** Ported the new `UpdaterScreen`, `OTAUpdateScreen`, and `UpdateAvailableScreen` to 1.21.1, adapting the `renderBackground` signature changes (`renderBackground(GuiGraphics, int, int, float)`).
- **Responsive Layout:** Brought over responsive vertical stacking and dynamic text width constraints for the updater screens, ensuring they don't break on narrow window sizes.
- **Dynamic Loading Animations:** Ported the `containerTick()` loading animations (`Checking for updates...`) for network callbacks.
- **GLSL Shaders:** Included the GLSL 1.50 (`#version 150`) blur shaders for modern GPU compatibility, ported from the 1.20.1 branch.

## Build System
- **Mod Menu Registration:** Registered `UPDATER_MENU` consistently across all loaders. Adapted NeoForge menu registration to use `IMenuTypeExtension`.
- **Standardization:** Renamed the mod to **ServerManagement+** and aligned author names and configuration keys globally.
