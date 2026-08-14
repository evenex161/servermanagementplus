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

## Economy & Market
- **Drop Rate Tracker & Virtual Recipes:** Built `DropRateTracker` to capture dynamic block drop multipliers in real time by sampling `Block.getDrops()` on block breaks. Injected these drop rates into `RecipeBasedPricing` as bi-directional virtual `IRecipeEntry` recipes, seamlessly bridging raw ore prices to ingot prices without hardcoded anchors.
- **Reverse Crafting Economy:** Added automatic reverse crafting deduction to `RecipeBasedPricing.collectRecipes` for all standard mono-ingredient crafting recipes (e.g. implicitly deducing `iron_ingot` backwards from `iron_block`).
- **Dynamic Capital Scaling:** Overhauled `RecipeBasedPricing` to scale all anchor and dynamic fallback item prices dynamically based on the server's `STARTING_BALANCE` multiplier (capital-adjusted inflation).
- **Fallback Pricing Upgrades:** Upgraded the fallback pricing algorithm to calculate accurate valuations for uncraftable modded items using their 1.21.1 DataComponent `RARITY` (Uncommon, Rare, Epic) and unstackable properties.
- **Economy Configurations:** Exposed the `STARTING_BALANCE` configuration in the `EconomyManagementScreen` Settings tab with dynamic network syncing on all platforms.
- **Blacklisted Item Notices:** Updated `ClientItemTooltipHandler` to display a custom "Item is on the Trading Blacklist" notice instead of calculated market values for explicitly banned items.
- **Market Parity Fixes:** Fixed an infinite crafting cycle bug in `RecipeBasedPricing` where unanchored items (like `raw_gold`) crashed prices; added explicit anchors to raw ores and patched the cycle lock to fallback to anchors. Synchronized `ClientMarketData` fallback values with the server's `capitalMultiplier` logic to fix client UI parity.
- **MineBay Enhancements:** Restructured the MineBay search box for better UX. Moved it to the left side (aligned with "All Listings") and expanded its width. Fixed a severe focus bug where the typing indicator vanished and focus was lost on every keystroke by removing the UI rebuild hook from the text responder. Search now works seamlessly across both "All Listings" and "My Listings" tabs.
- **Trade Blacklist Editor:** Fixed the nested search bar within the `TradeBlacklistEditorWidget` not receiving focus or keyboard events by actively hooking and forwarding `mouseClicked`, `charTyped`, `keyPressed`, and `setFocused()` events from the parent screen to the child `EditBox`.
- **Warning Persistence:** Migrated the "Don't ask me again" confirmation setting for the trade blacklist deletion warning to a dedicated per-server `ClientConfig` (`servermanagement_client.json`). It now maps server IPs to booleans to ensure safety across different multiplayer servers.
- **Enforced Module Toggles:** Commands `/minebay` and `/minestacks` (or `/casino`) now strictly query both the global economy feature state and their respective granular `ModConfig` toggles (`MINEBAY_ENABLED`, `MINESTACKS_ENABLED`). Disabled modules will correctly block GUI access and return a failure message to the player.
- **Crafting Cycle Feedback Loop Breaker:** Resolved exponential price divergence for modded items in `RecipeBasedPricing` caused by unanchored reversible crafting recipes (e.g. BetterNether blocks/items). Added an `increaseCounts` limit (max 15 upward adjustments) and a hard cap ceiling (`MAX_RECIPE_PRICE = 1,000,000.0`) to automatically lock cyclic feedback loops to `DEFAULT_PRICE` ($1.00) and prevent game-breaking market prices. Automatically corrects existing prices on server startup.
- **Economy & Feature Toggle Architecture Fix:** Resolved 5 critical issues in the Economy/MineBay/MineStacks disable toggle system across Forge, Fabric, and NeoForge:
  - Added missing `ECONOMY_ENABLED` configuration field to `ModConfig` and updated `FeatureManager` to load/persist the `economy` toggle to disk.
  - Enforced `MINEBAY_ENABLED` and `MINESTACKS_ENABLED` config checks in `OpenGuiPacket` to prevent bypassing feature disables via `BankScreen` buttons.
  - Separated `isFeatureEnabled("economy")` and `MINEBAY_ENABLED`/`MINESTACKS_ENABLED` checks in `MineBayCommand` and `MineStacksCommand` with clear, distinct feedback messages ("MineBay is disabled because Economy is disabled" vs "MineBay has been disabled by an administrator").
  - Added automatic server-to-client `SyncEconomySettingsPacket` dispatch on player join (`SessionEventHandler`) to ensure client UI controls in `EconomyManagementScreen` accurately match server configuration states.

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

