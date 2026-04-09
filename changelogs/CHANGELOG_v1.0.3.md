# Changelog - v1.0.3 Forge (Help Integration, Security Hardening, Quality & Multi-Loader Support)

**Release Date**: April 5, 2026  
**Previous Version**: v1.0.2  
**Minecraft Version**: 1.20.1 / 1.21.1  
**Mod Loader**: Forge  
**Forge Version**: 47.4.0+ (MC 1.20.1) / 52.1.0+ (MC 1.21.1)  
**OTA Build**: 05 (MC 1.20.1) / 04 (MC 1.21.1)  
**JAR Files**:  
- `servermanagementplus-v1.0.3-b05-mc1.20.1-forge-release.jar`  
- `servermanagementplus-v1.0.3-b04-mc1.21.1-forge-release.jar`

> **NeoForge users**: See [CHANGELOG_v1.0.3-neoforge.md](CHANGELOG_v1.0.3-neoforge.md) for the NeoForge-specific changelog.

---

## 🔢 Build Number Versioning (b04+)

### New Version Naming Scheme
- **Build numbers** added to all version strings: `v1.0.3-bXX-mcX.XX.X-<loader>-release`
- Build numbers (`-bXX`) differentiate incremental updates within the same release version
- **Mod loader identifier** (`forge`, `neoforge`) now embedded in JAR filenames and OTA version strings
- Gradle automatically populates `ota.properties` with version, build, MC version, and mod loader at build time
- `mod_build` and `mod_loader` properties in `gradle.properties` control the build number and loader tag

### Multi-Version & Multi-Loader OTA Support
- **Minecraft version awareness** — OTA version tracking now includes the target MC version
- **Mod loader awareness** — OTA version tracking now includes the mod loader (`forge`, `neoforge`, `fabric`, `quilt`)
- **Cross-version update blocking** — A 1.20.1 client connected to a 1.21.1 server (or vice versa) will not receive an OTA update
- **Cross-loader update blocking** — A Forge client connected to a NeoForge server (or vice versa) will not receive an OTA update
- **MC version and mod loader sent in network packets** — `VersionCheckPacket` carries explicit MC version and mod loader fields
- **CurseForge update checker** reads MC version dynamically from `ota.properties` instead of hardcoding
- **VersionTracker** reads MC version from OTA properties instead of hardcoding
- **`OTAVersion.parseFromString()`** recognizes known mod loaders when parsing version strings

---

## 🔄 Minecraft 1.21.1 Port

### Full Port to Forge 1.21.1
- **Minecraft 1.21.1** support with **Forge 52.1.0** and **ForgeGradle 7** (Gradle 9.3.0, Java 21)
- Complete networking API migration: `NetworkEvent` → `CustomPayloadEvent`, `ChannelBuilder`/`messageBuilder` pattern
- All 56 packet handlers, ItemStack serialization, Advancement API, GUI screens, and NbtIo calls updated
- Player head system updated for `ResolvableProfile` and `DataComponents`
- Three branches: **mc/1.20.1** (Forge), **mc/1.21.1** (Forge), **mc/1.21.1-neoforge** (NeoForge)

---

## 📖 Help Command Integration

- **`/help` command override** — All Server Management commands now appear in vanilla `/help` with full descriptions
- **`/help <command>`** — Detailed usage info for each mod command (e.g., `/help bank` shows all subcommands)
- **Admin filtering** — Admin commands are tagged with `[Admin]` and only visible to players with OP level 2+
- **Non-SM commands** — Vanilla and other mod commands still appear via standard Brigadier fallback

---

## ⚙️ Config-Driven Economy

All hardcoded economy values are now read from `servermanagement-common.toml`:

| New Config Entry | Default | Description |
|---|---|---|
| `startingBalance` | 1000.0 | Money given to new players on first join |
| `encryptedStorage` | true | AES-256-GCM encryption for economy data |
| `transactionHistoryLimit` | 100 | Max transactions stored per player |
| `maxListingsPerPlayer` | 10 | MineBay listing cap per player |
| `teleportCooldown` | 5 | Seconds between world teleports |

### Economy Adjustments
- **Starting balance** now $1,000 (was $0 — new players receive money and a "Starting Balance" transaction record)
- **Transaction history** increased from 50 to 100 entries per player
- **Free daily reward** reduced from $100 to $50 (configurable via task template manager)
- **Free reward amount** is now admin-configurable through the template manager GUI

