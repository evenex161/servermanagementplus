---
description: "Use when writing or modifying Java code in this Minecraft Forge mod. Covers Forge conventions, project architecture, networking, GUI, config, error handling, and coding style for the servermanagement mod."
applyTo: "src/**/*.java"
---
# Servermanagement Forge Mod — Coding Guidelines

## Current Version

- **Latest published release**: v1.0.3 (`servermanagementplus-v1.0.3-mc1.21.1-release.jar`)
- `mod_version` in `gradle.properties` must be a **clean Maven version** (e.g., `1.0.3`) — no `v` prefix or `-release` suffix. Forge's `DefaultArtifactVersion` rejects non-standard formats.
- `build.gradle` prepends `v`, appends `-mc{minecraft_version}-release` to produce the JAR filename: `servermanagementplus-v${mod_version}-mc${minecraft_version}-release.jar`.
- `ota.version` in `src/main/resources/ota.properties` uses the full display format (`v1.0.3-release`). OTA version parsing in `OTAVersion.java` strips leading `v` and trailing `-release`.
- `ota.minecraft_version` in `src/main/resources/ota.properties` must match the `minecraft_version` in `gradle.properties`. This is used for OTA multi-version compatibility validation.
- **Multi-version support**: The OTA system validates Minecraft version compatibility before sending updates. A 1.20.1 server will never push a 1.21.1 JAR to a client (or vice versa).

## Release Checklist

When the user says a new version is ready for release, perform all of these steps:

1. Update `mod_version` in `gradle.properties` and `ota.version` / `ota.build` / `ota.minecraft_version` in `src/main/resources/ota.properties`.
2. Create or update `CHANGELOG_v<version>.md` with all changes since the last release.
3. Update the **Current Version** section at the top of this instructions file.
4. Update `CURSEFORGE_PAGE.md` — this is the public-facing mod description for CurseForge. Reflect any new features, commands, config changes, or removed functionality so the page stays accurate.
5. Run `gradlew clean build` and confirm the JAR is produced in `build/libs/`.

## Forge Conventions

- Target Minecraft version and Forge version are defined in `gradle.properties` (`minecraft_version` and `forge_version`). Branch `mc/1.20.1` targets MC 1.20.1 with Forge 47.4.x and Java 17. Branch `mc/1.21.1` targets MC 1.21.1 with Forge 52.1.0 and Java 21.
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
