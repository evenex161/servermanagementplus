# ServerManagement+ BETA-v2.1.1-v2-goldrush — *Internal Beta, superseded by v2.1.2*

Welcome to the "Goldrush" Beta! This massive update completely overhauls the global economy engine and introduces a stunning visual node-based editor for daily tasks!

## ✨ New Features & Major Overhauls
- **The Global Census Engine (Economy Rewrite):** Completely replaced the old event-driven economy with a tick-based differential scanner. The server now continuously scans chunk containers and offline player inventories to perfectly synchronize real existence with market value. This fundamentally solves economy duplication glitches and hyper-inflation!
- **Creative Mode Market Protection:** Items spawned via the Creative Inventory or the `/give` command are now explicitly tagged and ignored by the Census Engine. Server admins and creative players can no longer unknowingly crash the economy!
- **Dynamic Scarcity & Capital Scaling:** The economy now dynamically adjusts based on the server's `STARTING_BALANCE`. Uncraftable items (like Dragon's Breath or Heart of the Sea) automatically receive massive scarcity multipliers based on real server supply, driving their value up without any hardcoded anchors.
- **Immersive Fullscreen Node Editor:** Overhauled the Daily Task Template creator into a stunning, "Vision Pro"-inspired fullscreen experience. Replaced the static background with a dynamic, glowing "cyberpunk" HUD grid that floats over the blurred world. Features drag-to-connect interactive pins, cubic bezier connection lines, and a right-click 3D camera unlock system allowing players to freely look around the world while editing.
- **Multiple Item Rewards:** Daily Task Templates now fully support assigning an unlimited number of items as rewards.
- **Update Source Control & Beta Channels:** Players can now customize their update experience by explicitly selecting their preferred source portal (CurseForge or Modrinth) and configuring their preferred update channel (Public Releases or Beta Releases), with a smart automatic fallback protocol to avoid upload delays.

## 🐛 Notable Bug Fixes & Polish
- **Market Parity Fixes:** Fixed an infinite crafting cycle bug where unanchored items (like `raw_gold`) crashed prices; added explicit anchors to raw ores and synchronized client fallback values with the server's capital multiplier.
- **Node Editor Polish:** Fixed an issue where the Free Reward text fields (Quantity/Search) would visually detach from the parent node when dragged. Node blocks now strictly require a Left-Click Drag, and a new "Fullscreen" toggle expands the immersive editor across the entire game window.
- **Dynamic HUD Transitions:** The Daily Tasks HUD overlay now utilizes smooth mathematical interpolation for vertical transitions, preventing widgets from instantly popping into place. Long task descriptions are intelligently truncated to preserve prefix and suffix visibility.
- **Anti-Tamper DRM:** Implemented compile-time API key steganography. CurseForge API keys are now securely encrypted and bound to the compiled bytecode, preventing malicious tampering.
- **Floating Logo Button:** The dynamic ServerManagement logo (on the Main Menu and Pause Screen) is now freely draggable via Left-Click Hold, and no longer erroneously triggers a manual update check upon interaction.