---

## 🛒 MineBay Marketplace

- **Max listings enforcement** — Players are blocked from creating listings beyond the configured limit (default: 10)
- **User feedback** — Error message with the exact limit shown when a player hits the cap
- **Item safety** — Items are returned to inventory if listing creation is rejected

---

## 🌍 World Manager

- **Teleport cooldown** — Configurable cooldown (default: 5 seconds) between world teleports
- **Cooldown feedback** — Players see remaining cooldown time when attempting to teleport too soon
- **Thread-safe tracking** — Cooldowns stored in ConcurrentHashMap

---

## 🐛 Bug Fixes

- **NPE fix in LoginNotificationHandler** — `player.getServer()` now null-checked before scheduling login notifications, preventing a potential crash on edge-case login scenarios
- **Duplicate variable in ClaimFreeRewardPacket** — Removed duplicate `templateManager` variable declaration that could cause compilation issues
- **Dead code cleanup** — Removed unused `originalId` variable and misleading comment in `DailyTaskTemplateManager.updateTemplate()`

---

## 🧹 Code Quality

- **Removed unused imports** — `com.google.gson.*` from MineBayManager, `TransactionManager` from GamblingManager
- **Final fields** — `bankInventories` in EconomyManager and `playerId` in MineBayListingDraft made `final` for thread safety
- **No wildcard imports** — Cleaned remaining wildcard import violation

---

## � Security Hardening

### Network Buffer Overflow Protection
- **45 unbounded `readUtf()` calls** across 25 packet files now enforce strict length limits via `readUtf(N)` / `writeUtf(N)`:

| Field Type | Max Length | Example Packets |
|---|---|---|
| Player names | 16 chars | BankTransferPacket, PMViewInventoryPacket, SendMoneyRequestPacket |
| Dimension IDs | 256 chars | SyncWorldDetailPacket, WMTogglePortalsPacket, WMSetTimerPacket |
| Portal types | 32 chars | WMTogglePortalsPacket, WMSetTimerPacket, SyncWorldDetailPacket |
| UUID strings (listing/offer IDs) | 36 chars | PurchaseListingPacket, AcceptOfferPacket, RejectOfferPacket |
| Feature IDs / game options | 64 chars | ToggleFeaturePacket, PlaceGamblingBetPacket, GamblingTensionPacket |
| Version strings | 64 chars | ModFileRequestPacket, ModFileCompletePacket, VersionCheckPacket |
| Hash strings | 128 chars | ModFileChunkPacket, ModFileCompletePacket, VersionCheckPacket |
| Messages / descriptions | 256 chars | GamblingResultPacket, SendMoneyRequestPacket, SyncDailyTasksPacket |

This prevents a malicious client from sending multi-megabyte strings in a single packet to exhaust server memory.

### Input Validation
- **Player name regex validation** — PMViewInventoryPacket and PMSpectatePlayerPacket now reject player names that don't match `[a-zA-Z0-9_]{1,16}` before attempting any lookup
- **Log injection prevention** — ConsoleCommandPacket strips `\n` and `\r` from command strings before logging to prevent log forging attacks

### Race Condition Fix
- **MineBayManager.createListing()** — Now `synchronized` to prevent a TOCTOU (time-of-check-time-of-use) race where two rapid requests could bypass the max-listings-per-player limit

---

## ⚡ Performance Improvements

### Chat Isolation — O(n) → O(1) Lookups
- Replaced `ArrayList.contains()` with `HashSet` for allowed dimension checks in ChatIsolationHandler, reducing per-player lookup cost from O(n) to O(1) during chat broadcasts

### Tab List Isolation — Reduced GC Pressure
- Reused a static `Map<String, List<ServerPlayer>>` instead of allocating a new HashMap + ArrayLists every second (20-tick cycle)
- Added `HashSet<UUID>` for O(1) duplicate detection when adding operators to the visible player list (was O(n) `List.contains()`)

### Timer Tick Handler — Early Exit Optimization
- `sendTimerWarnings()` now exits immediately when `remainingTime` is not at a warning threshold (60, 30, 10, 5, 4, 3, 2, 1), avoiding unnecessary string lookups and object construction for ~99% of ticks