## Economy & Market
- **Drop Rate Tracker & Virtual Recipes:** Built `DropRateTracker` to capture dynamic block drop multipliers in real time by sampling `Block.getDrops()` on block breaks. Injected these drop rates into `RecipeBasedPricing` as bi-directional virtual `IRecipeEntry` recipes, seamlessly bridging raw ore prices to ingot prices without hardcoded anchors.
- **Reverse Crafting Economy:** Added automatic reverse crafting deduction to `RecipeBasedPricing.collectRecipes` for all standard mono-ingredient crafting recipes (e.g. implicitly deducing `iron_ingot` backwards from `iron_block`).
- **Dynamic Capital Scaling:** Overhauled `RecipeBasedPricing` to scale all anchor and dynamic fallback item prices dynamically based on the server's `STARTING_BALANCE` multiplier (capital-adjusted inflation).
- **Fallback Pricing Upgrades:** Upgraded the fallback pricing algorithm to calculate accurate valuations for uncraftable modded items using their 1.21.1 DataComponent `RARITY` (Uncommon, Rare, Epic) and unstackable properties.
- **Economy Configurations:** Exposed the `STARTING_BALANCE` configuration in the `EconomyManagementScreen` Settings tab with dynamic network syncing on all platforms.
- **Blacklisted Item Notices:** Updated `ClientItemTooltipHandler` to display a custom "Item is on the Trading Blacklist" notice instead of calculated market values for explicitly banned items.
- **Market Parity Fixes:** Fixed an infinite crafting cycle bug in `RecipeBasedPricing` where unanchored items (like `raw_gold`) crashed prices; added explicit anchors to raw ores and patched the cycle lock to fallback to anchors. Synchronized `ClientMarketData` fallback values with the server's `capitalMultiplier` logic to fix client UI parity.
- **MineBay Enhancements:** Restructured the MineBay search box for better UX. Moved it to the left side (aligned with "All Listings") and expanded its width. Fixed a severe focus bug where the typing indicator vanished and focus was lost on every keystroke by removing the UI rebuild hook from the text responder. Search now works seamlessly across both "All Listings" and "My Listings" tabs.
- **Trade Blacklist Editor:** Fixed the nested search bar within the `TradeBlacklistEditorWidget` not receiving focus or keyboard events by actively hooking and forwarding `mouseClicked`, `charTyped`, `keyPressed`, and `setFocused()` events from the parent screen to the child `EditBox`.
- **Warning Persistence:** Migrated the "Don't ask me again" confirmation setting for the trade blacklist deletion warning to a dedicated per-server `ClientConfig` (`servermanagement_client.json`). It now maps server IPs to booleans to ensure safety across different multiplayer servers.
- **Enforced Module Toggles:** Commands `/minebay` and `/minestacks` (or `/casino`) now strictly query both the global economy feature state and their respective granular `ModConfig` toggles (`MINEBAY_ENABLED`, `MINESTACKS_ENABLED`). Disabled modules will correctly block GUI access and return a failure message to the player.
- **Crafting Cycle Feedback Loop Breaker:** Resolved exponential price divergence for modded items in `RecipeBasedPricing` caused by unanchored reversible crafting recipes (e.g. BetterNether blocks/items). Added an `increaseCounts` limit (max 15 upward adjustments) and a hard cap ceiling (`MAX_RECIPE_PRICE = 1,000,000.0`) to automatically lock cyclic feedback loops to `DEFAULT_PRICE` ($1.00) and prevent game-breaking market prices. Automatically corrects existing prices on server startup.
- **Economy & Feature Toggle Architecture Fix:** Resolved 5 critical issues in the Economy/MineBay/MineStacks disable toggle system across Forge, Fabric, and NeoForge:
  - Added missing `ECONOMY_ENABLED` configuration field to `ModConfig` and updated `FeatureManager` to load/persist the `economy` toggle to disk.
  - Enforced `MINEBAY_ENABLED` and `MINESTACKS_ENABLED` config checks in `OpenGuiPacket` to prevent bypassing feature disables via `BankScreen` buttons.
  - Separated `isFeatureEnabled("economy")` and `MINEBAY_ENABLED`/`MINESTACKS_ENABLED` checks in `MineBayCommand` and `MineStacksCommand` with clear, distinct feedback messages ("MineBay is disabled because Economy is disabled" vs "MineBay has been disabled by an administrator").
  - Added automatic server-to-client `SyncEconomySettingsPacket` dispatch on player join (`SessionEventHandler`) to ensure client UI controls in `EconomyManagementScreen` accurately match server configuration states.

