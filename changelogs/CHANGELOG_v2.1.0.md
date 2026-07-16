# Changelog — ServerManagement+ v2.1.0 (Minecraft 1.20.1)

**Release Date:** Jun 10, 2026
**Latest Changes:** Jun 10, 2026
**Minecraft:** 1.20.1 | **Forge:** 47.4.0 | **Fabric:** 0.92.8+1.20.1 (loader 0.19.2) | **Branch:** `mc/1.20.1`

---

## Overview

v2.1.0 is the **1.20.1 backport** of the v2.1.0 release shipped on the `mc/1.21.1-forge` branch. It brings the full feature set, GUI polish, networking refactor, and bug-fix work from the 1.21.1 line back to Minecraft 1.20.1. NeoForge is intentionally **not** included on this branch — Forge 47.4.0 and Fabric 0.92.8 (loader 0.19.2) on Minecraft 1.20.1 are both shipped as full feature builds. All multi-loader-derived code improvements (record-based packets, virtual-thread async I/O, tab-isolation heartbeat, GUI scaling pass, etc.) are present in both modules.

---

## Project Structure

The repo follows the **jaredlll08/MultiLoader-Template** layout used on the 1.21.1 branch, but with NeoForge dropped:

- **`buildSrc/`** — Gradle convention plugins
- **`common/`** — platform-agnostic code (`Constants`, `Services`, `IPlatformHelper`)
- **`forge/`** — full Forge 1.20.1 implementation (254 files)
- **`fabric/`** — full Fabric 1.20.1 implementation (254 files), built jar `ServerManagement+-fabric-1.20.1-2.1.0-mc1.20.1.jar` (~852 KB).

Old single-module `src/main/` removed; everything now lives under `forge/src/main/` and `fabric/src/main/`.

---

## Backported Highlights

### Record-Based Networking
- All **72 packet classes** converted from regular Java classes to **Java records** (mirrors the 1.21.1 work).
- `FriendlyByteBuf` decode constructors delegate to canonical record constructors via `this(buf.readX(), …)`.
- Static handlers / `Pattern` constants preserved in record bodies.
- Networking still uses Forge 1.20.1's `SimpleChannel` (`NetworkRegistry.newSimpleChannel`) — the record refactor is independent of the channel API.

### Virtual-Thread AsyncSaveScheduler
- `AsyncSaveScheduler` rewritten to dispatch save I/O to a daemon executor pool while a single-thread `ScheduledExecutorService` handles debounce timing.
- Reduces platform-thread overhead for concurrent save operations.

### Tab-Isolation Heartbeat
- Added a **100-tick (5 s) heartbeat** in `TabListIsolationHandler` so per-world tab membership self-corrects after dimension changes / re-logins.

### GUI Polish & Scaling Pass
- Every screen reviewed against in-game GUI Scale 3.0 (854×457) screenshots.
- Backported uniform pose-matrix scaling for all `ScalableContainerScreen`-derived screens.
- Dashboard cards now shrink-to-fit; MineBay edit-back navigation restored.
- Spectate flow / world-detail sync polish.
- Contrast & animation fixes from Phase 2.3.
- Re-enabled item-slot clicks at non-1.0 GUI scale.

### Slime Heads
- Slime-head item / placement / break-protection / noteblock-sound features ported back to the 1.20.1 NBT (`SkullOwner` + `Tag`-based custom data) instead of 1.21's `DataComponents`.

### Achievement Reward System
- Backported tier classification from `Advancement.DisplayInfo#getFrame()` (the 1.20.1 equivalent of 1.21's `getType()`).
- Reward listener uses 1.20.1 advancement APIs (`Advancement` instead of `AdvancementHolder`, `getCriteria()` / `getParent()` accessors).

### Economy / MineBay / Margin History
- All economy + MineBay improvements from v2.1.0 ported.
- Storage I/O uses 1.20.1's `File`-based `NbtIo` overloads (`writeCompressed(CompoundTag, File)` / `readCompressed(File)`).
- `server.getServerDirectory()` returns `File` on 1.20.1, so `.toPath()` is added before `.resolve(…)` chains.

### Other Backports
- `ItemStack` comparison uses 1.20.1's `isSameItemSameTags` (1.21 component-aware comparison not available).
- `Component.literal` / hover-name accessors switched back to `setHoverName` / `getHoverName` / `hasCustomHoverName`.
- `ResourceLocation` constructed via `new ResourceLocation(ns, path)` (the 1.21 static factories don't exist on 1.20.1).
- Mouse-scroll override signature reverted to 1.20.1's 3-arg form `mouseScrolled(double, double, double)`.
- `Screen.renderBackground(GuiGraphics)` 1-arg override used in place of 1.21's 4-arg variant.
- `ClientboundPlayerInfoUpdatePacket` rebuilt via `new …(FriendlyByteBuf)` constructor instead of 1.21's `STREAM_CODEC.decode(buf)`.

---

## Technical Improvements

- Gradle wrapper: **8.11**
- Java toolchain: **17** (Forge 1.20.1 still targets Java 17)
- ForgeGradle: [6.0.16, 6.2)
- Parchment mappings: 2023.09.03 (1.20.1)
- Centralized dependency versions via `gradle/libs.versions.toml`

---

## Migration Notes (vs. v2.0.x on `mc/1.20.1-forge`)

- Source layout moved from `src/main/java/…` to `forge/src/main/java/…` (and `fabric/src/main/java/…`).
- Output jar artifacts: `ServerManagement-forge-1.20.1-2.1.0-mc1.20.1.jar` and `ServerManagement+-fabric-1.20.1-2.1.0-mc1.20.1.jar`.
- The `mc/1.20.1-forge` branch was renamed to `mc/1.20.1`. Tag `pre-v2.1.0-backport` marks the pre-backport state for rollback.
- NeoForge support removed from this branch; if you need NeoForge on Minecraft 1.21.1, use `mc/1.21.1-forge`.
- Fabric 1.20.1 ships the full feature set on par with Forge. Networking uses the legacy `ServerPlayNetworking` / `ClientPlayNetworking` API (1.20.1 predates `CustomPacketPayload` / `PayloadTypeRegistry`). An access widener exposes `AbstractContainerScreen` / `Screen` fields needed by the GUI debug overlay and `MineStacksScreen`.

---

## Known Limitations

- Some hard-coded `Component.literal("§a…")` strings in source files contain mojibake (`┬º`) inherited from the 1.21.1 source — purely cosmetic in-game, does not affect compile or behavior. Will be cleaned up in a future patch release.
- 1.20.1 lacks 1.21's component-aware `ItemStack.isSameItemSameComponents`; backport falls back to `isSameItemSameTags`. Item comparison is therefore tag-based, not component-based.
