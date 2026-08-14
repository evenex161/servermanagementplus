# ServerManagement+ BETA-v2.1.1-v2-goldrush

Welcome to the "Goldrush" Beta! This massive update completely overhauls the global economy engine, introduces a stunning visual node-based editor for daily tasks, and brings full compatibility to Minecraft 1.21.1 across Forge, Fabric, and NeoForge!

## ✨ New Features & Major Overhauls
- **The Global Census Engine (Economy Rewrite):** Completely replaced the old event-driven economy with a tick-based differential scanner. The server now continuously scans chunk containers and offline player inventories to perfectly synchronize real existence with market value. This fundamentally solves economy duplication glitches and hyper-inflation!
- **Creative Mode Market Protection:** Items spawned via the Creative Inventory or the `/give` command are now explicitly tagged and ignored by the Census Engine. Server admins and creative players can no longer unknowingly crash the economy!
- **Dynamic Scarcity & Capital Scaling:** The economy now dynamically adjusts based on the server's `STARTING_BALANCE`. Uncraftable items (like Dragon's Breath or Heart of the Sea) automatically receive massive scarcity multipliers based on real server supply, driving their value up without any hardcoded anchors.
- **Visual Node-Based Editor:** Overhauled the Daily Task Template creator into a TIA Portal-inspired visual node-based editor! Features drag-to-connect interactive pins, cubic bezier connection lines, and hover glow animations.
- **Multiple Item Rewards:** Daily Task Templates now fully support assigning an unlimited number of items as rewards.
- **Smart Background Updater:** The SM Updater is now fully integrated across all loaders. It features silent background checks, a beautiful `UpdateAvailableScreen` upon game startup, and a dedicated `Server Updater` card in the Admin Dashboard.
- **Performance Orchestration & Synergy Engine:** Integrated `zstd-jni` packet compression via raw Netty ByteBuffers for zero-copy memory efficiency. Added an MSPT Monitor, dynamic workload scaling, and Distant Horizons tiered rendering pipeline integration.

## 🐛 Notable Bug Fixes & Polish
- **Crafting Cycle Feedback Loop Breaker:** Resolved exponential price divergence for modded items caused by unanchored reversible crafting recipes (e.g., BetterNether). A hard cap ceiling now automatically locks cyclic feedback loops to prevent game-breaking market prices.
- **Market Parity Fixes:** Fixed an infinite crafting cycle bug where unanchored items (like `raw_gold`) crashed prices; added explicit anchors to raw ores and synchronized client fallback values with the server's capital multiplier.
- **Trade Blacklist Enforcement:** Implemented a new `TradeBlacklistEditorWidget` with multi-select (CTRL+Click), item ID autocompletion, and an interruptible hold-to-delete animation. Banned items are now strictly blocked in MineBay and visually labeled in client tooltips.
- **Dynamic HUD Transitions:** The Daily Tasks HUD overlay now utilizes smooth mathematical interpolation for vertical transitions, preventing widgets from instantly popping into place. Long task descriptions are intelligently truncated to preserve prefix and suffix visibility.
- **Enforced Module Toggles:** `/minebay` and `/minestacks` commands now strictly respect both global economy feature states and granular config toggles, preventing disabled module bypasses.
- **Anti-Tamper DRM:** Implemented compile-time API key steganography. CurseForge API keys are now securely encrypted and bound to the compiled bytecode, preventing malicious tampering.
- **Dedicated Server Stability:** Solved critical packet serialization crashes and JVM class verification panics on dedicated servers by completely migrating to the 1.21.1 `CustomPacketPayload` and `StreamCodec` architecture.
