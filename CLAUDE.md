# ServerManagement Mod — Project Context

## Project Summary
Minecraft Forge server management mod (MC 1.21.1, Forge 52.1.0, Java 21).
Provides admin dashboard, economy, marketplace (MineBay), gambling (MineStacks),
world/player management, OTA updates, MOTD editor, remote console, and performance tuning.
Mod ID: `servermanagement` | Group: `com.servermanagement` | Version: 2.1.0 | License: MIT

## MultiLoader Structure (jaredlll08/MultiLoader-Template)
```
buildSrc/          # Gradle convention plugins (multiloader-common.gradle, multiloader-loader.gradle)
common/            # Platform-agnostic code (3 files: Constants, Services, IPlatformHelper)
forge/             # Forge loader — ALL mod code lives here (254 Java files)
neoforge/          # NeoForge stubs (entry point + platform helper only)
fabric/            # Fabric stubs (entry point + platform helper only)
```
- Gradle 8.10 (fabric-loom incompatible with 9.x)
- Root plugins (apply false): fabric-loom 1.8-SNAPSHOT, net.neoforged.moddev 2.0.49-beta
- forge/: net.minecraftforge.gradle [6.0.24,6.2), official mappings, reobf=false
- common/: NeoForm 1.21.1-20240808.144430, Parchment 2024.11.10
- NeoForge: 21.1.80 | Fabric API: 0.109.0+1.21.1, Loader: 0.16.9
- Platform abstraction via Java ServiceLoader (IPlatformHelper interface)

## Tech Stack
- **Language**: Java 21
- **Build**: Gradle 8.10, ForgeGradle, fabric-loom, moddev
- **Config**: ForgeConfigSpec (TOML), ConfigMigration system with versioning
- **Networking**: SimpleChannel, 72 IPacket classes, auto-increment IDs
- **GUI**: AbstractContainerScreen + custom widgets, ScreenScaler for responsive layout
- **Persistence**: Gson JSON, AES-256-GCM encryption, AsyncSaveScheduler (debounced)
- **Security**: EncryptionManager (AES-256-GCM), session tokens, permission checks (OP 2)

## Code Style & Conventions
- **Classes**: PascalCase | **Methods**: camelCase | **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase (`com.servermanagement.features.economy`)
- **Config fields**: UPPER_CASE (`WORLD_MANAGER_ENABLED`)
- **Event handlers**: `@SubscribeEvent` static methods in `@Mod.EventBusSubscriber` classes
- **Registry**: DeferredRegister pattern (ModMenuTypes)
- **Commands**: Brigadier via RegisterCommandsEvent, `source.hasPermission(2)` for admin
- **Packets**: IPacket interface — constructor(fields), constructor(FriendlyByteBuf), encode(), handle()
- **Logging**: `LogUtils.getLogger()`, SLF4J `{}` placeholders, no string concatenation
- **Imports**: Specific (no wildcards), organized by package group
- **Version format**: Clean Maven version in gradle.properties (e.g., `2.1.0`), NO `v` prefix
- **GUI headers**: opaque `0xFF1A1A2E` bar + `0xFF333333` separator + gold `0xFFD700` title
- **No emoji** in MC GUI — bitmap font renders them as boxes; use ASCII

## Package Structure (forge/src/main/java/com/servermanagement/)
```
client/        — Client data caching, screen setup
commands/      — Brigadier command implementations
config/        — ForgeConfigSpec, migrations, validation
event/         — @SubscribeEvent handler classes
features/      — Feature interface + implementations
  economy/     — EconomyManager, BankAccount, DailyTasks, Achievements, MarketPricing
  gambling/    — 4 games (CoinFlip, DiceRoll, SlotMachine, Roulette), GamblingStats
  minebay/     — Marketplace (listings, offers, escrow)
  playermanager/ slimehead/ worldmanager/
gui/           — Menus, screens, widgets (18 screens, 18 menus)
  economy/ gambling/ minebay/ menu/ provider/ screen/ widgets/
network/       — ModNetworking (SimpleChannel), 72 packet classes
  packet/      — IPacket implementations
ota/           — OTA update checking (CurseForge API)
security/      — Encryption, sessions, data storage
server/        — Server-only (file transfer, console streaming)
util/          — AsyncSaveScheduler, PerformanceMetrics, helpers
```

## Key Architecture Patterns
- **Feature system**: `Feature` interface → `FeatureRegistry.registerFeatures()` → `FeatureManager`
- **Admin dashboard**: DashboardScreen hub → specialized admin screens (all require OP 2)
- **Player GUIs**: Dashboard, Bank, DailyTasks, Achievements, MineBay, MineStacks
- **Slot.x/y are final** in Forge 1.21.1 — compute scaled positions at Menu constructor time
- **Render order**: draw custom text AFTER `super.render()` (it calls renderBg() internally)
- **AsyncSaveScheduler**: debounced saves with `scheduleSave(key, runnable)`, `flushAll()` on shutdown
- **ScreenScaler**: REF 1010×570, MIN_SCALE 0.85, ~95% at 1080p, full at 1440p+

## Known Issues & TODOs
- `SessionEventHandler.java:23` — TODO: Send session token to client via packet
- `MigrationManager.java:141` — TODO: Implement backup for critical migrations
- OTA update: "Could not find mod JAR file" warning (filename changed after MultiLoader migration)
- Recipe pricing: "did not fully converge after 100 iterations" warning on startup (non-critical)
- NeoForge/Fabric modules are stubs — real porting not yet done

## Pending Tasks
- [ ] Commit MultiLoader migration (260 files changed, untested commit)
- [ ] Delete `src.bak/` (old source backup, 253 duplicate files)
- [ ] Implement MultiLoader for 1.20.1 branch (branch was deleted from remote)
- [ ] Port mod code to NeoForge/Fabric (currently stubs only)
- [ ] Fix OTA JAR detection for new MultiLoader filename pattern

## Build & Test
```powershell
.\gradlew :forge:build          # Build forge JAR (produces forge/build/libs/*.jar)
.\gradlew :neoforge:build       # Build neoforge JAR
.\gradlew :fabric:build         # Build fabric JAR
.\gradlew build                 # Build all modules
```
Test launcher (`test.bat`):
```batch
test.bat client                 # Forge client (default)
test.bat server                 # Forge server (nogui)
test.bat client neoforge        # NeoForge client
test.bat server fabric          # Fabric server
test.bat both                   # Server + client (auto-connect)
test.bat debug                  # Server + 2 clients, auto-op, debug logging
test.bat debug neoforge         # Same but on NeoForge
```
Run directories: `<loader>/runs/client/` and `<loader>/runs/server/`
Gradle properties: `-PmcUsername=Name` (set player name), `-PmcGameDir=path` (override client game dir)

## Session Workflow
- Think before acting. Read existing files before writing code.
- Prefer editing over rewriting whole files.
- Test your code before declaring done.
- No sycophantic openers or closing fluff.
- User instructions always override this file.
- Always append session summaries to `docs/progress.md`
- Always save standalone summary to `session_summary.md`
- Always keep `.gitignore` updated to only push relevant files