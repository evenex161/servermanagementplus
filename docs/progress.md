# Progress Log

## Fabric port bug-fix session

See `session_summary.md` for the full report. Headlines:

- Wired all missing Fabric event registrations in
  `ServerManagementModFabric.onInitialize()` (server tick, player join/leave,
  dimension change, chat, use-block).
- Fixed `/bank` crash by switching `BANK_MENU` to `registerSimple` (it was
  registered as `ExtendedScreenHandlerType` despite `BankMenuProvider` being
  a plain `MenuProvider`). Deleted orphan duplicate `gui/BankMenu.java`.
- Added self-spectate / self-view-inventory rejection in
  `PlayerManagerSingleton`.
- Fixed mojibake `Â§c` → `§c` in `PortalEventHandler` (six lines).
- Hardened all packet receivers (server + client) by wrapping in
  `server.execute(...)` / `client.execute(...)` to keep handlers off the
  netty thread.
- Added Mixin infrastructure (`servermanagement.mixins.json`,
  `mixin/NoteBlockMixin.java`) and refactored `SlimeHeadManager` to expose
  `tryPlaySlimeSound(Level, BlockPos) → boolean` so the slime-head
  note-block sound now works on Fabric.
- Portal travel for non-OPs blocked via combination of `UseBlockCallback`
  (ignition) + `AFTER_PLAYER_CHANGE_WORLD` teleport-back fallback (no
  pre-travel cancel event in Fabric API).

Build verified: `.\gradlew :fabric:build` → BUILD SUCCESSFUL.

Bug 7 (item-count rendering) cause not pinned from static analysis. Wrapped
client packet handlers as a probable mitigation; needs runtime confirmation.

## WorldDetailScreen crash fix (Fabric runtime)

**Symptom**: Player2 client crashed with ReportedException: Rendering screen ? StringIndexOutOfBoundsException: Range [0, 1) out of bounds for length 0 at WorldDetailScreen.java:228 after clicking a world entry in the World Manager. Server stayed up; client window closed. Earlier `open_gui_packet` attempts (1-5) likely opened other screens (World List); the 6th was the first WORLD_DETAIL click.

**Root cause**: Race between two server?client packets sent back-to-back in `OpenGuiPacket.handle()`:
1. `ServerPlayNetworking.send(SyncWorldDetailPacket)` populates `ClientPacketHandler.cachedDimensionId`
2. `player.openMenu(WorldDetailMenuProvider)` triggers vanilla `ClientboundOpenScreen`

Under Fabric, `ClientPlayNetworking` receivers fire on the netty thread and re-schedule via `client.execute()`. That extra hop puts our cache update behind vanilla's openScreen handler in the render-thread queue. The screen is constructed with `dimensionId = """" and `render()` calls `"".substring(0, 1)` ? crash. Forge does not exhibit this because its packet plumbing dispatches via `enqueueWork` from the same point where vanilla schedules the open-screen task.

**Fix** (`fabric/src/main/java/com/servermanagement/gui/screen/`):
- `WorldDetailScreen`: refresh `dimensionId` from `ClientPacketHandler` at the top of `render()` and call `rebuildWidgets()` once it becomes available; guard the title `substring` against empty (shows "Loading..." for one frame).
- `PortalTimerScreen`: same lazy-refresh pattern so the portal-type toggles reflect the correct dimension when the sync packet arrives late.
- Constructors now explicitly null-check the cache value.

**Verification**: `.\gradlew :fabric:compileJava` ? BUILD SUCCESSFUL.

**Memory updated**: Added an "S2C ordering race" pitfall to `/memories/repo/servermanagement-forge-conventions.md` so future Fabric screens are written defensively.


---

# Fabric Runtime Bug Sweep (5 issues)

Date: April 22, 2026

## Summary

