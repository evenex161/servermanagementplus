# Changelog — ServerManagement+ v2.1.0

**Release Date:** Still in developement
**Latest Changes:** April 22, 2026
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

**Note:** NeoForge port compiles successfully but is not yet runtime-tested. Fabric port has been runtime-tested with critical fixes applied (see Bug Fixes). Forge remains the primary development target.

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

### Debug Test Launcher — buildSrc File Lock in Debug Mode
- **Bug**: In `test.bat debug` mode, Client 2 always failed with `Could not copy file ... precompiled_MultiloaderCommon$_run_closure7.class` during `:buildSrc:compileGroovyPlugins`
- **Root Cause**: Client 1's Gradle JVM holds Windows file locks on `buildSrc/build/` class files for the entire session. Client 2 (even with `--project-cache-dir=.gradle-c2`) still uses the same `buildSrc/build/` directory and fails when attempting to copy locked files
- **Fix**: Added `BUILDSRC_ALT_BUILD_DIR` environment variable support in `buildSrc/build.gradle` — when set, redirects `layout.buildDirectory` to a separate directory. `test.bat` sets this to `build-c2` for Client 2's Gradle invocation

### Fabric — SyncBettingSlotStatePacket Crash on /minestacks
- **Bug**: Executing `/minestacks` on the Fabric port caused an immediate disconnect with `ClassCastException: SyncBettingSlotStatePacket cannot be cast to DiscardedPayload`
- **Root Cause**: `SyncBettingSlotStatePacket` is a C2S (client→server) packet — its `handle()` method calls `context.player()` to get the `ServerPlayer`. However, in the Fabric `ModNetworking.java`, it was registered as S2C (`PayloadTypeRegistry.playS2C()`) with a `ClientPlayNetworking.registerGlobalReceiver` handler. When the client tried to send it, the server didn't recognize it as a valid C2S payload type and cast it to `DiscardedPayload`
- **Fix**: Moved registration from `playS2C()` to `playC2S()`, replaced `ClientPlayNetworking.registerGlobalReceiver` with `ServerPlayNetworking.registerGlobalReceiver`

### Fabric — Missing Commands (/sm, /bank, /servermanagement, etc.)
- **Bug**: On the Fabric port, only `/minebay`, `/minestacks`, `/casino`, and `/overflow` commands were available. All other commands (`/sm`, `/servermanagement`, `/bank`, `/smconfig`, `/worldmanager`, `/playermanager`, etc.) were missing
- **Root Cause**: The `CommandRegistrationCallback` in `ServerManagementModFabric.onInitialize()` only registered 3 commands (MineBay, MineStacks, Overflow). The main `ModCommands.onRegisterCommands(dispatcher)` — which registers all 20+ admin and player commands — was never called
- **Fix**: Added `ModCommands.onRegisterCommands(dispatcher)` to the `CommandRegistrationCallback`

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


---

## Pre-Release Patches (April 22, 2026)

### GUI scaling Phase 2 — uniform pose-matrix scaling for all container screens

