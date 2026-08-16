# ServerManagement+ v2.1.2-b3 — First Public Beta for Minecraft 1.21.1

> This is the first public beta of ServerManagement+ for Minecraft 1.21.1. It consolidates all improvements from the internal v2.1.1 beta cycle (Beta 1 through Beta 3) into a single release under the new v2.1.2 version line.

---

## 🏗️ Architecture & Up-Porting
- **Up-ported from 1.20.1 to 1.21.1:** Successfully migrated all architectural changes, UI overhauls, and bug fixes from the `mc/1.20.1` branch.
- **MultiLoader Parity:** Full compatibility across **Fabric**, **Forge**, and **NeoForge** loaders on 1.21.1.
- **NeoForge Compatibility:** Replaced legacy `net.minecraftforge.fml` hooks with `net.neoforged.fml` equivalents.
- **CustomPacketPayload Migration:** Completely migrated the legacy packet system to 1.21.1's `CustomPacketPayload` and `StreamCodec` architecture.

---

## ✨ New Features & Major Overhauls

### Economy Engine
- **The Global Census Engine (Economy Rewrite):** Replaced the old event-driven economy with a tick-based differential scanner. The server now continuously scans chunk containers and offline player inventories to perfectly synchronize real existence with market value — fundamentally solving economy duplication glitches and hyper-inflation.
- **Creative Mode Market Protection:** Items spawned via Creative Inventory or `/give` are now tagged and ignored by the Census Engine, preventing admins from unknowingly crashing the economy.
- **Dynamic Scarcity & Capital Scaling:** The economy dynamically adjusts based on the server's `STARTING_BALANCE`. Uncraftable items (like Dragon's Breath or Heart of the Sea) automatically receive scarcity multipliers based on real server supply.
- **Comprehensive Economy Stabilization:** Overhauled the economy engine's scaling logic to prevent price collapses. Intrinsic item values no longer crash to the $0.50 floor when players spend their starting balances. Inflation now only scales prices *upwards* as server wealth grows.
- **Dynamic Pricing Caps:** Items with zero supply no longer skyrocket to multi-million dollar prices. Introduced smoother scarcity curves and hard price caps for unknown items.
- **Supply/Demand Curve Smoothing:** Abundant building materials retain much more of their value. The aggressive supply deflation penalty has been significantly softened.
- **Reverse Crafting Loop Fixed:** Removed recursive recipe calculations that caused raw material values to spiral downwards. Prices now strictly flow upward from raw materials to crafted products.

### Node Editor & Daily Tasks
- **Immersive Fullscreen Node Editor:** Overhauled the Daily Task Template creator into a stunning fullscreen experience with a dynamic glowing grid, drag-to-connect interactive pins, and cubic bezier connection lines.
- **Multiple Item Rewards:** Daily Task Templates now support assigning unlimited items as rewards.
- **Dynamic HUD Transitions:** The Daily Tasks HUD overlay uses smooth mathematical interpolation for vertical transitions. Long task descriptions are intelligently truncated.

### Performance Orchestration
- **Zstd Packet Compression:** Added `zstd-jni` library with `ZstdPacketCompressor` for zero-copy compression directly in the Netty pipeline.
- **Server-Side Optimization Suite:** Chunk PreGenerator (spiral matrix algorithm), MSPT Monitor (throttles at 45ms), Dynamic Workload Scaler, and Chunk Unload Delay Manager.
- **Client-Side Rendering Engine:** LRU Chunk Cache (up to 10k unloaded chunks), Fake Chunk Injector, Smart Packet Queue (FOV-prioritized), and Async Occlusion Culler.
- **Distant Horizons Deep TPS Synergy:**
  - **Chunk Gen Throttle** — TPS-reactive view-distance reduction with hysteresis to prevent oscillation.
  - **Movement Leniency** — Relaxes "moved too quickly" detection during TPS drops or when DH is active.
  - **Dynamic View Distance** — Emergency view/simulation distance reduction during critical TPS drops.
  - **DH Sync Coordinator** — Bridges SM+ server-side chunk pre-generation with DH's LOD invalidation system.
