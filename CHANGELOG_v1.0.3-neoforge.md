# Changelog - v1.0.3 NeoForge (Initial NeoForge Port & Multi-Loader Support)

**Release Date**: April 5, 2026  
**Previous Version**: N/A (first NeoForge release — ported from Forge v1.0.3-b04)  
**Minecraft Version**: 1.21.1  
**Mod Loader**: NeoForge  
**NeoForge Version**: 21.1.222+  
**OTA Build**: 04  
**JAR File**: `servermanagementplus-v1.0.3-b04-mc1.21.1-neoforge-release.jar`  
**Branch**: `mc/1.21.1-neoforge`

> **Forge users**: See [CHANGELOG_v1.0.3.md](CHANGELOG_v1.0.3.md) for the Forge-specific changelog.

---

## 🔀 NeoForge Port

### Full Port from Forge to NeoForge 21.1
This is the initial NeoForge release of Server Management Plus, ported from the Forge `mc/1.21.1` branch. All features, systems, and fixes from the Forge v1.0.3 release are included.

- **NeoForge 21.1.222** with **NeoGradle 7.1.21** (Gradle 9.3.0, Java 21)
- **Build system migration**: ForgeGradle → NeoGradle, Forge dependency replaced with NeoForge
- **Mod metadata**: `META-INF/mods.toml` → `META-INF/neoforge.mods.toml`
- **Mod entry point**: Constructor updated to `(IEventBus, ModContainer)` signature (NeoForge convention)

### Networking API Migration
- **Complete packet system rewrite**: All 55 packets now implement `CustomPacketPayload` with `TYPE`, `STREAM_CODEC`, and `type()` method
- **SimpleChannel → PayloadRegistrar**: `ModNetworking.java` fully rewritten using NeoForge's `PayloadRegistrar` API
- **Packet distribution**: Migrated to `PacketDistributor.sendToServer()`, `sendToPlayer()`, `sendToAllPlayers()`
- **Packet handlers**: `Supplier<NetworkEvent.Context>` → `IPayloadContext`, `context.getSender()` → `(ServerPlayer) ctx.player()`
- **Packet completion**: `context.setPacketHandled(true)` removed (not needed in NeoForge)

### API Changes from Forge
| Forge API | NeoForge API |
|---|---|
| `@Mod.EventBusSubscriber` | `@EventBusSubscriber` |
| `Bus.FORGE` | `Bus.GAME` |
| `ForgeConfigSpec` | `ModConfigSpec` |
| `NetworkEvent.Context` | `IPayloadContext` |
| `SimpleChannel` | `PayloadRegistrar` |
| `MenuScreens.register()` | `RegisterMenuScreensEvent` |
| `TickEvent.ServerTickEvent` | `ServerTickEvent.Post` |
| `BuiltInRegistries` (Forge) | `BuiltInRegistries` (NeoForge, same name, different package) |

### Import Remapping
- Bulk import replacement across all 206 Java source files
- `net.minecraftforge.*` → `net.neoforged.*` throughout the codebase
- `net.minecraftforge.fml.*` → `net.neoforged.fml.*`
- `net.minecraftforge.event.*` → `net.neoforged.neoforge.event.*`
- `net.minecraftforge.registries.*` → `net.neoforged.neoforge.registries.*`

---

## 🔢 Multi-Loader Version Naming

### JAR Filename & OTA Identification
- **Mod loader in filename**: JAR files now include the mod loader identifier: `servermanagementplus-v1.0.3-b04-mc1.21.1-neoforge-release.jar`
- **Version format**: `v<version>-b<build>-mc<mcVersion>-<loader>-release`
- `mod_loader=neoforge` property in `gradle.properties`
- `ota.mod_loader` property in `ota.properties` populated by Gradle at build time

### Cross-Loader OTA Protection
- **Mod loader awareness in OTA system** — `OTAVersion` now tracks the mod loader alongside Minecraft version
- **Cross-loader update blocking** — A NeoForge client will not receive OTA updates from a Forge server (or vice versa)
- **`VersionCheckPacket`** carries the server's mod loader, enabling client-side loader compatibility checks
- **`OTAVersion.isCompatibleWith()`** validates both Minecraft version and mod loader match
- **`OTAVersion.parseFromString()`** recognizes known loaders: `forge`, `neoforge`, `fabric`, `quilt`

---

## ✅ Feature Parity with Forge v1.0.3

All features from the Forge v1.0.3 release are fully functional in this NeoForge port:

- **Economy System** — Bank accounts, transactions, encrypted storage, starting balance
- **MineBay Marketplace** — Player-to-player item trading with listings and offers
- **MineStacks Gambling** — Slot machine, coin flip, dice roll, crash game
- **World Manager** — Multi-world teleportation, portal control, dimension timers
- **Daily Tasks** — Configurable daily challenges with rewards
- **Player Management** — Inventory viewing, spectating, money transfers
- **OTA Update System** — Automatic mod updates with version checking
- **Dashboard** — Admin GUI with server management controls
- **Chat & Tab List Isolation** — Per-dimension chat and tab list separation
- **Achievement System** — Custom achievement tracking
- **Session Tracking** — Player session history and statistics
- **Help Integration** — `/help` command override with admin filtering
- **Config-Driven Economy** — All economy values configurable via TOML

### Security & Performance
All security hardening and performance improvements from Forge v1.0.3 are included:
- Network buffer overflow protection (45 `readUtf()` calls with length limits)
- Player name regex validation
- Log injection prevention
- Race condition fix in MineBayManager
- O(1) lookups in ChatIsolationHandler and TabListIsolationHandler
- Timer tick handler early exit optimization
- Atomic ExpiringCache operations

---

## 📁 Files Changed (vs. Forge mc/1.21.1 branch)

| File | Change |
|---|---|
| `build.gradle` | ForgeGradle → NeoGradle 7.1.21, Forge → NeoForge 21.1.222, version string with `${mod_loader}` |
| `settings.gradle` | NeoGradle plugin repository |
| `gradle.properties` | NeoForge version, `mod_loader=neoforge`, removed Forge-specific properties |
| `neoforge.mods.toml` | **NEW** — NeoForge mod metadata (replaces `mods.toml`) |
| `ota.properties` | Added `ota.mod_loader=${mod_loader}` |
| `ServerManagementMod.java` | Constructor signature: `(IEventBus, ModContainer)` |
| `ModNetworking.java` | Complete rewrite: `SimpleChannel` → `PayloadRegistrar` |
| `ClientSetup.java` | `MenuScreens.register()` → `RegisterMenuScreensEvent` |
| All 55 packet classes | `CustomPacketPayload` implementation with `TYPE`, `STREAM_CODEC`, `type()` |
| All 206 Java files | Import remapping: `net.minecraftforge.*` → `net.neoforged.*` |
| `OTAVersion.java` | Added `modLoader` field, loader-aware compatibility checks |
| `VersionCheckPacket.java` | Added `serverModLoader` field, cross-loader OTA blocking |
| `PlayerJoinListener.java` | Sends mod loader in version check packet |
| `OTAUpdateManager.java` | Added `serverModLoader` parameter to `handleVersionMismatch` |
