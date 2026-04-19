# Changelog — ServerManagement+ v2.1.0

**Release Date:** April 19, 2026  
**Minecraft:** 1.21.1 | **Forge:** 52.1.0 | **NeoForge:** 21.1.80 | **Fabric API:** 0.116.1+1.21.1 | **Branch:** `mc/1.21.1-forge`

---

## Overview

v2.1.0 is a major update featuring a **MultiLoader architecture migration** (Forge + NeoForge + Fabric), comprehensive **GUI polish and rework**, and critical **bug fixes** for dimension isolation features. The project structure was migrated to jaredlll08/MultiLoader-Template, with complete ports to NeoForge and Fabric. Every screen in the mod was reviewed against real in-game screenshots at GUI Scale 3.0 (854×457), and layout issues were identified and fixed. This release focuses on multi-platform support, eliminating wasted space, fixing overlapping elements, improving visual consistency, and making every panel feel polished and intentional.

---

## MultiLoader Architecture Migration

Migrated the entire project from a single-module Forge setup to the **jaredlll08/MultiLoader-Template**, enabling simultaneous builds for Forge, NeoForge, and Fabric from a single codebase.

### Project Structure
- **`buildSrc/`** — Gradle convention plugins (`multiloader-common.gradle`, `multiloader-loader.gradle`)
- **`common/`** — Platform-agnostic code (`Constants`, `Services`, `IPlatformHelper` interface)
- **`forge/`** — Full Forge implementation (254 Java files) — Forge 52.1.0, ForgeGradle [6.0.24,6.2)
- **`neoforge/`** — Complete NeoForge port (254 files) — NeoForge 21.1.80, ModDevGradle 2.0.49-beta
- **`fabric/`** — Complete Fabric port (254 files) — Fabric API 0.116.1+1.21.1, Loader 0.18.1, fabric-loom 1.9.2
- Old `src/main/` deleted (256 files relocated to `forge/` subproject)

### Build System
- Gradle 8.11 with ForgeGradle, ModDevGradle, and fabric-loom plugins managed via `buildSrc` convention plugins
- **Version catalog** (`gradle/libs.versions.toml`) for centralized dependency management
- Platform abstraction via Java `ServiceLoader` (`IPlatformHelper` interface)
- Common module uses NeoForm 1.21.1-20240808.144430 with Parchment 2024.11.10 mappings
- All three loaders build independently, producing separate JARs

### NeoForge Port
- Adapted all event handlers to NeoForge event bus (`@SubscribeEvent`)
- Converted networking from Forge `SimpleChannel` to NeoForge `CustomPacketPayload` system
- Updated registry to NeoForge `DeferredRegister` API
- Ported config to `NeoForgeConfigSpec`

### Fabric Port
- Converted all event handlers to Fabric API callbacks (`ServerPlayConnectionEvents`, `ServerTickEvents`, etc.)
- Implemented networking via `PayloadTypeRegistry` / `ServerPlayNetworking`
- Added access widener (`servermanagement.accesswidener`) for required Minecraft internals
- Adapted commands, menus, and screens to Fabric conventions

### Test Infrastructure
- Rewrote `test.bat` for MultiLoader — added loader selection parameter (`forge|neoforge|fabric`)
- Wired `-PmcUsername` and `-PmcGameDir` Gradle properties into all three `build.gradle` run configurations
- Usage: `test.bat [client|server|both|debug] [forge|neoforge|fabric]`
- Run directories: `<loader>/runs/client/` and `<loader>/runs/server/`

**Note:** NeoForge and Fabric ports compile successfully but are not yet runtime-tested. Forge remains the primary development target.

---

## Technical Improvements

### Versioning Standards
- Upgraded Gradle wrapper from 8.10 to **8.11**
- Bumped Fabric Loader from 0.16.9 to **0.18.1**
- Bumped Fabric API from 0.109.0+1.21.1 to **0.116.1+1.21.1**
- Upgraded fabric-loom from 1.8-SNAPSHOT to **1.9.2** (stable)
- Created **Gradle version catalog** (`gradle/libs.versions.toml`) centralizing all dependency versions, plugins, and library aliases across all subprojects

### Record-Based Networking
- Converted all **216 packet classes** (72 per module × 3 modules) from regular Java classes to **Java records**
- Eliminates boilerplate: explicit field declarations, canonical constructors, and getter methods replaced by record component declarations
- `FriendlyByteBuf` decode constructors now delegate to canonical constructors with `this(buf.readX(), ...)`
- Static constants (`TYPE`, `STREAM_CODEC`, `Pattern`, etc.) preserved in record bodies
- Complex decode logic extracted to static helper methods where needed
- Packet behavior (encode/handle) and inner types (enums, nested records) remain unchanged

### Virtual Threading (AsyncSaveScheduler)
- Refactored `AsyncSaveScheduler` across all 3 modules to use Java 21 **virtual threads** for save I/O operations
- Architecture: single-thread `ScheduledExecutorService` handles debounce timing only; actual disk I/O dispatched to `Executors.newVirtualThreadPerTaskExecutor()`
- Reduces platform thread overhead for concurrent save operations — virtual threads are ideal for I/O-bound tasks
- Removed unused `AtomicBoolean` import from all copies

### Tab Isolation Heartbeat
- Added **100-tick heartbeat** (5-second cycle) to `TabListIsolationHandler` across all 3 modules
- Every 100 ticks, clears cached `previousVisiblePlayers` state and forces a full resync of listed/unlisted status
- Catches edge-case drift where client tab list state diverges from server intent (e.g., missed packets, race conditions during rapid dimension changes)
- Heartbeat counter resets after each full resync to avoid redundant resync in the same tick cycle