- **JVM GC Flag Patcher:** Detects suboptimal GCs and allows admins to auto-patch launch scripts with optimized ZGC flags (Java 21+). Accessible via chat notification on join or Performance Settings GUI.
- **Advanced Performance Diagnostics:** JMX Allocation Tracker (MB/s), GC health banners, DH version detection, real-time allocation rate, and total GC pause times in the overhauled Stats GUI.
- **7 New Config Options:** Full control over all DH synergy systems — toggle each independently, configure chunk reduction limits, movement leniency multiplier, and view distance reduction amount.

### Updater System
- **SM Updater Full Integration:** Server Updater card on Admin Dashboard, `/sm update` command, client-side TitleScreen interceptor for automatic update prompts, and background server update checking.
- **Update Source Control & Beta Channels:** Players can customize update sources (CurseForge/Modrinth) and channels (Release/Beta) with smart fallback.
- **Updater Log Cleanup:** `updater_jvm_error.log`, `updater_jvm_output.log`, and `updater_log.txt` are now automatically cleaned up only after a successful update is confirmed. Failed update logs are preserved for debugging.

### Economy UI & Market
- **MineBay Enhancements:** Restructured search box UX, fixed focus bugs, seamless search across tabs, proper empty state for zero-result searches.
- **Trade Blacklist Editor:** Full focus/keyboard event routing, hold-to-delete animation, CTRL+Click multi-select, autocomplete rendering fixes, and strict enforcement in MineBayScreen.
- **Unsaved Changes Protection:** Confirmation dialog when editing templates, configuring rewards, or changing settings prevents accidental data loss.
- **Enforced Module Toggles:** `/minebay` and `/minestacks` now respect both global economy state and granular config toggles.

### Other
- **Floating Logo Button:** Freely draggable logo on Main Menu and Pause Screen with smooth edge-bounce physics. Dynamic glowing notification dot for pending updates. OP-2 gated on Pause Screen.
- **Anti-Tamper DRM:** Compile-time API key steganography for CurseForge API keys.
- **Enhanced `/sm performance` Command:** Real-time status of all DH synergy systems.

---

## 🐛 Notable Bug Fixes
- **Node Graph Dragging:** Fixed drag events being swallowed by vanilla container slot interaction checks.
- **Nested UI Focus Loss:** Fixed Cash Reward text box failing to unfocus the item search field in the Node Editor.
- **Template Editor Fullscreen Fix:** Fixed Save/Cancel/Fullscreen buttons being unclickable in fullscreen mode.
- **Fullscreen Editor Persistence:** Fixed fullscreen background persisting when switching tabs.
- **Template Saving & Rewards:** Fixed templates failing to save with item-only rewards (no cash).
- **Template Search & Scrolling:** Fixed search box not filtering and added mouse wheel scrolling.
- **Network Stability (EncoderException):** Fixed `String too big` disconnects by removing restrictive string length bounds on network packets.
- **"Moved too quickly" Spam Fix:** Dynamically increased movement validation tolerance based on server load.
- **Chunk Gen Throttle Oscillation:** Added deadband hysteresis to prevent view distance oscillation under heavy load.
- **Qty Field Focus Fix:** Fixed unselectable Qty text field on Free Reward Tab.
- **Unsaved Changes Overlay Rendering:** Fixed GUI layering glitches with dim overlay rendering.
- **F3 Debug Screen HUD Overlap:** HUD auto-hides when debug mode is active.
- **MineBay & Economy UI Keybind Fixes:** "E" key no longer closes the UI while typing in search/price fields.
- **Crafting Cycle Feedback Loop Breaker:** Resolved exponential price divergence for modded items with reversible crafting recipes.
- **Economy & Feature Toggle Architecture Fix:** Fixed 5 critical issues in the disable toggle system across all loaders.
- **Client-Side Price Sync:** Fixed client showing different prices than the server.
- **Fabric Access Widener Fix:** Fixed `IllegalAccessError` crash when opening MineStacksScreen on Fabric due to missing `accessWidener` declaration in `fabric.mod.json`.
- **Performance Settings Access Control:** Fixed non-OP players being able to see and access the Floating Logo Button on the Pause Screen.