- **Symptom**: After Phase 1 (uniform fit-scale for the panel + slot Y multiplied by `vScale`), GUI Scale 1 looked correct but GUI Scales 2 and 3 progressively smeared widgets and text out of place, and GUI Scale 4 / Auto was unusable. The root cause was a coupling mismatch: only the panel rectangle and the menu's slot Y constants were scaled, while every per-screen widget, label, and custom-drawn background sat at hard-coded design-space offsets (`leftPos + 15`, `topPos + 60`, `centerY + 30`, etc.) and continued to draw at full size on top of a now-shrunken panel
- **Fix — `ScalableContainerScreen<T>` base class**: New base class in `com.servermanagement.gui` that wraps the entire panel render in a single uniform pose-matrix scale. `init()` computes `guiScale = ScreenScaler.scaleFactor(designW, designH, width, height)` and keeps `imageWidth/imageHeight` at the original design size so AbstractContainerScreen continues to lay out slots and labels in design space. `render()` is `final`: it calls `renderBackground` once un-scaled, then pushes a pose anchored at the screen center (`pose.translate(cx,cy,0); pose.scale(s,s,1f); pose.translate(-cx,-cy,0)`), invokes a new `protected renderContent(...)` method that subclasses override (this is where each screen does its old `renderBg → drawString → super.render → renderTooltip` work), then pops the pose. Mouse coordinates are inverse-transformed for `mouseClicked/Released/Dragged/Scrolled` (`design = cx + (real - cx) / s`, drag deltas divided by `s`) so click hit-testing matches the visually scaled layout. A `suppressBackgroundOnce` flag prevents AbstractContainerScreen.render's own `renderBackground` call from re-blacking the screen inside the scaled pose
- **Menu reverts**: `MineBayMenu` and `MineStacksMenu` now use pure design-space slot Y constants again (offering Y=85, offer Y=148, inventory Y=230 for MineBay; bet Y=30, inventory Y=140 for MineStacks). The Phase 1 `vScale` factor and the `getScaledPanelWidth` / `getClientPanelWidth` / `getScaledVerticalFactor` helpers were deleted because the new base class scales slots visually via the pose matrix — keeping them would double-scale and round slots into wrong rows
- **Per-screen migration**: All 17 container screens migrated through a single recipe — extends changed from `AbstractContainerScreen<X>` to `ScalableContainerScreen<X>`, `super(menu, inv, title)` → `super(menu, inv, title, designW, designH)`, the `int[] dim = ScreenScaler.scale(W, H, this.width, this.height); imageWidth = dim[0]; imageHeight = dim[1];` block removed from `init()`, `public void render(...)` renamed to `protected void renderContent(...)`, inner `super.render(...)` → `super.renderContent(...)`, redundant `renderBackground/renderTooltip` calls removed, and the now-unused `ScreenScaler` import deleted. `ItemPickerScreen` extends `Screen` directly (not a container screen) and was left untouched
- **Files**: 1 new base class (`gui/ScalableContainerScreen.java`) + 17 screens + 2 menus per loader × 3 loaders = 60 files. All three loaders compile clean (`:forge:compileJava :neoforge:compileJava :fabric:compileJava` → BUILD SUCCESSFUL)
- **Phase 3 (deferred)**: Tooltips and item-stack stack-counts render through the same pose matrix, so at small workspaces tooltip text shrinks to match the panel. A follow-up pass can pop the pose before tooltip rendering, or selectively re-scale text up by `1/guiScale` so tooltip fonts always stay legible. Per-screen "compact mode" alternative layouts for GUI Scale 4/Auto are also still on the table for screens whose internal grids stay cramped after uniform scaling (DailyTasks 400×430, EconomyManagement 600×450)

### GUI scaling Phase 2.1 — eliminate ghost panel rendered at design-space anchor

- **Symptom**: After Phase 2 every screen showed a small unscaled "ghost" copy of the panel header drifting near the top-left of the workspace, in addition to the correctly-scaled panel at the screen center. F3+M debug overlay reported the right design-space anchor and panel size, so the bug was strictly in the render pipeline, not in layout math
- **Root cause**: In MC 1.21.1 Forge, `AbstractContainerScreen` overrides `Screen.renderBackground(...)` to call **both** `renderTransparentBackground(g)` **and** `renderBg(g, pt, mx, my)`. The Phase 2 base class called `renderBackground(...)` once at the top of `render()` (intending only the dim world overlay) — but the polymorphic dispatch landed on the `AbstractContainerScreen` override, which painted the panel at design-space `(leftPos, topPos)` (e.g. `(40, -38)`) without the pose-matrix scale, producing the ghost. The scaled pose then painted the real panel correctly, leaving two visually distinct copies
- **Fix — `ScalableContainerScreen.render()` rewrite**: Instead of calling `this.renderBackground(...)`, the base class now inlines the three `Screen` primitives that produce only the dim world overlay (`renderPanorama` when `level == null`, `renderBlurredBackground`, `renderMenuBackground`). `renderBg` is no longer invoked outside the scaled pose. The `suppressBackgroundOnce` flag is still set before `pose.pushPose()` so the second `renderBackground` call inside `Screen.render` (triggered from `AbstractContainerScreen.render → super.render`) is also skipped, preventing a redundant `renderBg` repaint at design coords inside the scaled pose
- **Files**: 1 file per loader × 3 loaders = 3 files. No per-screen changes needed — all 17 migrated screens automatically pick up the fix through the `final render()` in the base class. Compile clean across `:forge:compileJava :neoforge:compileJava :fabric:compileJava`

### GUI scaling Phase 2.2 — frosted-glass contrast layer + MineBay inventory frame