### ExpiringCache — Atomic Get Operations
- Made the compound get-check-remove operation in `ExpiringCache.get()` atomic via explicit `synchronized(cache)` block, preventing a race where two threads could read the same entry concurrently and both try to remove it

---

## 🔇 Console Output Polish

### Duplicate Log Removal
- **GamblingManager** — Removed duplicate "MineStacks gambling system initialized" (already logged by ServerManagementMod)
- **ModFileTransferManager** — Removed duplicate "OTA Update System initialized" (already logged by ServerManagementMod)
- **PlayerJoinListener** — Removed 4 duplicate lines (server OTA version, data version, JAR name, JAR size) already logged client-side in VersionCheckPacket
- **MineBayManager** — Removed duplicate "Created MineBay listing" (already logged with more detail in CreateListingPacket)
- **SessionEventHandler** — Removed duplicate "Created session for player" (already logged in SessionManager)
- **EconomyServerHandler** — Removed duplicate "Server stopping - saving economy data" (already implied by EconomyManager shutdown)

### Verbose Banner Condensation
- **VersionCheckPacket** — Condensed 8-line INFO banner on every player connect into a single DEBUG line
- **VersionCheckPacket** — Condensed 7-line WARN "UPDATE AVAILABLE" banner into a single INFO line
- **PlayerJoinListener** — Condensed 6-line WARN CurseForge update banner into a single INFO line
- **OTAUpdateManager** — Condensed 7-line WARN "VERSION MISMATCH" banner into a single INFO line
- **WorldManagerData** — Condensed 3 INFO lines per save (path + portal states) into 1 DEBUG line

### Log Level Corrections
- **MineBayManager** hold/release item — Downgraded from INFO to DEBUG (fires on every MineBay interaction)
- **MineBayManager** save — Downgraded from INFO to DEBUG (fires frequently during gameplay)
- **WorldManager** save — Downgraded from INFO to DEBUG
- **FeatureManager** sync states — Downgraded from INFO to DEBUG (fires on every client connect)
- **VersionCheckPacket** versions-match — Downgraded from INFO to DEBUG (normal case, not noteworthy)
- **PlayerJoinListener** version check flow — Downgraded join/send messages from INFO to DEBUG
- **PacketTimestampTracker** — Removed per-packet DEBUG trace log (fired on every single packet processed)

### Debug Artifact Removal
- **OTAUpdateManager** — Removed `=== CALLED ===` and `updateInProgress:` debug messages left at INFO level
- **OTAUpdateManager** — Removed raw `Minecraft instance:` and `Player instance:` object logging
- **VersionCheckPacket** — Removed redundant "Calling OTAUpdateManager.handleVersionMismatch()" INFO log
- **SlimeHeadManager** — Removed redundant "enabled" log that fired alongside "initialized"

### String Format Fixes
- **ModFileTransferManager** — Replaced string concatenation with SLF4J `{}` placeholder in error log
- **SecureDataStorage** — Replaced 2 string concatenations with SLF4J `{}` placeholders in error logs

---

## 📁 Files Changed