Five user-reported issues on the Fabric build were all caused by the same
class of porting gap: Forge auto-wires handlers via `@SubscribeEvent` on its
event bus, but Fabric requires explicit registration with the appropriate
callback (or a Mixin where Fabric API exposes nothing). After the
MultiLoader migration the handler classes were copied verbatim but several
were left orphaned. One additional issue is a packet-ordering race specific
to Fabric's `client.execute()` wrap on `ClientPlayNetworking` receivers.

## Issues, root cause and fix

### 1. Portal activation/deactivation does not work

* **Root cause**: Forge's `EntityTravelToDimensionEvent` fires *before* the
  player moves and is cancellable. Fabric only exposes
  `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD`, which fires
  after travel is complete — so the previous "teleport-back" workaround
  in [`ServerManagementModFabric.java`](fabric/src/main/java/com/servermanagement/ServerManagementModFabric.java)
  appeared to do nothing because the player had already left the dimension.
* **Fix**: New
  [`EntityChangeDimensionMixin`](fabric/src/main/java/com/servermanagement/mixin/EntityChangeDimensionMixin.java)
  injects at the `HEAD` of `Entity#changeDimension(DimensionTransition)`
  and short-circuits with the existing
  [`PortalEventHandler.onEntityTravelToDimension`](fabric/src/main/java/com/servermanagement/features/worldmanager/PortalEventHandler.java)
  check, returning the entity unchanged when travel is denied. Removed the
  obsolete teleport-back workaround.

### 2. Portal Timers do not tick

* **Root cause**:
  [`TimerTickHandler.onServerTick`](fabric/src/main/java/com/servermanagement/features/worldmanager/TimerTickHandler.java)
  is the same per-second timer driver as on Forge, but it was never
  invoked from any `ServerTickEvents.END_SERVER_TICK` listener.
* **Fix**: Added the call inside the existing tick lambda in
  [`ServerManagementModFabric.java`](fabric/src/main/java/com/servermanagement/ServerManagementModFabric.java)
  next to the other tick handlers, with try/catch parity.

### 3. Achievement rewards never paid out / Achievements screen empty

* **Root cause**: Fabric API has no public advancement-grant event, and
  [`AchievementRewardListener.onAdvancementEarned`](fabric/src/main/java/com/servermanagement/features/economy/AchievementRewardListener.java)
  was never wired up. Server-side
  `EconomyManager.getAchievementTracker()` therefore stayed empty, so
  every `SyncAchievementsPacket` shipped an empty set and the screen
  rendered "No achievements earned yet".