- **Symptom**: With the Phase 2.1 ghost-panel removal, screens whose `renderBg` overrides were never re-invoked (Bank, DailyTasks, Achievements, MineBay, EconomyManagement, MineStacks) had no opaque dark panel at all — only the blurred world background showed through. The look was visually pleasing ("frosted glass") but text rendered against the bright sky was hard to read, and the MineBay player inventory had no visible frame so it blended into the world
- **Fix — uniform translucent backdrop in base class**: `ScalableContainerScreen.render()` now paints a soft frosted backdrop (`0x80101015` fill + 1px `0x60FFFFFF` outline) at the design-space panel rect via a new `protected drawFrostedPanel(GuiGraphics)` hook called inside the scaled pose, before the subclass's `renderContent`. The blurred world stays visible underneath but every text label now has a guaranteed dark contrast base. Screens with their own opaque `renderBg` (Dashboard, ConfigScreen, ConsoleScreen, GlobalSettingsScreen, MotdEditorScreen, PerformanceSettingsScreen, PlayerManagerScreen) layer on top and remain readable
- **Inventory-frame helper**: New `protected drawInventoryPanel(g, slotX, slotY, rows, hotbarGap, drawLabel)` in the base class. Paints a slightly darker frosted strip (`0x90080810`) + 1px outline framing the 9×rows slot grid + hotbar, plus a thin separator line between grid and hotbar and an optional `Inventory` label above the grid. Reusable by any container screen that exposes the player inventory
- **MineBayScreen integration**: `renderContent` now calls `drawInventoryPanel(g, leftPos+219, topPos+230, 3, 4, true)` before `super.renderContent` (so slots and items render on top), only when `menu.isInventoryVisible()`. The previous opaque `0xE0202020` inventory backdrop in `renderBg` is dead code (suppressed since Phase 2.1) but left in place — the new helper supersedes it
- **Files**: 1 base class (`gui/ScalableContainerScreen.java`) + 1 screen (`gui/minebay/MineBayScreen.java`) per loader × 3 loaders = 6 files. Compile clean across `:forge:compileJava :neoforge:compileJava :fabric:compileJava`

### GUI scaling Phase 2.3 — contrast polish & animation fixes

Five user-reported visual issues against the Phase 2.2 baseline. All fixes applied symmetrically to forge / neoforge / fabric.

- **MineBay — drop redundant "Inventory" label**: The Phase 2.2 inventory frame painted an "Inventory" header above the value text, duplicating the more informative "Inventory value" line below it. `MineBayScreen.renderContent` now passes `drawLabel=false` to `drawInventoryPanel(g, leftPos+219, topPos+230, 3, 4, false)` — the frame and separator stay, only the redundant label is suppressed
- **DailyTasks — divider lines between task cards**: Task cards rendered as one continuous block with no visual separation, so adjacent rows blurred into each other. `DailyTasksScreen.renderTasks` now draws a thin `0x40FFFFFF` 1px line between consecutive tasks at `taskY + taskSlotHeight - 4`, spanning `centerX + 20 → centerX + imageWidth - 20`. Skipped after the last visible row
- **MineStacks — animation runs at correct speed on NeoForge**: `Screen.render`'s `partialTick` parameter has different semantics across loaders — Forge passes a per-tick fraction in `[0, 1]`, but NeoForge 21.x passes a realtime delta-ticks value (often `> 1.0`). The MineStacks tension/win/animation loops multiplied that float (`partialTick * 20f`, `* 10f`, `* 0.02f`, `* 0.05f`), so the same code ran ~2× faster on NeoForge. `MineStacksScreen` now derives its own `frameDeltaTicks` from `System.nanoTime()` (50 ms = 1 tick, clamped to `1.0` to absorb GC pauses, with a `0.05` first-frame fallback) and uses it consistently. Forge timing is unchanged; NeoForge now matches
- **MineStacks — animation overlay covers the full screen**: The tension and ending animations darkened the screen with `guiGraphics.fill(0, 0, this.width, this.height, 0x80/60000000)`. Inside the scaled pose, `(0,0,width,height)` interprets as design-space, so the rect shrunk inward and left bright bands at the screen edges. New `protected fillScreen(GuiGraphics g, int color)` helper in `ScalableContainerScreen` computes the pre-scale rect that maps to actual screen-space `(0..width, 0..height)` based on the scaler's center anchor and active scale factor. Both MineStacks overlays now call `fillScreen(...)` and cover the entire viewport correctly
- **DashboardCard — title text no longer overflows**: Card descriptions already truncated with a `..` suffix when wider than `width - 6`, but the title `drawCenteredString` was unbounded. Centered titles wider than the card overflowed both edges; the visual symptom was left-column titles getting clipped on the right and right-column titles getting clipped on the left. `DashboardCard.renderWidget` now applies the same trim-and-append-`..` loop to the title before drawing
- **Files**: `gui/ScalableContainerScreen.java` (added `fillScreen` helper), `gui/minebay/MineBayScreen.java`, `gui/economy/DailyTasksScreen.java`, `gui/gambling/MineStacksScreen.java`, `gui/widgets/DashboardCard.java` × 3 loaders = 15 files. Compile clean across `:forge:compileJava :neoforge:compileJava :fabric:compileJava` → `BUILD SUCCESSFUL`