| File | Change |
|---|---|
| `ModConfig.java` | Added 5 economy config entries |
| `BankAccount.java` | Transaction limit 50→100, new constructor with starting balance |
| `EconomyData.java` | Config-driven starting balance with transaction record |
| `PlayerDailyTasks.java` | Free reward 100→50 |
| `DailyTasksManager.java` | Template manager integration for configurable rewards |
| `ClaimFreeRewardPacket.java` | Template manager value, removed duplicate variable |
| `OpenGuiPacket.java` | Syncs daily tasks via template manager |
| `ModCommands.java` | Template manager sync, HelpCommandIntegration wiring |
| `MineBayManager.java` | Max listings check, import cleanup, final field, synchronized createListing |
| `CreateListingPacket.java` | Null handling for max listings reached |
| `WorldManager.java` | Teleport cooldown system |
| `HelpCommandIntegration.java` | **NEW** — /help override with descriptions & admin filtering |
| `LoginNotificationHandler.java` | NPE null-check fix |
| `GamblingManager.java` | Removed unused import |
| `DailyTaskTemplateManager.java` | Removed dead variable |
| `EconomyManager.java` | `bankInventories` made final |
| `SyncWorldDetailPacket.java` | readUtf/writeUtf length limits |
| `WMTogglePortalsPacket.java` | readUtf/writeUtf length limits |
| `WMSetTimerPacket.java` | readUtf/writeUtf length limits |
| `WMTeleportToDimensionPacket.java` | readUtf/writeUtf length limits |
| `WMSetLobbyPacket.java` | readUtf/writeUtf length limits |
| `SyncWorldListPacket.java` | readUtf/writeUtf length limits |
| `BankTransferPacket.java` | readUtf/writeUtf length limits |
| `PMViewInventoryPacket.java` | readUtf length limit, player name validation |
| `PMSpectatePlayerPacket.java` | readUtf length limit, player name validation |
| `ModFileCompletePacket.java` | readUtf/writeUtf length limits |
| `ModFileRequestPacket.java` | readUtf/writeUtf length limits |
| `ModFileChunkPacket.java` | readUtf/writeUtf length limits |
| `PlaceGamblingBetPacket.java` | readUtf/writeUtf length limits |
| `PlaceGamblingBetWithItemPacket.java` | readUtf/writeUtf length limits |
| `SendMoneyRequestPacket.java` | readUtf/writeUtf length limits |
| `ToggleFeaturePacket.java` | readUtf/writeUtf length limits |
| `GamblingResultPacket.java` | readUtf/writeUtf length limits |
| `GamblingTensionPacket.java` | readUtf/writeUtf length limits |
| `SyncDailyTasksPacket.java` | readUtf/writeUtf length limits |
| `SyncFeatureStatesPacket.java` | readUtf/writeUtf length limits |
| `SyncAchievementsPacket.java` | readUtf/writeUtf length limits |
| `VersionCheckPacket.java` | readUtf/writeUtf length limits |
| `SyncMoneyRequestsPacket.java` | readUtf/writeUtf length limits |
| `SyncMineBayListingsPacket.java` | readUtf/writeUtf length limits |
| `RejectOfferPacket.java` | readUtf/writeUtf length limits |
| `PurchaseListingPacket.java` | readUtf/writeUtf length limits |
| `DeleteListingPacket.java` | readUtf/writeUtf length limits |
| `CreateOfferPacket.java` | readUtf/writeUtf length limits |
| `AcceptOfferPacket.java` | readUtf/writeUtf length limits |
| `ConsoleCommandPacket.java` | Log injection prevention |
| `ChatIsolationHandler.java` | HashSet for O(1) dimension lookups |
| `TabListIsolationHandler.java` | Reused static collections, HashSet dedup |
| `TimerTickHandler.java` | Early exit in sendTimerWarnings |
| `ExpiringCache.java` | Atomic synchronized get operation |
| `GamblingManager.java` | Removed duplicate init log |
| `ModFileTransferManager.java` | Condensed OTA init to single DEBUG, fixed string concat |
| `PlayerJoinListener.java` | Condensed verbose join logging, removed duplicates |
| `VersionCheckPacket.java` | Condensed version check banners, downgraded to DEBUG |
| `OTAUpdateManager.java` | Removed debug artifacts, condensed mismatch banner |
| `MineBayManager.java` | Downgraded hold/release/save logs to DEBUG, removed duplicate |
| `SessionEventHandler.java` | Removed duplicate session creation log |
| `EconomyServerHandler.java` | Removed duplicate shutdown log |
| `WorldManagerData.java` | Condensed save logs to single DEBUG line |
| `WorldManager.java` | Downgraded save log to DEBUG |
| `FeatureManager.java` | Downgraded sync log to DEBUG |
| `SlimeHeadManager.java` | Removed redundant enabled log |
| `PacketTimestampTracker.java` | Removed per-packet DEBUG trace log |
| `SecureDataStorage.java` | Fixed string concatenation to SLF4J format |
| `ota.properties` | OTA build 2 → 3, added `ota.mod_loader` |
| `gradle.properties` | Added `mod_loader` property |
| `build.gradle` | Mod loader in version string, added to processResources |
| `OTAVersion.java` | Added `modLoader` field, loader-aware compatibility checks, `parseFromString()` recognizes loaders |
| `VersionCheckPacket.java` | Added `serverModLoader` field, cross-loader OTA blocking |
| `PlayerJoinListener.java` | Sends mod loader in version check packet |
| `OTAUpdateManager.java` | Added `serverModLoader` parameter to `handleVersionMismatch` |