- **MineBay & Economy UI Keybind Fixes:** Fixed a critical bug where pressing the "E" key while typing in search boxes or price fields inside MineBayScreen and EconomyManagementScreen would bypass the text field and trigger the default Minecraft inventory keybind, instantly closing the UI. This was resolved by explicitly intercepting keyPressed and charTyped events to prioritize the focused EditBox.
- **Trade Blacklist Visual Polish & Enforcement:** Added missing 	ick() method bindings in TradeBlacklistEditorWidget to successfully trigger and render the red progress bar animation when holding right-click to delete items. Fixed list-item focus handling to ensure multi-select (CTRL+Click) correctly updates. Additionally, strictly enforced the Trade Blacklist in MineBayScreen, preventing blacklisted items from proceeding past Step 1 with an "Item Blacklisted" red warning button.

- **MineBay Visual Polish:** Fixed search filtering visual bugs across all loaders where UI elements (like "Details" buttons) from previous listings would persist on screen after a search update. Implemented explicit dynamic widget tracking and a `refreshBrowseWidgets()` lifecycle, allowing search results to seamlessly regenerate action buttons without triggering a full screen rebuild (which would destroy typing focus). Also added a proper "No matches found" empty state for zero-result searches.
- **Trade Blacklist Focus Bouncing:** Fixed `TradeBlacklistEditorWidget` search box failing to gain focus when clicked. This was caused by the parent `Screen`'s focus bouncing logic (`setFocused(false)` then `setFocused(true)` on the widget) aggressively clearing the `EditBox`'s focus without restoring it. Solved by introducing an `isPendingSearchBoxFocus` flag to safely survive the screen focus bounce.
- **Trade Blacklist Visual Overlap:** Fixed `TradeBlacklistEditorWidget` autocomplete search results rendering underneath the 3D models of blacklisted items. Minecraft's item rendering writes to the depth buffer, causing 3D items to clip through the 2D autocomplete background. Resolved by pushing the `PoseStack` and applying a Z-axis translation (`translate(0, 0, 400)`) specifically for the autocomplete overlay to render it strictly above all depth-buffered items.
- **Global Census Engine:** Replaced the event-based economy tracker (`onItemCrafted`, `onItemSmelted`, `onItemPickup`) with a new tick-based **Global Census Engine** in `ItemSupplyDemandTracker` across all loaders (Forge, Fabric, NeoForge).
- **Differential Economy Caching:** Implemented continuous background chunk scanning and player inventory scanning using differential cache maps (`playerCache`, `chunkCache`) to accurately deduce item existence rather than intercepting item events. This fundamentally solves economy duplication glitches involving creative mode items, un-tracked item drops, or command-spawned items influencing the market.
- **Creative Mode Market Protection:** Added `CreativeItemImportMixin` and `ItemInputMixin` to explicitly tag any items spawned via the Creative Inventory or the `/give` command with a `servermanagement_creative` NBT tag. The Global Census Engine ignores these tagged items, preventing server admins or creative players from artificially crashing the economy by importing items out of thin air.
- **Offline Player Census Scanning:** Upgraded the Global Census Engine to automatically scan offline player `.dat` files in the world's `playerdata` directory during server startup. This ensures the total economy item supply accurately reflects items held by players who are not currently online, preventing artificial hyper-inflation.
- **Rare Item Economy Anchors:** Added rare uncraftable vanilla items (Dragon Head, Dragon's Breath, Heart of the Sea, Sponge, Trident, etc.) to the `ItemValuation` anchor map, ensuring these edge-case items have proper baseline prices that the census engine can dynamically scale based on scarcity.
- **Dynamic Scarcity Scaling:** Overhauled economy dynamic fallback pricing. Integrated the Global Census Engine into RecipeBasedPricing and ClientMarketData across all loaders to assign massive scarcity multipliers (100,000 / (supply + 1)) to uncraftable/un-anchored items (like Dragon Heads), perfectly balancing modded novelty items.

## Commands & Routing (2026-08-14)
- **`/dailies claim` Registration:** Fixed missing `/dailies claim` sub-command across all three loaders (Forge, NeoForge, Fabric). The standalone `/dailies` command previously only opened the GUI; it now also supports a `claim` sub-command to claim free daily rewards via chat.
- **`/sm hud edit` Registration:** Fixed missing `/sm hud edit` sub-command across all three loaders. Added the `hud` → `edit` sub-tree to the `/sm` Brigadier command tree, routing to `OpenGuiPacket(GuiType.HUD_EDIT)`.
- **OpenGuiPacket HUD_EDIT Separation:** Fixed `OpenGuiPacket` handler where the `HUD_EDIT` case erroneously fell through to `UPDATER`, opening the wrong screen. Split into separate `case` blocks across all loaders. `HUD_EDIT` now sends a server-to-client packet back to the player (since `HudEditScreen` is client-side-only), and the client handles it by calling `Minecraft.getInstance().setScreen(new HudEditScreen())`.
- **OpenGuiPacket S2C Registration:** Registered `OpenGuiPacket` as a bidirectional (server-to-client) payload in Fabric (`PayloadTypeRegistry.playS2C()` + `ClientPlayNetworking.registerGlobalReceiver()`) and NeoForge (`registrar.playToClient()`) to enable the HUD_EDIT server→client flow.
- **HudEditScreen Stub:** Created `HudEditScreen.java` as a client-side-only stub screen across all three loaders, displaying a centered "HUD Editor - Coming Soon" placeholder message.

## GUI Widgets & Visual Overhaul (2026-08-14)
- **DropdownWidget (NEW):** Created a reusable `DropdownWidget` component (`gui/widgets/DropdownWidget.java`) across all loaders. Features click-to-expand overlay with hover highlighting, z-translated rendering (`translate(0, 0, 400)`) to render above 3D items, keyboard navigation (Arrow keys, Enter, Escape), gold checkmark for selected item, text truncation with ellipsis, and selection callback via `Consumer<Integer>`.
- **NodeBasedTemplateEditorWidget (NEW):** Implemented a TIA Portal-inspired visual node-based editor (`gui/widgets/NodeBasedTemplateEditorWidget.java`) for daily task templates across all loaders. Features:
  - Three connected node cards: **Task Type** (with embedded `DropdownWidget`), **Task Details** (description + target amount `EditBox`es), and **Rewards** (cash reward `EditBox`).
  - Interactive circular connector pins on node edges with hover glow pulse animations driven by a `tickCount` sine wave.
  - Cubic bezier connection lines between nodes, rendered via 20-segment fill approximation.
  - Dot grid background for the editor canvas.
  - Drag-to-connect interaction from output pins to input pins.
  - Full input routing: Tab key cycling between fields, proper focus management across nested `EditBox`es and `DropdownWidget`, dropdown z-ordering above all other elements.
- **Economy "Create New Template" Overhaul:** Replaced the old flat form in `EconomyManagementScreen.initEditMode()` (which used ugly `◄ Type` / `Type ►` cycling buttons, plain `EditBox`es, and a static layout) with the new `NodeBasedTemplateEditorWidget` across all three loaders. The `saveTemplate()` method now reads directly from the node editor's getters (`getSelectedTaskType()`, `getDescription()`, `getGoal()`, `getRewardAmount()`).

### Added
- Created isolated ClientGuiOpener class to prevent Dedicated Server class verification crashes.

### Fixed
- Fixed a Dedicated Server crash on startup across all loaders caused by OpenGuiPacket attempting to classload 
et.minecraft.client.Minecraft during JVM verification.
- Fixed a NeoForge-specific client crash during startup (Cannot register payload... as it is already registered) by switching OpenGuiPacket dual registration to playBidirectional.

### Security
- Implemented Anti-Tamper DRM for CurseForge API Key extraction. The API key is now dynamically encrypted at compile-time and the AES-128 decryption key is derived exclusively from the SHA-256 hash of the CurseForgeUpdateChecker.class bytecode at runtime. If the compiled mod is decompiled and maliciously altered in any way, the hash will change, permanently destroying the embedded CurseForge telemetry API key and safely falling back to user-provided configuration.
