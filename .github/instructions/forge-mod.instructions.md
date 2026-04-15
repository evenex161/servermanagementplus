---
description: "Use when writing or modifying Java code in this Minecraft Forge mod. Covers Forge conventions, project architecture, networking, GUI, config, error handling, and coding style for the servermanagement mod."
applyTo: "src/**/*.java"
---
# Servermanagement Forge Mod — Coding Guidelines

## Current Version

- **Latest published release**: v2.0.0-b01 (release) (`servermanagementplus-v2.0.0-b01-mc1.21.1-forge-release.jar`)
- `mod_version` in `gradle.properties` must be a **clean Maven version** (e.g., `1.0.4`) — no `v` prefix or `-release` suffix. Forge's `DefaultArtifactVersion` rejects non-standard formats.
- `mod_release_type` in `gradle.properties` controls the release suffix: `pre-release` for betas, `release` for stable releases.
- `build.gradle` constructs the JAR filename: `servermanagementplus-v${mod_version}-b${mod_build}-mc${minecraft_version}-${mod_loader}-${mod_release_type}.jar`.
- `ota.releaseType` in `src/main/resources/ota.properties` uses `${mod_release_type}` (substituted by Gradle at build time).

## Release Checklist

When the user says a new version is ready for release, perform all of these steps:

1. Update `mod_version` in `gradle.properties` and `ota.version` / `ota.build` in `src/main/resources/ota.properties`.
2. Create or update `CHANGELOG_v<version>.md` with all changes since the last release.
3. Update the **Current Version** section at the top of this instructions file.
4. Update `CURSEFORGE_PAGE.md` — this is the public-facing mod description for CurseForge. Reflect any new features, commands, config changes, or removed functionality so the page stays accurate.
5. Run `gradlew clean build` and confirm the JAR is produced in `build/libs/`.

## Forge Conventions

- Target Minecraft 1.21.1 with Forge 52.1.0 and Java 21.
- Use `@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)` for event listener classes. Handler methods must be `static` with `@SubscribeEvent`.
- Client-only event handlers must specify `value = Dist.CLIENT` and `bus = Mod.EventBusSubscriber.Bus.MOD` on the class annotation.
- Use `event.enqueueWork(...)` for thread-safe work in lifecycle events (`FMLClientSetupEvent`, `FMLCommonSetupEvent`).
- All registries use `DeferredRegister` with `RegistryObject` holders. Pass `IEventBus` via a static `register()` method.
- Commands use Brigadier. Admin commands require `.requires(source -> source.hasPermission(2))`. Cast with `source.getEntity() instanceof ServerPlayer`.

## Project Architecture

- Base package: `com.servermanagement`. Keep feature code in its own sub-package under `features/`.
- Features implement the `Feature` interface (`getId`, `initialize`, `onEnable`, `onDisable`) and register through `FeatureManager`.
- Networking uses `SimpleChannel` in `ModNetworking`. Packets implement `IPacket` with two constructors (full init + `FriendlyByteBuf` deserialization), matching encode/decode field order.
- Packet handlers must call `ctx.get().enqueueWork(...)` and `ctx.get().setPacketHandled(true)`.
- Client-side data is cached via static methods in `ClientPacketHandler`. Screens read from these caches, never directly from server state.
- GUI: Menus extend `AbstractContainerMenu`, screens extend `AbstractContainerScreen<T>`. Register menu-screen bindings in `ClientSetup` inside `event.enqueueWork(...)`.

## Coding Style

- Constants: `UPPER_SNAKE_CASE`, `public static final`.
- Classes: `PascalCase`. Methods/fields: `camelCase`.
- Use `final` on fields wherever possible, especially for thread safety.
- No wildcard imports.
- Use `LogUtils.getLogger()` for the Logger (SLF4J). Log with `{}` placeholders: `LOGGER.error("Failed to do X: {}", detail, exception)`.
- Prefer early returns for disabled features: `if (!ModConfig.FEATURE_ENABLED.get()) return;`

## Error Handling

- Wrap feature/subsystem initialization in try-catch. Log the error, but never crash the mod — degrade gracefully.
- Inside async runnables (`AsyncSaveScheduler`, `ScheduledExecutorService`), always try-catch and log.
- Return `boolean` from validation/repair methods to signal success or failure.
- Use `PerformanceMetrics.getInstance()` to record saves, cache hits, and other operations for visibility.

## Config & Data Persistence

- Config uses `ForgeConfigSpec.Builder` in `ModConfig` with `static { }` block initialization. Access values via `.get()`.
- JSON data uses `Gson` with `setPrettyPrinting()`. Include a `dataVersion` field for migration support (see `DataVersion.CURRENT_VERSION`).
- Async saves go through `AsyncSaveScheduler` (debounced by key). Flush all pending saves on server shutdown.
- Encryption uses `EncryptionManager` singleton with AES-256-GCM. Use `ThreadLocal<Cipher>` for thread safety.

## GUI Responsive Scaling (ScreenScaler)

- All custom screens use `ScreenScaler` (`gui/ScreenScaler.java`) for responsive sizing.
- Reference dimensions: `REF_WIDTH=1010`, `REF_HEIGHT=570`, `MIN_SCALE=0.85`.
- At 1080p screens render ~95% size (960×540 GUI units). At 1440p+ they render at full size.
- Usage: `ScreenScaler.scale(baseWidth, baseHeight)` returns `int[]{scaledWidth, scaledHeight}`.
- Screens call `ScreenScaler.scale()` in `init()` and assign to `imageWidth`/`imageHeight`.