---

## GUI Improvements

### Dashboard Screen
- Close button moved **inside** the panel boundary (was extending outside)
- Card height now dynamically calculated to reserve space for the Close button
- Close button widened from 100px → 120px for better click target

### MineBay — Step 1 (Place Item to Sell)
- Added **step progress indicators** (1, 2, 3) right-aligned on the subtitle row
- Added a **visual drop zone frame** around the offering slot area
- "Place Item" / "Next" action button widened to fill most of the panel (`min(350, imageWidth-40)`)
- Instructions and help text **centered** instead of left-aligned
- Full-width underline below step title

### MineBay — Step 2 (Set Prices)
- Price input columns now use **percentage-based widths** (45% / 20% / remainder) filling the full panel
- Price item controls widened: amount box 45→70px, mode button 60→95px, clear button 22→30px
- Added **bordered preview area** on the right side of each price item row showing item name or "← Click to pick" hint
- Full-width underline and properly spaced labels
- "Next: Confirm" button centered with full width
- Row spacing increased from 28→30px for better readability

### Performance Settings Screen
- Panel height increased from 340→420px — all 8 toggles now fit without clipping
- Panel width increased from 380→430px
- **Header height** increased from 72→78px — toggle/setting rows no longer overlap tab buttons
- Tab buttons use calculated even spacing (`tabGap=5`, `tabWidth=(total-gaps)/3`)
- Bottom buttons (Dashboard / Close) now **symmetrical** with equal widths
- **Stats page now scrollable** — added scroll offset support for the stats tab content

### World Detail Screen
- Panel enlarged from 350×230 → 380×280
- Reduced top gap (`startY` 50→38) — eliminated the empty bar below the header
- "Portal Timer" label moved from **below** the controls to **above** them
- Timer controls taller (20→22px height), portal type button wider (70→80px)
- Row spacing increased from 28→32px
- Bottom buttons (Back / Close) now symmetrical using `(imageWidth-30)/2`

### Console Screen
- Panel enlarged from 500×300 → 550×330
- Back button (←) sized up from 20×18 → 22×20
- **Title text shifted right** (15→32px) so it no longer overlaps the ← back button
- Input row controls taller (20→22px) with calculated button widths (Send 65px, Clear 55px)
- More vertical space for console output area

### Config Screen (Mod Configuration)
- Panel widened from 320×330 → 400×380
- Added **description text** below each feature name (e.g., "Manage dimensions, portals & timers")
- Added **alternating row backgrounds** for visual structure
- Added **subtle divider lines** between feature rows
- Bottom buttons now symmetrical with equal widths
- Toggles repositioned with evenly distributed spacing

### Economy Management Screen
- Template card height reduced from 90→70px, padding 8→5px — **3 cards now fit inside the panel** without overflow
- Card starting position moved up (118→112) for better alignment
- Button sizes adjusted: Edit (55px), Delete (60px), Enabled (120px) — all stay within card bounds
- Text labels repositioned to match compressed card layout

### MineStacks Screen
- Panel slightly enlarged from 400×220 → 420×230 for more breathing room

---

## Technical Details

- All screens use `ScreenScaler.scale(baseW, baseH, screenW, screenH)` for responsive sizing
- Layout calculations use relative positioning from `centerX`/`centerY` (panel top-left)
- Scroll support added to Performance Settings Stats tab via `getMaxScroll()` and scissor clipping
- Tab/Chat isolation now uses differential packet updates for efficiency (only sends changes)

---

## Bug Fixes

### /spectate Command — Vanilla Collision
- **Bug**: `/spectate Player2` returned "Player2 is not in spectator mode" instead of activating the mod's spectate feature
- **Root Cause**: Vanilla Minecraft has its own `/spectate` command that requires the executing player to already be in `GameType.SPECTATOR`. Brigadier merged both command registrations, and vanilla's handler was matched first
- **Fix**: Remove vanilla's `spectate` node from the command dispatcher via reflection on Brigadier's internal `children` and `literals` maps before registering our version