* **Fix**: New
  [`PlayerAdvancementsMixin`](fabric/src/main/java/com/servermanagement/mixin/PlayerAdvancementsMixin.java)
  injects at `RETURN` of
  `PlayerAdvancements#award(AdvancementHolder, String)` and forwards to
  the existing listener when the post-award progress is `isDone()`
  (matching Forge's "advancement just completed" semantic).

### 4. Item-price tooltips missing in vanilla and MineBay

* **Root cause**: Forge fires `ItemTooltipEvent` on the global event bus
  and `@SubscribeEvent` auto-registers it. Fabric requires an explicit
  `ItemTooltipCallback.EVENT.register(...)` — that registration was
  missing from
  [`ClientSetup`](fabric/src/main/java/com/servermanagement/client/ClientSetup.java),
  so [`ClientItemTooltipHandler.onItemTooltip`](fabric/src/main/java/com/servermanagement/client/ClientItemTooltipHandler.java)
  was dead code.
* **Fix**: Register the callback in `ClientSetup#onInitializeClient`,
  forwarding the third argument (`TooltipFlag tooltipType`) to the
  handler. The 1.21.1 callback signature is
  `(ItemStack, Item.TooltipContext, TooltipFlag, List<Component>)`.

### 5. GUI screens show stale state until reopened

* **Root cause**: All `ClientPlayNetworking` receivers in
  [`ModNetworking`](fabric/src/main/java/com/servermanagement/network/ModNetworking.java)
  are wrapped as `(payload, ctx) -> ctx.client().execute(() -> payload.handle(null))`.
  When the server sends `Sync*Packet` immediately followed by
  `player.openMenu(...)`, the vanilla `ClientboundOpenScreen` packet can
  schedule its render-thread task *before* our wrapped handler runs.
  Screens that snapshot the cache in their constructor or `init()` then
  display the previous values until reopened.
* **Fix**: Added
  [`ClientPacketHandler.refreshOpenScreen()`](fabric/src/main/java/com/servermanagement/client/ClientPacketHandler.java),
  which calls `init()` again on the currently-open
  `com.servermanagement.*` screen so widgets re-read fresh cache values.
  The helper is invoked at the end of every cache-mutating handler:
  `handleMotdSync`, `handleWorldList`, `handleWorldDetail`,
  `handleGlobalSettings`, `handleEconomyTemplates`, `handleEconomyStats`,
  `handlePerformanceSettings`, plus the inline cache writes in
  `SyncBankAccountPacket`, `SyncBankInventoryPacket`,
  `SyncDailyTasksPacket`, `SyncAchievementsPacket`,
  `SyncMoneyRequestsPacket`, `SyncMarketPricesPacket`,
  `SyncFeatureStatesPacket`, `SyncGamblingStatsPacket`,
  `SyncMineBayListingsPacket`, `PMSyncPlayerListsPacket`. The helper
  guards by class-name prefix so vanilla/third-party screens are never
  rebuilt.

## Files changed

### Added
* `fabric/src/main/java/com/servermanagement/mixin/EntityChangeDimensionMixin.java`
* `fabric/src/main/java/com/servermanagement/mixin/PlayerAdvancementsMixin.java`

### Modified
* `fabric/src/main/resources/servermanagement.mixins.json` — registered both
  new mixins.
* `fabric/src/main/java/com/servermanagement/ServerManagementModFabric.java` —
  wired `TimerTickHandler.onServerTick`, removed obsolete
  `AFTER_PLAYER_CHANGE_WORLD` teleport-back.
* `fabric/src/main/java/com/servermanagement/client/ClientSetup.java` —
  registered `ItemTooltipCallback`.
* `fabric/src/main/java/com/servermanagement/client/ClientPacketHandler.java`
  — added `refreshOpenScreen()` and called it at the end of every cache
  mutator.
* `fabric/src/main/java/com/servermanagement/network/packet/SyncBankAccountPacket.java`,
  `SyncBankInventoryPacket.java`, `SyncDailyTasksPacket.java`,
  `SyncAchievementsPacket.java`, `SyncMoneyRequestsPacket.java`,
  `SyncMarketPricesPacket.java`, `SyncFeatureStatesPacket.java`,
  `SyncGamblingStatsPacket.java`, `PMSyncPlayerListsPacket.java`,
  `minebay/SyncMineBayListingsPacket.java` — appended
  `ClientPacketHandler.refreshOpenScreen()` after their inline cache writes.

## Build

`.\gradlew :fabric:build` — **BUILD SUCCESSFUL** (3m 11s).

## Runtime verification

Pending: relaunch `test.bat debug fabric`, light a portal, watch a portal
timer count down, earn an achievement, hover items in inventory and in the
MineBay screen, and toggle a setting in Global Settings or World Detail
without reopening to confirm the live update.

## Fabric follow-up fixes (prices, portal travel, achievements)

Date: April 22, 2026

- Root-caused incorrect Fabric pricing display to packet direction wiring:
  `SyncMarketPricesPacket` was registered as C2S and never as S2C receiver,
  so clients did not refresh `ClientMarketData`.
- Fixed Fabric networking wiring in
  `fabric/src/main/java/com/servermanagement/network/ModNetworking.java`:
  removed C2S registration + server receiver for `SyncMarketPricesPacket`,
  added S2C registration + client receiver.
- Hardened portal blocking by adding
  `fabric/src/main/java/com/servermanagement/mixin/ServerPlayerChangeDimensionMixin.java`
  and registering it in `fabric/src/main/resources/servermanagement.mixins.json`.
  This enforces restrictions on the concrete `ServerPlayer#changeDimension`
  path in addition to the existing `EntityChangeDimensionMixin`.
- Stabilized achievement reward hook timing in
  `fabric/src/main/java/com/servermanagement/mixin/PlayerAdvancementsMixin.java`:
  captures pre-award done state and performs done-check on server execute queue
  to avoid return-order timing races.

Build status:
- `:fabric:compileJava` currently blocked by external file lock on
  `%USERPROFILE%/.gradle/caches/fabric-loom/.../mappings.jar`
  (`FileSystemException: file is in use by another process`).
  Source-level diagnostics for edited files report no Java/resource errors.

## Stale-cache audit + portal-timer UX + Fabric inventory tooltips (2026-04-22)

Three issues fixed across forge/neoforge/fabric (all 3 loaders compile clean).

### Issue 1 — Fabric inventory tooltip prices not synced
`ServerManagementModFabric.onInitialize()` JOIN handler now also dispatches
`LoginNotificationHandler.onPlayerLogin(p)` (with try/catch). This causes
`EconomyManager.syncMarketPrices(player)` to run on join, populating client
`recipePrices` so `ClientMarketData.getBasePrice()` uses recipe-derived
prices instead of vanilla `ItemValuation` rarity fallback. Forge/NeoForge
already triggered this via `@SubscribeEvent` PlayerLoggedInEvent — Fabric
needs explicit dispatch.

### Issue 2 — Stale client cache on screen reconstruction (broad audit)
Every `Sync*Packet.handle` calls `ClientPacketHandler.refreshOpenScreen()`
which re-invokes `screen.init()`. Fix only worked when `init()` re-read
cache. Audited all screens latching cache snapshots in their constructors.

Added `reloadFromClientCache()` method to 4 menus × 3 loaders (12 files):

- `BankMenu` — re-pulls balance + transactions
- `DailyTasksMenu` — re-pulls task list + reset time
- `AchievementsMenu` — re-pulls earned achievements + reward total
- `MotdEditorMenu` — re-pulls MOTD text

Each menu's constructor now delegates to the new method. Then 6 screens × 3
loaders (18 files) call the reload from `init()`:

- `ConfigScreen` — calls `menu.refreshStates()` (already existed)
- `BankScreen` — calls `menu.reloadFromClientCache()`
- `DailyTasksScreen` — calls `menu.reloadFromClientCache()`
- `AchievementsScreen` — calls `menu.reloadFromClientCache()` AND rebuilds
  its private `achievementsList` field from the refreshed menu set
- `MotdEditorScreen` — guarded `if (originalMotdText == null)` to avoid
  clobbering pending edits
- `PortalTimerScreen` — re-reads `ClientPacketHandler.getCachedDimensionId()`
  in init() (mirror of `WorldDetailScreen` lines 60-65 fix)

### Issue 3 — Portal timer countdown UX
- `WMSetTimerPacket` (3 loaders) — on success now constructs and sends a
  fresh `SyncWorldDetailPacket` to the originating player, so the live
  countdown line in WorldDetailScreen appears immediately after starting a
  timer instead of requiring a screen reopen.
- `WorldDetailScreen` (3 loaders) — when `hasTimer == true`, the seconds
  EditBox is now: cleared, given hint text "running", and `setEditable(false)`.
  The "Set Timer" button is no longer added while a timer is running. Prevents
  the confusing "EditBox value counts down" behavior on every refresh.

### Files touched (33)
- 3× `WMSetTimerPacket.java`
- 3× `WorldDetailScreen.java`
- 3× `ServerManagementModFabric.java` (1 file actually — fabric only)
- 12× menus: `BankMenu` `DailyTasksMenu` `AchievementsMenu` `MotdEditorMenu` × 3
- 18× screens: `ConfigScreen` `BankScreen` `DailyTasksScreen` `AchievementsScreen` `MotdEditorScreen` `PortalTimerScreen` × 3

### Validation
`.\gradlew :forge:compileJava :neoforge:compileJava :fabric:compileJava` →
BUILD SUCCESSFUL (only pre-existing deprecation notes for `MotdEditorScreen`
and `CurseForgeUpdateChecker`).


---

# Session Summary — Stale-cache audit + portal-timer UX + Fabric inventory tooltips

Three issues fixed across forge / neoforge / fabric (all 3 loaders compile clean).

## Issue 1 — Fabric inventory tooltip prices not synced to Economy Engine

**Symptom (Fabric only):** Inventory tooltip item prices and stack prices were
not synced to the actual Economy Engine's calculated prices with the crafting
recipe logic integrated. MineBay tooltips worked correctly because they were
eventually corrected by `EconomyServerHandler.onServerTick`'s 2-minute
`syncMarketPricesToAll()`.

**Root cause:** Fabric's `ServerManagementModFabric.onInitialize()` JOIN
handler had no dispatch for `LoginNotificationHandler.onPlayerLogin(player)`,
so `EconomyManager.syncMarketPrices(player)` never ran on join. The client's
`ClientMarketData.recipePrices` map stayed empty, and `getBasePrice()` fell
back to `ItemValuation.getItemValue(...)` (vanilla rarity) instead of
recipe-derived prices. Forge/NeoForge already fired this via the
`@SubscribeEvent` `PlayerLoggedInEvent` event, but Fabric needs explicit
dispatch.

**Fix:** Added `com.servermanagement.features.economy.notifications.LoginNotificationHandler.onPlayerLogin(p)`
to the Fabric JOIN handler (with try/catch wrapper).

## Issue 2 — Stale client cache on screen reconstruction (broad audit)

**Symptom:** Screen contents stayed stale until the user closed and reopened
the GUI, even though `Sync*Packet.handle` calls
`ClientPacketHandler.refreshOpenScreen()` which re-invokes `screen.init()`.

**Root cause:** Many menus latched cache snapshots in their constructors, and
the matching screens never re-read those values in `init()`. So
`refreshOpenScreen()` did re-run `init()`, but the menu fields it relied on
were already-stale ctor-time copies.

**Fix:** Added a `reloadFromClientCache()` method to 4 menus × 3 loaders
(12 files):

- `BankMenu` — re-pulls balance + transactions
- `DailyTasksMenu` — re-pulls task list + reset time
- `AchievementsMenu` — re-pulls earned achievements + reward total
- `MotdEditorMenu` — re-pulls MOTD text

Each menu's constructor now delegates to the new method. Then 6 screens × 3
loaders (18 files) call the reload from `init()`:

- `ConfigScreen` — calls `menu.refreshStates()` (method already existed)
- `BankScreen` — calls `menu.reloadFromClientCache()`
- `DailyTasksScreen` — calls `menu.reloadFromClientCache()`
- `AchievementsScreen` — calls `menu.reloadFromClientCache()` AND rebuilds
  its private `achievementsList` field from the refreshed menu set
- `MotdEditorScreen` — guarded by `if (originalMotdText == null)` to avoid
  clobbering pending edits
- `PortalTimerScreen` — re-reads `ClientPacketHandler.getCachedDimensionId()`
  in `init()` (mirror of the existing `WorldDetailScreen` fix)

## Issue 3 — Portal timer countdown UX

**Symptom A:** The new portal-timer countdown line in the world details GUI
did not appear immediately after starting a timer; the player had to reopen
the screen for it to show up.

**Root cause A:** `WMSetTimerPacket` mutated server-side state but never
pushed a fresh `SyncWorldDetailPacket` back to the originating player. The
client only refreshed its cache on the next periodic sync.

**Fix A:** `WMSetTimerPacket` (3 loaders) — on success now constructs and
sends a fresh `SyncWorldDetailPacket(dimensionId, areNetherPortalsEnabled,
areEndPortalsEnabled, hasActiveTimer, (int)getRemainingTime,
isDimensionChatConnected, getTimerPortalType)` to the originating player via
`ModNetworking.sendToPlayer(packet, player)`. The countdown now appears in
real time without reopening the screen.

**Symptom B:** While a portal timer was running, the seconds EditBox kept
counting down on every screen refresh — confusing because the field looked
editable while actually being overwritten by sync packets.

**Root cause B:** `init()` unconditionally called `setValue(hasTimer ?
String.valueOf(timerSeconds) : "60")`, so each refresh stamped the
remaining-seconds value into the EditBox.

**Fix B:** `WorldDetailScreen.init()` (3 loaders) — when `hasTimer == true`,
the seconds EditBox is now: cleared (`setValue("")`), given hint text
"running" (`setHint(Component.literal("running"))`), and locked
(`setEditable(false)`). The "Set Timer" button is no longer added while a
timer is running. The "Clear" button stays available.

## Files changed (33 total)

- `WMSetTimerPacket.java` × 3 (forge/neoforge/fabric)
- `WorldDetailScreen.java` × 3
- `ServerManagementModFabric.java` × 1 (fabric only)
- Menus × 12: `BankMenu`, `DailyTasksMenu`, `AchievementsMenu`, `MotdEditorMenu` × 3 loaders
- Screens × 18: `ConfigScreen`, `BankScreen`, `DailyTasksScreen`,
  `AchievementsScreen`, `MotdEditorScreen`, `PortalTimerScreen` × 3 loaders

## Validation

```
.\gradlew :forge:compileJava :neoforge:compileJava :fabric:compileJava --no-daemon
```

Result: BUILD SUCCESSFUL. Only pre-existing deprecation notes for
`MotdEditorScreen.java` and `CurseForgeUpdateChecker.java`.



---

# Session Summary — Portal timer dimension awareness, chat de-spam, and travel-window fix

Three portal-timer issues fixed across forge / neoforge / fabric (all 3 loaders compile clean).

## Issue 1 — Broadcast/chat messages didn't mention the dimension

**Symptom:** When a portal timer ran on, say, Overworld → Nether portals, every
player on the server received "Nether portals close in 1 minute" with no
dimension context. Players in The Nether or modded dimensions had no way to
tell whether their own portals were affected.

**Root cause:** `WorldManager.getPortalDescription(dimensionId, portalType)`
only returns a portal-type label like "Nether portals" or "Portals to the
Overworld". The TimerTickHandler call sites assembled messages from that
label alone, never interpolating the dimension name.

**Fix:** `TimerTickHandler.sendTimerWarnings()` and `handleTimerComplete()` (× 3
loaders) now also call `WorldManager.getDimensionName(dimensionId)` and
include " in <dimName>" in:

- The 60s chat heads-up
- Every subtitle line (60s / 30s / 10s / 5s / 4-1s / completion)
- The completion chat message

Example before: `§e⚠ Nether portals close in 1 minute`
Example after:  `§e⚠ Nether portals in Overworld close in 1 minute`

The 5-second / 4-1s subtitles also now read `Nether portals in Overworld`
instead of the prior generic `...` placeholder, so the title bar always tells
players which world the timer is for.

## Issue 2 — Chat spam from per-interval announcements

**Symptom:** Each warning interval (60s, 30s, 10s, 5s) produced a chat line +
title + sound, on top of the per-second 4-1s title ticks. With four players
online this was 4 chat lines per warning fan-out, repeated every second in the
last 5 seconds. Players asked for less chat noise.

**Root cause:** `sendTimerWarnings` unconditionally built a `Component
warning` for the 30s, 10s, and 5s intervals.

**Fix:** Dropped chat (`warning = ...`) at 30s, 10s, and 5s. Those intervals
are now title + subtitle + sound only — still highly visible to every player
via the title bar but with zero chat clutter. Kept:

- 60s chat heads-up (the "you have a minute" alert)
- Completion chat message (the "result")

Net effect: 5 chat lines per timer reduced to 2.

## Issue 3 — Timer cancelled all portal travel during the countdown

**Symptom:** While a portal timer was running, players couldn't travel through
the portal at all. This defeated the purpose of the announced "you have N
seconds" warning — there was nothing players could do with that warning.

**Root cause:** `PortalEventHandler.onEntityTravelToDimension` had a block
that always cancelled travel when `hasActiveTimer && remainingTime > 0`,
regardless of which direction the timer was transitioning.

Analysis of the two timer directions:

- **enabled → disabled:** the portal state hasn't actually flipped yet; it
  is still enabled. The cancel was the *only* thing blocking travel — and it
  did so while loudly announcing "you have time to escape!", which was a
  contradiction.
- **disabled → enabled:** the portal state hasn't flipped yet; it is still
  disabled. The upstream `if (!portalAllowed)` check already cancels travel
  with a clean, dimension-aware "X portals are disabled in <dim>!"
  message. The timer-active block was redundant.

**Fix:** Removed the timer-active travel cancellation block from
`PortalEventHandler.onEntityTravelToDimension` (× 3 loaders). Travel during
an enabled→disabled countdown now works, giving players the actual escape
window the warnings promised. Travel during a disabled→enabled countdown is
still correctly blocked by the existing `!portalAllowed` branch.

Manual portal frame ignition / portal block placement during a timer is
unaffected: those checks only consult the current portal-enabled state, which
during enabled→disabled is still "true", so a player with flint and steel can
still light a Nether portal during the countdown — consistent with the new
"escape window" behavior.

## Files changed (6)

- `features/worldmanager/TimerTickHandler.java` × 3 (forge / neoforge / fabric)
  - `sendTimerWarnings()` body rewritten — adds dimension to subtitles and
    60s chat; drops chat at 30s/10s/5s
  - `handleTimerComplete()` — adds dimension to chat message + subtitle
- `features/worldmanager/PortalEventHandler.java` × 3
  - Removed the `if (worldData.hasActiveTimer(dimensionId)) { ... }` travel
    cancel block; replaced with a documenting comment explaining why both
    timer directions are correctly handled by surrounding logic

`WorldManager.java` (which owns `getDimensionName` / `getPortalDescription`)
needed no changes — both helpers were already public and dimension-aware.

## Validation

```
.\gradlew :forge:compileJava :neoforge:compileJava :fabric:compileJava
```

Result: BUILD SUCCESSFUL.
# Session Summary — Stale-cache audit + portal-timer UX + Fabric inventory tooltips

Three issues fixed across forge / neoforge / fabric (all 3 loaders compile clean).

## Issue 1 — Fabric inventory tooltip prices not synced to Economy Engine

**Symptom (Fabric only):** Inventory tooltip item prices and stack prices were
not synced to the actual Economy Engine's calculated prices with the crafting
recipe logic integrated. MineBay tooltips worked correctly because they were
eventually corrected by `EconomyServerHandler.onServerTick`'s 2-minute
`syncMarketPricesToAll()`.

**Root cause:** Fabric's `ServerManagementModFabric.onInitialize()` JOIN
handler had no dispatch for `LoginNotificationHandler.onPlayerLogin(player)`,
so `EconomyManager.syncMarketPrices(player)` never ran on join. The client's
`ClientMarketData.recipePrices` map stayed empty, and `getBasePrice()` fell
back to `ItemValuation.getItemValue(...)` (vanilla rarity) instead of
recipe-derived prices. Forge/NeoForge already fired this via the
`@SubscribeEvent` `PlayerLoggedInEvent` event, but Fabric needs explicit
dispatch.

**Fix:** Added `com.servermanagement.features.economy.notifications.LoginNotificationHandler.onPlayerLogin(p)`
to the Fabric JOIN handler (with try/catch wrapper).

## Issue 2 — Stale client cache on screen reconstruction (broad audit)

**Symptom:** Screen contents stayed stale until the user closed and reopened
the GUI, even though `Sync*Packet.handle` calls
`ClientPacketHandler.refreshOpenScreen()` which re-invokes `screen.init()`.

**Root cause:** Many menus latched cache snapshots in their constructors, and
the matching screens never re-read those values in `init()`. So
`refreshOpenScreen()` did re-run `init()`, but the menu fields it relied on
were already-stale ctor-time copies.

**Fix:** Added a `reloadFromClientCache()` method to 4 menus × 3 loaders
(12 files):

- `BankMenu` — re-pulls balance + transactions
- `DailyTasksMenu` — re-pulls task list + reset time
- `AchievementsMenu` — re-pulls earned achievements + reward total
- `MotdEditorMenu` — re-pulls MOTD text

Each menu's constructor now delegates to the new method. Then 6 screens × 3
loaders (18 files) call the reload from `init()`:

- `ConfigScreen` — calls `menu.refreshStates()` (method already existed)
- `BankScreen` — calls `menu.reloadFromClientCache()`
- `DailyTasksScreen` — calls `menu.reloadFromClientCache()`
- `AchievementsScreen` — calls `menu.reloadFromClientCache()` AND rebuilds
  its private `achievementsList` field from the refreshed menu set
- `MotdEditorScreen` — guarded by `if (originalMotdText == null)` to avoid
  clobbering pending edits
- `PortalTimerScreen` — re-reads `ClientPacketHandler.getCachedDimensionId()`
  in `init()` (mirror of the existing `WorldDetailScreen` fix)

## Issue 3 — Portal timer countdown UX

**Symptom A:** The new portal-timer countdown line in the world details GUI
did not appear immediately after starting a timer; the player had to reopen
the screen for it to show up.

**Root cause A:** `WMSetTimerPacket` mutated server-side state but never
pushed a fresh `SyncWorldDetailPacket` back to the originating player. The
client only refreshed its cache on the next periodic sync.

**Fix A:** `WMSetTimerPacket` (3 loaders) — on success now constructs and
sends a fresh `SyncWorldDetailPacket(dimensionId, areNetherPortalsEnabled,
areEndPortalsEnabled, hasActiveTimer, (int)getRemainingTime,
isDimensionChatConnected, getTimerPortalType)` to the originating player via
`ModNetworking.sendToPlayer(packet, player)`. The countdown now appears in
real time without reopening the screen.

**Symptom B:** While a portal timer was running, the seconds EditBox kept
counting down on every screen refresh — confusing because the field looked
editable while actually being overwritten by sync packets.

**Root cause B:** `init()` unconditionally called `setValue(hasTimer ?
String.valueOf(timerSeconds) : "60")`, so each refresh stamped the
remaining-seconds value into the EditBox.

**Fix B:** `WorldDetailScreen.init()` (3 loaders) — when `hasTimer == true`,
the seconds EditBox is now: cleared (`setValue("")`), given hint text
"running" (`setHint(Component.literal("running"))`), and locked
(`setEditable(false)`). The "Set Timer" button is no longer added while a
timer is running. The "Clear" button stays available.

## Files changed (33 total)

- `WMSetTimerPacket.java` × 3 (forge/neoforge/fabric)
- `WorldDetailScreen.java` × 3
- `ServerManagementModFabric.java` × 1 (fabric only)
- Menus × 12: `BankMenu`, `DailyTasksMenu`, `AchievementsMenu`, `MotdEditorMenu` × 3 loaders
- Screens × 18: `ConfigScreen`, `BankScreen`, `DailyTasksScreen`,
  `AchievementsScreen`, `MotdEditorScreen`, `PortalTimerScreen` × 3 loaders

## Validation

```
.\gradlew :forge:compileJava :neoforge:compileJava :fabric:compileJava --no-daemon
```

Result: BUILD SUCCESSFUL. Only pre-existing deprecation notes for
`MotdEditorScreen.java` and `CurseForgeUpdateChecker.java`.