### Slot Positioning (CRITICAL — `Slot.x`/`Slot.y` are `final`)

- In Forge 1.21.1, `Slot.x` and `Slot.y` are **final fields** — they cannot be reassigned after construction.
- **Workaround**: Compute scaled positions at Menu constructor time using `ScreenScaler`.
- Pattern: Add `getScaledPanelWidth()` / `getClientPanelWidth()` methods to the Menu class. Call `ScreenScaler.scale()` on the client side; fall back to a default on the server (where ScreenScaler is unavailable).
- Slot positions must be computed relative to `panelWidth` in the constructor: e.g., `new GamblingSlot(container, 0, panelWidth - 50, 30)`.
- Custom slot subclasses: `ToggleableSlot` (MineBay — visibility toggle, `setBasePosition()`), `GamblingSlot` (MineStacks — enabled/disabled toggle via `setEnabled()`).

### Render Order (`AbstractContainerScreen`)

- `AbstractContainerScreen.render()` internally calls `renderBg()` a second time.
- Any labels drawn **before** `super.render()` will be painted over. Always draw custom text **after** `super.render()`.

## GUI Inventory (All Screens & Menus)

### Admin-Only GUIs (OP level 2 required)
| Screen | Menu | Purpose |
|--------|------|---------|
| ConfigScreen | ConfigMenu | Feature toggles |
| ConsoleScreen | ConsoleMenu | Remote server console |
| WorldListScreen | WorldListMenu | Dimension list |
| WorldDetailScreen | WorldDetailMenu | Per-dimension settings |
| PlayerManagerScreen | PlayerManagerMenu | Online player management |
| PortalTimerScreen | PortalTimerMenu | Portal cooldown config |
| PerformanceSettingsScreen | PerformanceSettingsMenu | Server perf tuning |
| MotdEditorScreen | MotdEditorMenu | MOTD color/format editor |
| GlobalSettingsScreen | GlobalSettingsMenu | Global server settings |
| ServerManagementScreen | ServerManagementMenu | Top-level admin hub |
| EconomyManagementScreen | EconomyManagementMenu | Economy CRUD admin |

### Player GUIs (no OP required)
| Screen | Menu | Purpose |
|--------|------|---------|
| DashboardScreen | DashboardMenu | Feature hub with cards |
| BankScreen | BankMenu | Currency & item banking |
| DailyTasksScreen | DailyTasksMenu | Daily challenge system |
| AchievementsScreen | AchievementsMenu | Achievement rewards |
| MineBayScreen | MineBayMenu | Player marketplace |
| MineStacksScreen | MineStacksMenu | Casino (4 game modes) |

### Widget Classes (`gui/widgets/`)
- `DashboardCard` — Card widget for dashboard feature panels. Truncates description text with ".." when exceeding card width.
- `ConsoleOutput` — Server console output widget with vertical + horizontal scrolling, scissor clipping, and green highlighting for user commands. Shift+scroll for horizontal scroll.
- `ModernButton` — Styled button with `ButtonStyle` enum (PRIMARY, SECONDARY, SUCCESS, DANGER).

### Client-Side Data Caches
- `ClientMarketData` — Thread-safe cache for market pricing (inflation, supply/demand, recipe-based pricing). Synced via `SyncMarketPricesPacket`. Used by MineBayScreen for base price display.
- `ClientPacketHandler` — General client-side data cache for sync packets.
- `ClientBankData` — Bank balance cache synced via `SyncBankAccountPacket`.
- `ClientMineBayData` — MineBay listings cache.

## Server Console System

- `ServerConsoleManager` — Log4j appender that hooks into the root logger to capture server logs in real-time. Buffers last 50 lines, sends to subscribed players at 20 lines/tick cap.
- `ConsoleSubscribePacket` — Client→Server packet for subscribing/unsubscribing from log streaming.
- `ConsoleCommandPacket` — Client→Server packet for executing console commands. Creates a `CommandSourceStack` using `ConsoleCommandListener` as the source.
- `ConsoleCommandListener` — `CommandSource` implementation that relays command output back to the player via `ConsoleResponsePacket`. Uses `[CMD]` prefix for command results.
- **CRITICAL**: Never use `.withSuppressedOutput()` on `CommandSourceStack` — it silences `sendSuccess()`/`sendFailure()`, making commands appear to do nothing.

## MineBay Offer System

- When players place items in offer GUI slots, the menu system **moves items out of player inventory** into the menu's `offerContainer` (a `SimpleContainer`).
- **Server-side validation must read from `offerContainer`** (via `buyer.containerMenu` → cast to `MineBayMenu`), NOT from `buyer.getInventory().items` — items are no longer there.
- This is also more secure: server ignores client-sent item data in the packet and uses the authoritative server-side container state.

## MineBay Layout Constants

- **Inventory slots**: fixed at menu-relative Y=230 (3 rows of 9), hotbar at Y=288
- **Offer slots**: Y=148, offering slot at Y=85
- **Inventory separator**: Y=218-220 (drawn in `renderBg`)
- **`inventoryLabelY`**: fixed at 222 (just below separator, above slots)
- **Buttons** in states with inventory (MAKE_OFFER): positioned at `centerY + 218 - btnHeight - 3` (3px above separator)
- **View Details card**: uses `calculateDetailsCardBottom()` for dynamic height based on content; buttons positioned just below the card
- **Inventory area background**: tight-fit from Y=220 to Y=310 (actual slot bounds), not `imageHeight`
- **Page indicator**: sliding window model — `scrollOffset` increments by 1, `currentPage = scrollOffset + 1`