Follow-up bug-fix passes against the v2.1.0 baseline. All three loaders compile clean (`:forge:compileJava :neoforge:compileJava :fabric:compileJava` â†’ BUILD SUCCESSFUL).

### Stale Client Cache on GUI Reconstruction

- **Bug**: After v2.1.0, every `Sync*Packet.handle` triggers `ClientPacketHandler.refreshOpenScreen()` which re-invokes `screen.init()`. But several menus latched cache snapshots in their constructors and never re-read them, so `init()` re-ran with already-stale data and the GUI only showed fresh values after the user closed and reopened the screen
- **Fix**: Added a public `reloadFromClientCache()` method to `BankMenu`, `DailyTasksMenu`, `AchievementsMenu`, and `MotdEditorMenu` (Ã— 3 loaders = 12 files). Each menu's constructor now delegates to the new method. The matching screens â€” `ConfigScreen`, `BankScreen`, `DailyTasksScreen`, `AchievementsScreen`, `MotdEditorScreen`, `PortalTimerScreen` (Ã— 3 loaders = 18 files) â€” now invoke the reload from `init()` so a late sync packet rebuilds the GUI with current data
- **Special handling**:
  - `AchievementsScreen` also rebuilds its private `achievementsList` field from the refreshed menu set (the screen's ctor copies it once)
  - `MotdEditorScreen` is guarded by `if (originalMotdText == null)` to avoid clobbering pending unsaved edits
  - `PortalTimerScreen` re-reads `ClientPacketHandler.getCachedDimensionId()` (mirror of an earlier `WorldDetailScreen` fix)
- **Files**: `BankMenu`, `DailyTasksMenu`, `AchievementsMenu`, `MotdEditorMenu`, `BankScreen`, `DailyTasksScreen`, `AchievementsScreen`, `MotdEditorScreen`, `ConfigScreen`, `PortalTimerScreen` Ã— 3 loaders (30 files)

### Fabric â€” Inventory Tooltip Prices Not Synced to Economy Engine

- **Bug**: On Fabric only, inventory tooltip item and stack prices showed vanilla rarity-based fallback values instead of the Economy Engine's recipe-derived prices. MineBay tooltips eventually corrected via the periodic `EconomyServerHandler` sync, but inventory tooltips stayed stale
- **Root Cause**: Forge/NeoForge fired `LoginNotificationHandler.onPlayerLogin(player)` automatically via `@SubscribeEvent` for `PlayerLoggedInEvent`, which calls `EconomyManager.syncMarketPrices(player)` and populates client `recipePrices`. Fabric's `ServerManagementModFabric.onInitialize()` JOIN handler had no equivalent dispatch â€” `recipePrices` stayed empty and `ClientMarketData.getBasePrice()` fell back to `ItemValuation.getItemValue(...)` (vanilla rarity)
- **Fix**: Added explicit dispatch of `LoginNotificationHandler.onPlayerLogin(p)` (with try/catch) inside the Fabric `ServerPlayConnectionEvents.JOIN` handler

### Portal Timer â€” Live Countdown Didn't Appear Until GUI Reopen

- **Bug**: After starting a portal timer from the World Detail screen, the new live "Time left: M:SS (Type)" countdown line didn't appear until the player closed and reopened the screen
- **Root Cause**: `WMSetTimerPacket` mutated server-side state but never pushed a fresh `SyncWorldDetailPacket` back to the originating player. The client only refreshed via the next periodic sync
- **Fix**: `WMSetTimerPacket.handle` now constructs and sends `SyncWorldDetailPacket(dimensionId, areNetherPortalsEnabled, areEndPortalsEnabled, hasActiveTimer, (int) getRemainingTime, isDimensionChatConnected, getTimerPortalType)` to the originating player on success via `ModNetworking.sendToPlayer(packet, player)` (Ã— 3 loaders)

### Portal Timer â€” Seconds EditBox Counted Down Mid-Edit

- **Bug**: While a portal timer was running, the seconds EditBox in `WorldDetailScreen` kept counting down on every screen refresh â€” confusing because the field looked editable while actually being overwritten by sync packets
- **Root Cause**: `init()` unconditionally called `setValue(hasTimer ? String.valueOf(timerSeconds) : "60")`, so each refresh stamped the remaining-seconds value into the EditBox
- **Fix**: When `hasTimer == true`, the EditBox is now cleared (`setValue("")`), given hint text "running" (`setHint(Component.literal("running"))`), and locked (`setEditable(false)`). The "Set Timer" button is also no longer added while a timer is active. The "Clear" button stays available

### Portal Timer â€” Broadcast/Chat Messages Didn't Mention the Dimension

- **Bug**: Portal timer countdown broadcasts and the completion message read e.g. "Nether portals close in 1 minute" with no dimension context. Players in The Nether or modded dimensions had no way to tell whether their own portals were affected and could be misled into thinking their world's portals were about to flip
- **Root Cause**: `WorldManager.getPortalDescription(dimensionId, portalType)` only returns a portal-type label like "Nether portals". The `TimerTickHandler` call sites assembled messages from that label alone, never interpolating the dimension name
- **Fix**: `TimerTickHandler.sendTimerWarnings()` and `handleTimerComplete()` now also call `WorldManager.getDimensionName(dimensionId)` and include " in &lt;dimName&gt;" in the 60s chat heads-up, every subtitle line (60s / 30s / 10s / 5s / 4-1s / completion), and the completion chat message. Title-bar countdown subtitles now read e.g. "Nether portals in Overworld" instead of the prior generic "..." placeholder

### Portal Timer â€” Chat Spam from Per-Interval Announcements

- **Bug**: Each warning interval (60s, 30s, 10s, 5s) produced a chat line in addition to the title + sound. With multiple players online and the per-second 4-1s tail, this created excessive chat noise
- **Fix**: Dropped chat lines at the 30s, 10s, and 5s announcements (kept their title + subtitle + sound). Only the 60s heads-up and the completion result still print to chat. Title broadcasts already reach every player regardless of dimension, so no information is lost. Net effect: 5 chat lines per timer reduced to 2

### Portal Timer â€” Travel Cancelled During Countdown (Defeated the Warning)

- **Bug**: While a portal timer was running, players couldn't travel through the portal at all â€” even during an enabledâ†’disabled countdown that was loudly announcing "you have N seconds". This defeated the entire purpose of the warning window
- **Root Cause**: `PortalEventHandler.onEntityTravelToDimension` had a block that always cancelled travel when `hasActiveTimer && remainingTime > 0`, regardless of which direction the timer was transitioning. Analysis:
  - **enabled â†’ disabled**: portal state hasn't flipped yet â†’ still enabled. The cancel was the *only* thing blocking travel and it contradicted the announced escape window
  - **disabled â†’ enabled**: portal state hasn't flipped yet â†’ still disabled. The upstream `if (!portalAllowed)` check already cancels travel with a clean, dimension-aware message. The timer-active block was redundant
- **Fix**: Removed the timer-active travel cancellation block from `PortalEventHandler.onEntityTravelToDimension` (Ã— 3 loaders). Travel during an enabledâ†’disabled countdown now works, giving players the actual escape window. Travel during a disabledâ†’enabled countdown is still correctly blocked by the existing `!portalAllowed` branch with a dimension-aware error message. Manual portal frame ignition / portal block placement during a timer is also unaffected â€” those checks only consult the current portal-enabled state

### Files Modified (Pre-Release)

- `network/packet/WMSetTimerPacket.java` Ã— 3 â€” pushes fresh `SyncWorldDetailPacket` on success
- `gui/screen/WorldDetailScreen.java` Ã— 3 â€” EditBox lock + Set Timer button hide while timer active
- `features/worldmanager/TimerTickHandler.java` Ã— 3 â€” dimension in all messages; chat de-spam at 30s/10s/5s
- `features/worldmanager/PortalEventHandler.java` Ã— 3 â€” removed timer-active travel cancellation
- `ServerManagementModFabric.java` (fabric only) â€” JOIN dispatch for `LoginNotificationHandler.onPlayerLogin`
- `gui/economy/BankMenu.java`, `DailyTasksMenu.java`, `AchievementsMenu.java`, `gui/MotdEditorMenu.java` Ã— 3 â€” `reloadFromClientCache()` method
- `gui/economy/BankScreen.java`, `DailyTasksScreen.java`, `AchievementsScreen.java`, `gui/screen/ConfigScreen.java`, `MotdEditorScreen.java`, `PortalTimerScreen.java` Ã— 3 â€” invoke reload from `init()`

### GUI Scaling Audit — Panel & Slot Overflow at GUI Scale 4 / Auto

- **Bug**: At MC GUI Scale 4 and Scale Auto on 1080p (and on smaller GUI workspaces in general), mod screens overflowed the screen. Most visible on `MineBayScreen`: only the bottom inventory slot rows (4–12) were visible — the offering area, listings panel, and top inventory rows were drawn off-screen. The same shape of bug was present on every mod screen at high scales: the panel rectangle exceeded the workspace, slot Y offsets exceeded the panel, and the panel itself was visibly squashed (aspect ratio destroyed)
- **Root Cause 1 — `ScreenScaler.scale()` non-uniform clamp**: The old `scale(prefW, prefH, screenW, screenH)` clamped the scaled width and height **independently** to `screenW − 20` / `screenH − 20`, producing different X and Y scale factors when the workspace was tight. A 600×400 design panel at a 480×253 workspace (1080p Scale 4) became e.g. 460×233 — width only 92% of preferred but height only 58%, completely destroying the design ratio
- **Root Cause 2 — `MIN_SCALE` floor enforced even when it didn't fit**: `scaleFactor(screenW, screenH)` used `MIN_SCALE = 0.85` as a hard lower bound. At very small workspaces the geometric fit-scale required ~0.44, but the floor pinned the scale at 0.85 anyway, guaranteeing the panel would extend past the workspace
- **Root Cause 3 — absolute slot Y constants in `Menu` constructors**: `MineBayMenu` (offering Y=85, offer slot Y=148, inventory Y=230) and `MineStacksMenu` (bet slot Y=30, inventory Y=140) added their slots at fixed design-space Y coordinates. After the panel was scaled down those Y values still pointed at design-space rows that no longer existed inside the (now smaller) panel, so slots drew below the panel — the user-visible "inventory off screen" symptom
- **Fix — uniform fit-scale**: Rewrote `ScreenScaler` so all sizing decisions go through one uniform scale factor:
  - New `scaleFactor(int prefW, int prefH, int screenW, int screenH)` returns `min(1.0, min(fitScale, max(aestheticScale, AESTHETIC_FLOOR)))` where `fitScale = min((screenW − MARGIN)/prefW, (screenH − MARGIN)/prefH)`. The fit-scale always wins when the workspace is small, and `AESTHETIC_FLOOR = 0.85` only applies when the panel actually fits at that scale — so the panel can shrink as far as it has to instead of overflowing
  - `scale(prefW, prefH, screenW, screenH)` now applies the same scale to both dimensions: `{round(prefW * s), round(prefH * s)}`. No more independent W/H clamps and no more aspect-ratio destruction. The old aesthetic-only `scaleFactor(screenW, screenH)` is preserved for backwards compatibility
  - Added `scale1D(designOffset, prefW, prefH, screenW, screenH)` so menus can scale slot offsets in lock-step with the panel using a one-shot helper
- **Fix — slot Y offsets scale with the panel**: `MineBayMenu` and `MineStacksMenu` now compute a `vScale = ScreenScaler.scaleFactor(prefW, prefH, guiW, guiH)` once in the constructor (via a private `getScaledVerticalFactor(Inventory)` helper that returns `1.0f` server-side where there's no `Window`) and multiply each slot Y constant by it. Because the screens use the same `ScreenScaler.scale(...)` call to size the panel, the slot rows always land inside the panel rectangle at every GUI scale
- **Coverage**: Every mod screen consumes `ScreenScaler.scale(prefW, prefH, this.width, this.height)` to size its `imageWidth`/`imageHeight`, so all 17 mod screens automatically inherit the uniform fit-scale (Dashboard, MineBay, DailyTasks, Achievements, Bank, Config, EconomyManagement, Console, PerformanceSettings, WorldDetail, MotdEditor, PlayerManager, WorldList, PortalTimer, GlobalSettings, ServerManagement, MineStacks). Verified that all three loaders compile clean (`:forge:compileJava :neoforge:compileJava :fabric:compileJava` → BUILD SUCCESSFUL)
- **Files**: `gui/ScreenScaler.java`, `gui/minebay/MineBayMenu.java`, `gui/gambling/MineStacksMenu.java` × 3 loaders (9 files total). Forge ↔ NeoForge copies are byte-identical after the rewrite; Fabric copies of the menus differ only in their `MenuType` registration signature (`super(ModMenuTypes.X_MENU, …)` without `.get()`)
- **Phase 2 (deferred)**: The 17 screens themselves still use absolute internal Y offsets (`centerY + 60`, `+= 35`, etc.) for buttons/labels. Phase 1 keeps the panel and inventory slots inside the workspace at every GUI scale. At the smallest workspaces (1080p Scale 4 ≈ 480×253 GUI units) a few internal widgets may still visually crowd. A follow-up pass can either apply per-screen Y-offset scaling using `ScreenScaler.scale1D(...)` or wrap `render()` in a `pose.scale(s,s,1f)` matrix (with mouse-coord transforms) via a new `ScaledContainerScreen` base class

### Phase 2.4 — Spectate flow, world-detail sync, fabric daily-tasks, dashboard-card cutoff

Five user-reported bugs against the Phase 2.3 baseline. All fixes applied symmetrically across loaders where the bug existed.

- **Spectate self-block (forge / neoforge)**: `/playermanager spectate <self>` skipped the early-return guard in `PlayerManagerSingleton.spectatePlayer` because only the fabric copy had it. Added the missing `if (spectator.getUUID().equals(target.getUUID())) { sendSystemMessage("§cYou cannot spectate yourself!"); return; }` to the forge and neoforge copies right after the `target == null` check
- **Spectate duplicate "Now spectating …" chat (3 loaders)**: `ModCommands` unconditionally called `source.sendSuccess(() -> Component.literal("Now spectating " + targetName), false)` *after* `PlayerManagerSingleton.spectatePlayer(...)`. Since `spectatePlayer` already sends its own `§a`/`§c` chat result via `sendSystemMessage`, every successful spectate produced two chat messages, and on fabric a cancelled self-spectate still printed the bogus success line. Removed the command-level `sendSuccess` call (replaced with an explanatory comment) in all three loaders — chat now reflects only the singleton's authoritative result
- **WorldDetailScreen — portal timer execution didn't update GUI immediately (3 loaders)**: `SyncWorldDetailPacket.handle` updated the client cache via `ClientPacketHandler.handleWorldDetail` but never triggered a screen rebuild, so the live "Time left: M:SS" line, the EditBox lock state, and the Set-Timer button hide/show didn't refresh until the next `init()` (re-open the screen). Now after `handleWorldDetail` the handler checks `Minecraft.getInstance().screen instanceof WorldDetailScreen wds` and calls `wds.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight())` to force `init()` to re-run with the fresh cache values
- **Fabric — Daily Task action tracking did nothing**: `DailyTaskProgressListener` (block break, mob kill, item craft) and `PlayerMovementTracker` had static handler methods but **zero Fabric event bindings** — the forge/neoforge copies are wired via `@SubscribeEvent`, but the fabric init class had never registered the matching API callbacks. Added in `ServerManagementModFabric.onInitialize`:
  - `ServerTickEvents.END_SERVER_TICK` → `PlayerMovementTracker.onPlayerTick(server)` (per-tick distance accumulator)
  - `PlayerBlockBreakEvents.AFTER` → `DailyTaskProgressListener.onBlockBreak(level, player, pos, state)`
  - `ServerLivingEntityEvents.AFTER_DEATH` (from `net.fabricmc.fabric.api.entity.event.v1` — note the **entity** package, not `lifecycle`) → `DailyTaskProgressListener.onEntityKilled(entity, source)`
  - New `ResultSlotMixin` (`@Inject` at HEAD of `ResultSlot.onTake`) → `DailyTaskProgressListener.onItemCrafted(player, stack)`. There is no Fabric API event for crafting, so a mixin is required. Registered in `servermanagement.mixins.json` between `PlayerAdvancementsMixin` and `ServerPlayerChangeDimensionMixin`
- **DashboardCard — title and description cutoff at GUI Scale 4 / 5 / Auto (3 loaders)**: The cards rendered inside the `ScalableContainerScreen` pose-stack scale, but vanilla `GuiGraphics.enableScissor` interprets its rect in scaled-screen-space and **does not honor pose transforms**. At small GUI workspaces the design-space scissor box was placed at scaled coordinates and clipped where the user could see card-background paint over the title/description text. The previous truncation used a while-loop that left text reaching the card edge before the scissor cut it. Replaced both title and description truncation with `font.plainSubstrByWidth(text, maxW - font.width(".."))+".."` and tightened the inner safety margin from 6px to 10px (5px each side instead of 3) so text never reaches the scissor box at any scale. Truncation is now defensive instead of relying on the broken scissor

**Files**:
- `features/playermanager/PlayerManagerSingleton.java` × 2 (forge, neoforge — fabric already had the guard)
- `commands/ModCommands.java` × 3
- `network/packet/SyncWorldDetailPacket.java` × 3
- `ServerManagementModFabric.java` (fabric only)
- `mixin/ResultSlotMixin.java` (fabric only — new file)
- `servermanagement.mixins.json` (fabric only — registered ResultSlotMixin)
- `gui/widgets/DashboardCard.java` × 3

Verified clean compile across all three loaders (`:forge:compileJava :neoforge:compileJava :fabric:compileJava` → `BUILD SUCCESSFUL`).


### Phase 2.5 — Fabric daily-task live sync, MineBay edit-back navigation, dashboard-card cutoff (round 2) (April 26, 2026)

Three regressions / persistent bugs reported against the Phase 2.4 baseline.

- **Fabric — Daily tasks still not crediting / not visible in GUI (3 loaders)**: Phase 2.4 wired the Fabric event callbacks correctly, but logs from the user's test run showed no DailyTask traces because the listener had no logging, and the GUI never refreshed because progress was only pushed when the player re-opened the screen.
  - DailyTaskProgressListener (forge / neoforge / fabric): added a static pushSyncDailyTasks(ServerPlayer) helper that builds a SyncDailyTasksPacket (tasks, reset time, free-reward flags) from EconomyManager and sends it via ModNetworking.sendToPlayer. Called at the END of all four handlers (onBlockBreak, onItemCrafted, onEntityKilled, onVillagerTrade) so the open Daily Tasks GUI updates live as soon as progress is recorded
  - PlayerMovementTracker (3 loaders): same live-push call after the existing if (completedTask != null) notification block, so TRAVEL_DISTANCE progress also pushes a sync packet
  - ServerManagementModFabric (fabric only): added AtomicBoolean first-fire INFO logs to PlayerBlockBreakEvents.AFTER and ServerLivingEntityEvents.AFTER_DEATH so the server log unambiguously confirms the events are firing
  - ResultSlotMixin (fabric only): added a one-shot servermanagement INFO log on first crafting hook fire for the same diagnostic visibility
- **MineBay EditListing — Back button must not return to the place-item step (3 loaders)**: initCreateStep2's Back button was hardcoded to switchState(CREATE_STEP1) regardless of edit mode, which dropped the player into the create-listing item-placement screen they never came from. Now:
  - When `isEditMode == false`, behaviour is unchanged — Back goes to CREATE_STEP1
  - When `isEditMode == true`, the Back button checks `hasUnsavedEdits()` (compares selectedOfferType, money price box text, margin percent box text, and each of the 3 priceItems[] entries — itemstack identity via ItemStack.isSameItemSameComponents, amount, and useStacks — against the original listingBeingEdited). With no changes, edit state clears and the screen returns to editOriginState (the screen the player was on when they pressed "Edit", typically VIEW_MY_LISTINGS or VIEW_DETAILS). With changes, a new EDIT_DISCARD_CONFIRM modal opens with two buttons — "Discard Changes" (clears edit state and returns to origin) and "Keep Editing" (returns to CREATE_STEP2 with the form values intact)
  - Added editOriginState field captured in editListing(...) (defensively reset to BROWSE if the source was somehow inside the create flow)
  - New ScreenState.EDIT_DISCARD_CONFIRM, new initEditDiscardConfirm and enderEditDiscardConfirm (yellow-border modal mirroring the delete-confirm style), and corresponding cases in the init() and ender() switches
- **DashboardCard — title and description still cut off at GUI Scale 4 / 5 / Auto (3 loaders)**: Phase 2.4's "wider truncation + 10px margin" was insufficient; the user supplied 5 screenshots showing the card border still painted over the text. Root cause is unchanged (GuiGraphics.enableScissor ignores the parent ScalableContainerScreen pose scale), so the fix doubles down on defensive truncation:
  - Padding raised from 5px to **16px each side** (32px total)
  - .. (two-char) ellipsis replaced with … (single-char) to free another pixel of budget
  - Defensive second-pass while loop trims one character at a time until `font.width(text) <= maxW`, so glyph-width rounding from plainSubstrByWidth can never push the result over budget

**Files**:
- eatures/economy/DailyTaskProgressListener.java × 3
- eatures/economy/PlayerMovementTracker.java × 3
- ServerManagementModFabric.java (fabric only)
- mixin/ResultSlotMixin.java (fabric only)
- gui/minebay/MineBayScreen.java × 3
- gui/widgets/DashboardCard.java × 3

Verified clean compile across all three loaders (`.\gradlew :forge:compileJava :neoforge:compileJava :fabric:compileJava` → `BUILD SUCCESSFUL`).