### /teleportlobby — Teleports to Wrong Location
- **Bug**: `/teleportlobby` teleported to the dimension's world spawn instead of the lobby coordinates set by `/setlobby`
- **Root Cause**: The command called `WorldManager.teleportToDimension(player, lobby.dimension)` which uses `getSharedSpawnPos()` (the dimension's spawn), completely ignoring the lobby's stored x/y/z coordinates
- **Fix**: Replaced with direct `player.teleportTo(targetLevel, lobby.x, lobby.y, lobby.z, lobby.yaw, lobby.pitch)`. Also updated `/setlobby` to store exact player position (doubles) and rotation instead of `BlockPos` (integers) with hardcoded yaw/pitch of 0

### /smconfig — Opens Outdated Screen
- **Bug**: `/smconfig` opened an old, minimal screen with only 3 toggles (World Manager, Player Manager, SlimeHead) instead of the full 6-toggle config screen
- **Root Cause**: The command opened `ServerManagementMenuProvider` → `ServerManagementScreen` (the old screen) instead of `ConfigMenuProvider` → `ConfigScreen` (the current screen with Economy, Server Performance, MOTD toggles, descriptions, and alternating row backgrounds)
- **Fix**: Changed to open `ConfigMenuProvider`

### Chat Isolation — CRITICAL FIX
- **Bug**: Chat Isolation toggle in the GUI did nothing — chat was always global regardless of setting
- **Root Cause**: The event handler checked `ModConfig.CHAT_ISOLATION_ENABLED` (static Forge config, always `true`) but never checked `WorldManagerData.isChatIsolationEnabled()` (the runtime toggle controlled by the admin GUI). Additionally, when no `chatConnections` were configured, it returned early and allowed global chat — defeating the feature entirely
- **Fix**: Handler now checks the `WorldManagerData` runtime toggle. When isolation is active, chat is restricted to same-dimension players by default. Connected dimensions still act as optional cross-dimension chat bridges

### Tab Isolation — CRITICAL FIX
- **Bug**: Tab Isolation toggle in the GUI did nothing — all players were always visible in the tab list
- **Root Cause**: Same config mismatch as chat isolation, plus the handler computed visible players but **never sent any packets** — the loop body was empty with a TODO comment. The feature was completely unimplemented
- **Fix**: Full rewrite with actual packet logic:
  - Tracks previous visibility state per player
  - Sends `ClientboundPlayerInfoRemovePacket` to hide players not in the same dimension
  - Sends `ClientboundPlayerInfoUpdatePacket` to show newly visible players
  - Operators (permission level 2+) are always visible to everyone
  - Automatic restore of full tab list when isolation is disabled
  - Cleanup on player logout via `PlayerLoggedOutEvent`

### MineBay — Step Indicator Position
- **Bug**: Step progress indicators (1, 2, 3) floated at the header title level, overlapping with "MineBay – Player Trading" text
- **Fix**: Moved to right-aligned position on the "Step 1:" subtitle row (30×14px, compact)

### Tab Isolation — Players Not Reappearing After Dimension Travel
- **Bug**: When a player traveled to another dimension and came back, they remained invisible in the tab list of other players until reconnect
- **Root Cause**: `onPlayerChangedDimension` was clearing the player's entry from `previousVisiblePlayers`, causing the first-evaluation fallback to assume they saw ALL online players. Since the "should see" set (same-dimension only) was always a subset, the differential logic never generated ADD packets for returning players
- **Fix**: Kept `previousVisiblePlayers` intact across dimension changes — the differential logic naturally computes correct adds and removes

### Tab Isolation — Asymmetric Visibility Through Portals (Revised)
- **Bug**: When a player portaled to another player's dimension, the arriving player could see existing players, but existing players could NOT see the arriving player (fixed only by reconnect)
- **Root Cause (v2)**: `server.execute()` doesn't reliably delay by a full tick — tasks added from the main thread can be polled during `waitUntilNextTick()` in the SAME tick. The ADD packet was sent before vanilla's network flush completed, and before the client processed the respawn/entity-tracking packets. Additionally, vanilla's client uses `putIfAbsent` for `ADD_PLAYER` — if a stale entry existed (from a previous session or vanilla's own broadcasts), the add was silently ignored
- **Fix**: Replaced `server.execute()` with a proper **3-tick countdown** (`dimensionChangeCountdown`) that counts down in the `onServerTick` handler. Also added **force-remove-before-add**: before sending `createPlayerInitializing`, a `ClientboundPlayerInfoRemovePacket` is sent first to clear any stale entries, guaranteeing the client processes the ADD as fresh

### Tab Isolation — Disable Not Restoring Immediately
- **Bug**: Toggling tab isolation off in Global Settings didn't restore the full player list until the next 20-tick cycle
- **Fix**: Added `onIsolationToggled()` method called immediately from the toggle packet handler, bypassing the tick-based update interval

### Spectate — Cross-Dimension Camera Tracking (Revised)
- **Bug**: The spectator successfully teleported to the target's new dimension, but the camera detached and got stuck at the spawn position instead of reattaching to the target
- **Root Cause (v2)**: After cross-dimension teleport, the client receives a `ClientboundRespawnPacket` which resets `cameraEntity = null` and creates a new `ClientLevel`. The subsequent `ClientboundSetCameraPacket` (sent via `server.execute()` — same tick or next tick) arrives before the client has received entity tracking data for the target in the new level. `packet.getEntity(this.level)` returns `null`, so the camera is NOT set. Additionally, `tickSpectators` had no cooldown — it re-triggered the teleport+setCamera every tick, creating an infinite loop
- **Fix**: Added `pendingReattachTicks` field to SpectateData with a **10-tick settle period** after cross-dimension teleport. During the settle period, all spectate checks are skipped. After 10 ticks (enough for entity tracking to establish), `setCamera(target)` is called with fresh player references. The tick handler now properly skips processing while settling

### Spectate — Player Model Visibility
- **Bug**: When an admin started spectating another player, the admin's player model became invisible to all other players, making it obvious something was happening
- **Root Cause**: The spectate system switched the admin to `GameType.SPECTATOR`, which causes vanilla's `broadcastToPlayer()` to return `false` for non-spectator observers — making the entity invisible
- **Fix**: Complete rework of the spectate system with two modes:
  - **Stealth mode** (same-dimension): Body stays at original position, no game mode change. Camera is set via direct `ClientboundSetCameraPacket` (not `setCamera()` which auto-moves the body). Position frozen via `absMoveTo` + `setDeltaMovement(ZERO)` every tick. Player set invulnerable. Auto-cancels if target moves beyond entity tracking range (~450 blocks)
  - **Non-stealth mode** (cross-dimension): Body teleports to target's dimension (unavoidable for entity tracking), keeps original game mode (visible to others), set invulnerable. Camera locked via `setCamera()` after settle period. Body follows target via vanilla's tick behavior

