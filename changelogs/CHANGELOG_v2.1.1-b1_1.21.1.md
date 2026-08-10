# ServerManagement+ v2.1.1-b1 (Minecraft 1.21.1)

## Architecture & Up-Porting
- **Up-ported from 1.20.1 to 1.21.1:** Successfully up-ported all architectural changes, UI overhauls, and bug fixes from the `mc/1.20.1` branch to `mc/1.21.1`.
- **MultiLoader Parity:** Verified full compatibility and compilation across all three supported 1.21.1 loaders: **Fabric**, **Forge**, and **NeoForge**.
- **NeoForge Compatibility:** Replaced legacy `net.minecraftforge.fml` hooks with the new `net.neoforged.fml` equivalents in NeoForge specific implementations.

## Networking
- **CustomPacketPayload Migration:** Completely migrated the legacy packet system to the new 1.21.1 `CustomPacketPayload` and `StreamCodec` architecture for `CheckForUpdatesPacket`, `StartServerUpdatePacket`, and `SyncUpdateInfoPacket`. Registered these packets in NeoForge `ModNetworking.java` to fix `UnsupportedOperationException: Payload ... may not be sent to the server!` disconnects when opening `UpdaterScreen`.
- **Handshake Security:** Ported the cross-loader handshake parity checks, enforcing vanilla client rejection and verifying `SyncSessionTokenPacket.ID` on connection to prevent version mismatch crashes.

## GUI & Visuals
- **1.21.1 GUI Adaptation:** Ported the new `UpdaterScreen`, `OTAUpdateScreen`, and `UpdateAvailableScreen` to 1.21.1, adapting the `renderBackground` signature changes (`renderBackground(GuiGraphics, int, int, float)`).
- **SM Updater Full Integration:**
  - Added `UpdaterMenuProvider` across Forge, Fabric, and NeoForge modules.
  - Added `GuiType.UPDATER` to `OpenGuiPacket` to handle opening the updater menu remotely.
  - Integrated the `Server Updater` card into Row 3 of the Admin Dashboard UI (`DashboardScreen`).
  - Added `/sm update` command (opens updater GUI) and `/sm update skip` subcommand (skips currently pending update version).
  - Added client-side `TitleScreen` interceptor to silently query Modrinth/CurseForge APIs and prompt the player with `UpdateAvailableScreen` upon game startup.
  - Wired `ServerUpdateScheduler.start(loader, version)` into server startup across all loaders for automatic background update checking.
  - Cleaned up obsolete `ModFileTransferManager.initialize()` calls from server startup to eliminate warning logs.
- **Responsive Layout:** Brought over responsive vertical stacking and dynamic text width constraints for the updater screens, ensuring they don't break on narrow window sizes.
- **Dynamic Loading Animations:** Ported the `containerTick()` loading animations (`Checking for updates...`) for network callbacks.
- **GLSL Shaders:** Included the GLSL 1.50 (`#version 150`) blur shaders for modern GPU compatibility, ported from the 1.20.1 branch.

## Build System
- **Mod Menu Registration:** Registered `UPDATER_MENU` consistently across all loaders. Fixed `Network Protocol Error` disconnects when opening container menus by switching standard 2-argument menus from `IMenuTypeExtension` / `IForgeMenuType` (which expect extended payload buffers) to vanilla `MenuType<>(..., FeatureFlags.VANILLA_SET)` on Forge and NeoForge.
- **Standardization:** Renamed the mod to **ServerManagement+** and aligned author names and configuration keys globally.

## Performance Orchestration & Synergy Engine
- **Phase 1 (Foundation & Architecture):** Added the `zstd-jni` library and implemented `ZstdPacketCompressor` utilizing raw Netty ByteBuffers for zero-copy memory efficiency. Injected compression encoders/decoders directly into the base `Connection` pipeline via `ConnectionMixin`. Built `EnvironmentManager` for precise context detection.
- **Phase 2 (Server-Side Core):** Added `ChunkPreGenerator` for async pre-generation via a spiral matrix algorithm. Added `MSPTMonitor` to throttle operations if MSPT exceeds 45ms. Built `DynamicWorkloadScaler` to dynamically reduce `SimulationDistance` during lag spikes. Added `ChunkUnloadDelayManager` for 10s lazy unloads.
- **Phase 3 (Client-Side Engine):** Created `ClientChunkCache` (retains up to 10k unloaded chunks). Implemented `FakeChunkInjector` to feed cached chunks back to the renderer. Built `SmartPacketQueue` to prioritize packets in the player's FOV. Offloaded visibility raycasts to `AsyncOcclusionCuller`.
- **Phase 4 (Distant Horizons Integration):** Integrated `DistantHorizonsHook` via soft-dependency reflection. Added `TieredRenderingPipeline` to push DH minimum render distance outwards and eliminate Z-fighting.

## Economy Engine
- **Crafting Cycle Feedback Loop Breaker:** Resolved exponential price divergence for modded items in `RecipeBasedPricing` caused by unanchored reversible crafting recipes (e.g. BetterNether blocks/items). Added an `increaseCounts` limit (max 15 upward adjustments) and a hard cap ceiling (`MAX_RECIPE_PRICE = 1,000,000.0`) to automatically lock cyclic feedback loops to `DEFAULT_PRICE` ($1.00) and prevent game-breaking market prices. Automatically corrects existing prices on server startup.