### Player Manager — "E" Key Closing Screen
- **Bug**: Pressing "E" while typing a player name in the Player Manager closed the entire screen
- **Root Cause**: `AbstractContainerScreen.keyPressed()` treats the inventory key binding (default "E") as a close action
- **Fix**: Added `keyPressed()` override that intercepts non-Escape keys when an `EditBox` is focused and routes them to the focused widget instead

### PMSyncPlayerListsPacket — Classloading Safety
- **Bug**: Intermittent `NullPointerException` crash (`"this.modClass" is null`) during mod loading
- **Root Cause**: `PMSyncPlayerListsPacket.handle()` directly referenced `net.minecraft.client.Minecraft` — a client-only class — which could cause classloading issues on integrated server threads
- **Fix**: Wrapped client-only references in `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` to safely isolate them from server-side classloading

### Tab Isolation — Player Models Invisible After Dimension Travel
- **Bug**: After a player traveled to another dimension and returned, their 3D player model was invisible to other players (and vice versa). Tab list entries updated correctly, but the actual player entities were not rendered. Sometimes asymmetric: Player A could see Player B but Player B couldn't see Player A
- **Root Cause**: `ClientboundPlayerInfoRemovePacket` removes entries from the client's `playerInfoMap`. When a player returns to a dimension, vanilla sends `ClientboundAddEntityPacket` which calls `createEntityFromPacket()` — this checks `getPlayerInfo(uuid)` and **returns null** if the PlayerInfo was removed, logging "Server attempted to add player prior to sending player info" and skipping entity creation entirely. The player model is never added to the client's level. Our subsequent ADD packet re-adds the PlayerInfo, but the entity tracker won't resend `AddEntityPacket` (it already "sent" it). Asymmetric visibility occurs because the returning player's client still has the other player tracked, but not vice versa
- **Fix**: Replaced ALL `ClientboundPlayerInfoRemovePacket` usage with `ClientboundPlayerInfoUpdatePacket(UPDATE_LISTED)`. The `UPDATE_LISTED` action only affects the `listedPlayers` set (tab overlay visibility) WITHOUT removing from `playerInfoMap`. This preserves entity tracking compatibility — `handleAddEntity` always finds the PlayerInfo, so player models always render correctly. Packet is constructed manually via `RegistryFriendlyByteBuf` buffer because the standard `Entry(ServerPlayer)` constructor hardcodes `listed=true`. Also removed the force-remove-before-add pattern (no longer needed since we never remove PlayerInfo)

### Spectate — Same-Dimension: Hands/HUD Visible, Free Head Movement
- **Bug**: When spectating a player in the same dimension (stealth mode), the spectator could still see their own first-person hands and hotbar, and could freely move their head/look direction independently of the target — not a true "camera lock" experience
- **Root Cause**: Stealth mode only sent a `ClientboundSetCameraPacket` without switching game mode. In survival/creative/adventure mode, the client still renders first-person arms, the hotbar, and allows the player to look around. Only `GameType.SPECTATOR` suppresses the HUD and locks the view direction to the target entity
- **Fix**: Unified both spectate modes to switch to `GameType.SPECTATOR` before any camera setup. The `setGameMode(SPECTATOR)` call is now issued once at the top of `spectatePlayer()` for both stealth and non-stealth paths. Game mode is restored on stop, target disconnect, or out-of-range cancellation in all code paths

### Spectate — Cross-Dimension: Hands/HUD Visible, Body Collides with Target
- **Bug**: When spectating a player in another dimension (non-stealth mode), the spectator could still see their own first-person hands and hotbar, could move their head freely, and their player model was physically visible to the target — following them, obstructing their view, and colliding with their body causing unwanted movement
- **Root Cause**: Non-stealth spectate deliberately avoided `GameType.SPECTATOR` to keep the spectator body "visible," but this caused three compounding problems: (1) client renders first-person hands/HUD for non-spectator game modes, (2) survival/creative players have `noPhysics=false` — full entity collision, (3) `broadcastToPlayer()` returns `true` for non-spectators — body is visible and tracked by all nearby players
- **Fix**: Non-stealth (cross-dimension) spectate now switches the spectator to `GameType.SPECTATOR` before teleporting. This hides first-person hands (vanilla doesn't render them when camera is set to another entity in spectator mode), sets `noPhysics=true` (no collision), and makes the body invisible via `broadcastToPlayer()` returning `false`. Original game mode is saved in `SpectateData.gameMode` and restored on stop or target disconnect

### Spectate — Tab List Shows Spectator Mode (Italic/Gray Name)
- **Bug**: When a player started spectating, their name in the tab list changed to italic gray text — the vanilla spectator mode styling — revealing to all other players that they were spectating
- **Root Cause**: `setGameMode(GameType.SPECTATOR)` calls `ServerPlayerGameMode.changeGameModeForPlayer()` which broadcasts `UPDATE_GAME_MODE(SPECTATOR)` to ALL clients via `broadcastAll()`. The client's `PlayerTabOverlay` checks `getGameMode() == GameType.SPECTATOR` and applies `ChatFormatting.ITALIC` + semi-transparent color (line 84 of `PlayerTabOverlay.java`)
- **Fix**: After `setGameMode(SPECTATOR)`, immediately send a fake `UPDATE_GAME_MODE` packet to all OTHER clients with the spectator's original game mode (`data.gameMode`). The spectator's own client correctly knows it's in spectator mode (via `ClientboundGameEventPacket.CHANGE_GAME_MODE`), but all other clients see the player as survival/creative in the tab list. Fake packets are constructed manually via `RegistryFriendlyByteBuf`. Also added `onPlayerJoined()` hook to send fake game mode to newly joining players who would otherwise receive the real SPECTATOR mode via vanilla's `createPlayerInitializing` broadcast

### Spectate — Body Removed from Original Dimension on Target Cross-Dim Travel
- **Bug**: In stealth mode (started same-dimension), when the spectated player traveled to another dimension, spectating ended immediately with "target changed dimension." However, when spectating started cross-dimension (non-stealth), dimension changes worked perfectly with seamless camera tracking
- **Root Cause**: Stealth mode's cross-dimension handler simply ended spectating instead of following the target. Camera packets can't work cross-dimension (entity not tracked in the spectator's ClientLevel), so the camera would go blank. But non-stealth mode already had working cross-dimension support (teleport + settle + camera reattach)
- **Fix**: Instead of ending spectating, stealth mode now **transitions to non-stealth mode** when the target crosses dimensions. The spectator body teleports to the target's dimension (same as starting cross-dimension spectating), `data.stealthMode` is set to `false`, and the non-stealth tick handler takes over with `pendingReattachTicks`. The original body position (`data.x/y/z/dimension`) is preserved — `stopSpectate` always teleports back to the saved position. When the target returns to the spectator's original dimension, the system transitions **back to stealth mode**: body teleports to saved position, `data.stealthMode` reset to `true`, and the stealth tick handler resumes freezing the body in place. Fake game mode packets are re-broadcast after each dimension transition to maintain tab list disguise

### Spectate — Camera Stuck After Cross-Dimension Travel (Revised)
- **Bug**: When the spectated player traveled to another dimension, the spectator was teleported correctly but the camera got stuck at the spawn position and never reattached to the target
- **Root Cause**: After cross-dimension teleport, `setCamera(target)` sends a single `ClientboundSetCameraPacket`. On the client, `handleSetCamera` calls `getEntity(this.level)` to find the target — but entity tracking hasn't been established yet in the new `ClientLevel` (created by the respawn packet). The packet is silently ignored. The server-side `camera` field is set to `target`, so subsequent ticks see `getCamera() == target` and don't retry
- **Fix**: Added continuous `ClientboundSetCameraPacket(target)` reinforcement every tick in non-stealth mode (same pattern as stealth mode). Even if the first packet arrives before entity tracking, subsequent packets will succeed once the target entity is tracked. Cheap packet (just an entity ID integer)

### Spectate — Fire/Damage Invincibility Persists After Server Restart
- **Bug**: After spectating, a player could become permanently invincible to all damage (fire, fall, melee, etc.) even in Survival mode. The invincibility persisted across reconnects and server restarts
- **Root Cause**: `Entity.setInvulnerable(true)` is called when spectating starts, and the `Invulnerable` NBT flag is persisted to the player's save data. While all normal exit paths call `setInvulnerable(false)`, if the server crashes or is force-stopped while a player is actively spectating, the cleanup never runs. On next login, the player loads with `Invulnerable:1b` but is not in the spectating map — no code ever clears the flag
- **Fix**: Added a safety net in `onPlayerJoined()`: if a joining player has `isInvulnerable() == true` but is NOT in the active spectating map, the flag is forcibly cleared with a warning log. Also added debug logging to all `setInvulnerable()` calls (start, stop, target disconnect, out of range) for future traceability

### Spectate — Camera Freaks Out Before Range Disconnect
- **Bug**: When the spectated player walked away from the spectator's frozen body position (stealth mode), the camera would start flickering and freaking out well before spectating was cancelled
- **Root Cause**: The range check used a hardcoded 450-block limit, but entity tracking is based on server view distance (typically 10 chunks = 160 blocks). The client would lose the target entity at ~160 blocks, causing camera flickering for ~290 blocks before the range check finally triggered
- **Fix**: Range is now calculated dynamically from `server.getPlayerList().getViewDistance()`, stopping 2 chunks before the actual tracking limit (`(viewDistance - 2) * 16`, minimum 48 blocks). Camera is also reset to self FIRST (before game mode restore) for a clean visual exit with no flicker frame

### Debug Test Launcher — Race Condition
- **Bug**: In `test.bat debug` mode, one of the two clients would intermittently crash with "mods that were not found"
- **Root Cause**: Each `runClient`/`runServer` Gradle task re-ran `compileJava`, which briefly cleared and rewrote `build/sourceSets/main` while the other client's JVM was actively reading from it
- **Fix**: Added `-x compileJava -x processResources -x classes` flags to all launch commands in debug mode, since `gradlew classes` already runs at the top of the block

### Debug Test Launcher — MultiLoader Rewrite
- **Change**: Complete rewrite of `test.bat` for the MultiLoader project structure
- Accepts loader parameter: `test.bat [mode] [forge|neoforge|fabric]` (default: `forge`)
- Gradle task prefix resolves to `:<loader>:runClient` / `:<loader>:runServer`
- All paths (server dir, client dir, logs) resolve under `<loader>/runs/`
- Debug mode client2 directory created under the selected loader
- `-PmcUsername` and `-PmcGameDir` properties wired in ForgeGradle (`args`), ModDevGradle (`programArguments`), and fabric-loom (`programArg`) run configs

---

## New Features

### Player Manager — Complete Rework
- **Kick**: Kick online players with optional reason message
- **Ban/Unban**: Ban players (with optional IP ban) and unban by name; works for offline players via profile cache
- **Whitelist**: Add/remove players, toggle whitelist enforcement on/off via `PMWhitelistTogglePacket`
- **Spectate**: Watch another player's perspective with full cross-dimension tracking (see fix above)
- **View Inventory**: Open a read-only view of another player's inventory
- **Dynamic layout**: Row count adapts to panel height via `getMaxRows()`; self-player filtered from list
- **Auto-sync**: Server pushes updated ban/whitelist/toggle state to client after every mutation via `PMSyncPlayerListsPacket`
- **Tabbed interface**: Online / Banned / Whitelist tabs with per-tab search and pagination

---

## Files Modified

- `gui/screen/DashboardScreen.java` — init() rewrite
- `gui/screen/PerformanceSettingsScreen.java` — panel size, header height, scroll support
- `gui/screen/WorldDetailScreen.java` — dimensions, spacing, label positions
- `gui/screen/ConsoleScreen.java` — dimensions, title offset, input layout
- `gui/screen/ConfigScreen.java` — full visual overhaul with descriptions and row styling
- `gui/screen/PlayerManagerScreen.java` — complete rework with tabs, dynamic layout, keyPressed fix
- `gui/economy/EconomyManagementScreen.java` — template card compression
- `gui/minebay/MineBayScreen.java` — Step 1 & Step 2 layout rework
- `gui/gambling/MineStacksScreen.java` — panel size increase
- `features/worldmanager/ChatIsolationHandler.java` — fixed config mismatch, proper dimension-based filtering
- `features/worldmanager/TabListIsolationHandler.java` — full rewrite with differential packets, 3-tick countdown on dimension change, force-remove-before-add, immediate toggle response
- `features/playermanager/PlayerManagerSingleton.java` — spectate cross-dimension tracking, kick/ban/unban/whitelist, tickSpectators()
- `features/playermanager/PlayerManagerEvents.java` — server tick handler for spectate tracking
- `features/playermanager/PlayerManagerClientData.java` — client-side storage for synced player lists
- `network/packet/PMSyncPlayerListsPacket.java` — DistExecutor classloading fix
- `network/packet/PMWhitelistTogglePacket.java` — new packet for whitelist on/off toggle
- `network/packet/WMToggleTabIsolationPacket.java` — calls onIsolationToggled() immediately after toggle
- `test.bat` — race condition fix in debug mode

---

## Security & Safety Audit Fixes

### CRITICAL — Privilege Escalation in Console Command Execution
- **Bug**: `ConsoleCommandPacket` used `server.createCommandSourceStack()` (permission level 4 / console) instead of the player's own command source stack. This allowed OP2 players to execute console-level commands like `/stop`, `/op`, etc.
- **Fix**: Changed to `player.createCommandSourceStack()` which respects the player's actual permission level

### CRITICAL — Missing Permission Check on Dimension Teleport
- **Bug**: `WMTeleportToDimensionPacket` had no permission check — any connected client could teleport to any dimension by sending the packet directly
- **Fix**: Added `player.hasPermissions(2)` check before teleportation

### CRITICAL — No Rate Limiting on OTA File Transfer
- **Bug**: `ModFileRequestPacket` had no rate limiting — a malicious client could repeatedly request the full mod JAR, consuming server bandwidth and disk I/O
- **Fix**: Added `hasActiveOrCompletedTransfer()` check to allow only one transfer per player per session. Added player disconnect detection during transfer to abort early and save bandwidth

### HIGH — Stale Player Reference in Gambling Delayed Callback
- **Bug**: `PlaceGamblingBetPacket` captured the `player` and `account` objects in its 3-second delayed callback. If the player disconnected and reconnected during that delay, the callback would use stale references (potentially sending packets to a dead connection or reading stale balance data)
- **Fix**: Captured `playerUUID` instead, then re-lookups `ServerPlayer` by UUID in the delayed callback. Returns early if the player is no longer online. Re-fetches the `BankAccount` for fresh data

### HIGH — Network Buffer Oversized in SaveMotdPacket
- **Bug**: `SaveMotdPacket` decoded with `readUtf(32767)` (32KB) but the handler truncated to 512 characters. A malicious client could send 32KB packets for a 512-char field
- **Fix**: Changed decode to `readUtf(MAX_MOTD_LENGTH)` (512) to reject oversized data at the network layer

### HIGH — Unbounded Integer Fields in SaveTemplatePacket
- **Bug**: `SaveTemplatePacket` decoded `goal` and `rewardAmount` as raw `readInt()` with no validation — could set goals of `Integer.MAX_VALUE` or negative rewards
- **Fix**: Added bounds clamping: goal 1–10,000, rewardAmount 0–100,000

### HIGH — Unbounded Fields in SaveFreeRewardSettingsPacket
- **Bug**: `rewardAmount` and `cooldownHours` had no upper bounds — could set extreme values
- **Fix**: Capped rewardAmount to 100,000 and cooldownHours to 720 (30 days)

### MEDIUM — Unbounded Timer Duration in WMSetTimerPacket
- **Bug**: `seconds` field had no bounds validation — could set timers to `Integer.MAX_VALUE`
- **Fix**: Added bounds check: 0–2,592,000 (max 30 days)

### MEDIUM — OOM Risk in CreateListingPacket Decode
- **Bug**: `priceItemCount` was read as raw `readInt()` with no cap — a malicious packet could cause the server to allocate millions of list entries and OOM
- **Fix**: Capped `priceItemCount` to 54 (max inventory size) at decode time

### MEDIUM — OOM Risk in ModFileChunkPacket Decode
- **Bug**: `dataLength` for chunk data was read as raw `readInt()` with no cap — a crafted packet could allocate an arbitrarily large byte array
- **Fix**: Capped `dataLength` to `CHUNK_SIZE + 1024` (33KB) at decode time

---

## Thread Safety Fixes

### CRITICAL — GamblingManager Executor Thread Leak
- **Bug**: `GamblingManager.saveScheduler` (a `ScheduledExecutorService`) was never shut down on server stop. On repeated `/reload` or server restarts, this leaked executor threads
- **Fix**: Added `GamblingManager.shutdown()` method that shuts down the scheduler and performs a final save. Wired into `onServerStopping()` replacing `forceSave()`

### CRITICAL — DailyTasksManager Thread-Unsafe HashMap
- **Bug**: `DailyTasksManager.playerTasks` used a plain `HashMap` accessed from both the server tick thread (task progress updates) and network threads (packet handlers). This is a data race that can cause `ConcurrentModificationException` or silent data corruption
- **Fix**: Changed to `ConcurrentHashMap`. Also added migration in `load()` to convert deserialized `HashMap` instances

### CRITICAL — TransactionManager Unbounded In-Memory Growth
- **Bug**: `TransactionManager.completedTransactions` grew unbounded in memory — `save()` limited the on-disk file to 1000 entries, but the in-memory map was never trimmed
- **Fix**: After saving, trim the in-memory map to retain only the 1000 most recent entries

### HIGH — Unsynchronized Singleton Access
- **Bug**: `GamblingManager.getInstance()` and `PlayerManagerSingleton.getInstance()` were not `synchronized`, creating a race condition where two threads could each create a separate instance
- **Fix**: Added `synchronized` keyword to both `getInstance()` methods

### MEDIUM — Non-Volatile Shared Fields
- **Bug**: `RedstoneThrottleHandler.lastTickCount` and `MobSpawnLimiterHandler.spawnedThisTick`/`lastTickTime` were non-volatile static fields accessed from multiple threads (event handlers can fire from different threads). Without `volatile`, threads may see stale cached values
- **Fix**: Made all affected fields `volatile`

---

## Performance Improvements

### ItemMergeHandler O(n²) Cap
- **Bug**: `ItemMergeHandler` iterated over ALL `ItemEntity` instances in the world with an O(n²) nested loop. In worlds with thousands of dropped items (e.g., mob farms), this caused significant TPS drops
- **Fix**: Capped the item list to 500 entries per dimension per tick. Items beyond this limit are deferred to the next merge cycle

### ModFileTransferManager Disconnect Detection
- **Improvement**: Added `player.hasDisconnected()` check in the file transfer loop. Previously, the server would continue reading and sending chunks even after the client disconnected, wasting I/O and bandwidth

---

## Console Output Polish

Reduced mod log spam from **~40 lines** during startup to **7 clean lines**, and from **9 lines** during shutdown to **2 lines**. All demoted messages are still available at `DEBUG` log level for troubleshooting.

### Startup Output (after polish)
```
Registered 72 network packets
Registered GUI commands: /minebay, /minestacks, /casino, /overflow
ServerManagement v2.1.0 starting (Data Version: 2)
Encryption system initialized
Initialized 5 features (5 enabled, 0 disabled)
Subsystems initialized: TransactionManager, MineBay, Overflow, MineStacks, OTA, MOTD
ServerManagement v2.1.0 fully initialized and ready!
```

### Shutdown Output (after polish)
```
ServerManagement shutting down...
ServerManagement shutdown complete
```

### Changes
- **Consolidated subsystem init**: 8 individual "X initialized" lines → single `Subsystems initialized: ...` summary
- **Consolidated feature registration**: Per-feature `Registered feature: X` and `Initialized feature: X` lines → single `Initialized N features (M enabled, K disabled)` summary
- **Consolidated shutdown**: 4 individual shutdown lines → `shutting down...` / `shutdown complete` pair
- **Removed redundant network log**: Kept only `Registered 72 network packets` (removed preceding `Registering network packets`)
- **Removed migration banners**: `=== CONFIG MIGRATION REQUIRED ===` / `=== MIGRATION SUCCESSFUL ===` replaced with single-line summary
- **Demoted to DEBUG**: Config validation details, config migration steps, per-subsystem data loading (`Loaded X for Y players`), command registration, event handler registration, encryption key loading, OTA version loading, recipe pricing init, performance system init, console streaming init, feature enable/disable per-feature, all data file migration messages
- **Suppressed empty flush**: `AsyncSaveScheduler.flushAll()` now skips logging when `pendingSaves` is empty (was producing 3× "Flushing 0 pending save operations" during shutdown)

### Bug Fixes (discovered during polish)
- **Config validation NPE on startup**: `validateAndRepair()` was called during mod construction before `ForgeConfigSpec` was bound. Every config getter threw NPE → triggered `repairConfig()` which deleted the config → Forge recreated it with ~40 `Incorrect key` WARN lines. Fixed with `SPEC.isLoaded()` guard
- **Config migration NPE**: `needsMigration()` and `checkAndMigrate()` called `CONFIG_VERSION.get()/set()` before spec was loaded. Fixed with `SPEC.isLoaded()` guards in both methods and in the migration 0→1 apply() function
- **Feature count wrong**: `FeatureManager` summary showed "10 enabled" because it counted all `featureStates` config map entries instead of only registered features. Fixed to count `features.keySet()` filtered by `featureStates`

### Files Modified (Console Polish)
- `ServerManagementMod.java` — consolidated startup/shutdown messages
- `ConfigValidator.java` — isLoaded() guard, demoted validation logs
- `ConfigMigration.java` — isLoaded() guards, removed banners, demoted details
- `ModNetworking.java` — removed redundant "Registering" line, demoted client handler log
- `FeatureRegistry.java` — removed verbose registration logs
- `FeatureManager.java` — summary line, fixed count bug, demoted per-feature logs
- `AsyncSaveScheduler.java` — skip empty flush logging
- `PacketTimestampTracker.java` — demoted clearAll() log
- `ModCommands.java` — demoted command registration logs
- `WorldManagerEvents.java` — demoted event registration log
- `PlayerManagerEvents.java` — demoted event registration log
- `SlimeHeadManager.java` — demoted init/disabled logs
- `ServerPerformanceManager.java` — demoted init log
- `ServerConsoleManager.java` — demoted init log
- `EncryptionManager.java` — demoted key loading logs
- `OTAVersion.java` — demoted version loading log
- `EconomyManager.java` — demoted init/load/shutdown logs
- `EconomyData.java` — demoted migration and load logs
- `DailyTaskTemplateManager.java` — demoted migration and load logs
- `DailyTasksManager.java` — demoted load log
- `MoneyRequestManager.java` — demoted migration and load logs
- `AchievementRewardTracker.java` — demoted migration and load logs
- `ItemSupplyDemandTracker.java` — demoted load logs
- `MarginHistoryTracker.java` — demoted load logs
- `TransactionManager.java` — demoted load log
- `GamblingManager.java` — demoted load log
- `MineBayManager.java` — demoted load log
- `OverflowInventoryManager.java` — demoted load log
- `RecipeBasedPricing.java` — demoted init log
- `WorldManagerData.java` — demoted migration, load, backup, corruption logs
- `PlayerPreferences.java` — demoted migration logs
- `SecureDataStorage.java` — demoted encryption migration logs

---

## Files Modified (Audit)

- `network/packet/ConsoleCommandPacket.java` — privilege escalation fix
- `network/packet/WMTeleportToDimensionPacket.java` — permission check added
- `network/packet/ModFileRequestPacket.java` — rate limiting added
- `network/packet/PlaceGamblingBetPacket.java` — stale player reference fix
- `network/packet/SaveMotdPacket.java` — decode buffer size fix
- `network/packet/SaveTemplatePacket.java` — integer bounds validation
- `network/packet/SaveFreeRewardSettingsPacket.java` — upper bounds added
- `network/packet/WMSetTimerPacket.java` — timer bounds validation
- `network/packet/minebay/CreateListingPacket.java` — priceItemCount cap
- `network/packet/ModFileChunkPacket.java` — dataLength cap
- `features/gambling/GamblingManager.java` — synchronized getInstance, scheduler shutdown
- `features/playermanager/PlayerManagerSingleton.java` — synchronized getInstance
- `features/economy/DailyTasksManager.java` — ConcurrentHashMap migration
- `features/economy/TransactionManager.java` — in-memory trimming after save
- `features/economy/EconomyManager.java` — already had proper shutdown (verified)
- `features/serverperformance/RedstoneThrottleHandler.java` — volatile field
- `features/serverperformance/MobSpawnLimiterHandler.java` — volatile fields
- `features/serverperformance/ItemMergeHandler.java` — O(n²) cap
- `server/ModFileTransferManager.java` — transfer tracking, disconnect detection
- `ServerManagementMod.java` — wired GamblingManager.shutdown()

---

## Files Modified (MultiLoader Migration)

- `build.gradle` — stripped to root-level plugin declarations only (apply false)
- `settings.gradle` — MultiLoader subproject includes (`common`, `forge`, `neoforge`, `fabric`, `buildSrc`)
- `gradle.properties` — added NeoForge/Fabric versions, loader versions, Parchment mappings
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 8.10
- `.gitignore` — updated for MultiLoader structure (per-loader `runs/`, `build/`, loom cache)
- `buildSrc/build.gradle` — convention plugin project
- `buildSrc/src/main/groovy/multiloader-common.gradle` — common subproject conventions
- `buildSrc/src/main/groovy/multiloader-loader.gradle` — loader subproject conventions
- `common/build.gradle` — NeoForm + Parchment mappings
- `common/src/main/java/com/servermanagement/` — Constants, Services, IPlatformHelper
- `forge/build.gradle` — ForgeGradle config, run configs with mcUsername/mcGameDir
- `forge/src/main/java/com/servermanagement/` — 254 Java files (moved from `src/`)
- `neoforge/build.gradle` — ModDevGradle config, run configs with mcUsername/mcGameDir
- `neoforge/src/main/java/com/servermanagement/` — 254 Java files (complete port)
- `fabric/build.gradle` — fabric-loom config, run configs with mcUsername/mcGameDir
- `fabric/src/main/java/com/servermanagement/` — 254 Java files (complete port)
- `fabric/src/main/resources/servermanagement.accesswidener` — access widener for Minecraft internals
- `test.bat` — complete rewrite for MultiLoader loader selection
