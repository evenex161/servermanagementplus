# ServerManagement+ BETA-v2.1.1-v3

## 🐛 Notable Bug Fixes & Polish
- **Node Graph UI Dragging:** Fixed an issue where the node graph editor could not be dragged across the screen. Drag events are now properly routed through the screen scaling logic and will no longer be swallowed by vanilla container slot interaction checks.
- **Nested UI Focus Loss:** Fixed a bug in the Economy Node Editor where clicking the Cash Reward text box failed to unfocus the internal item search field. This previously caused all typed numbers to be incorrectly swallowed by the hidden item picker.
- **Network Stability (EncoderException):** Fixed a critical disconnect issue (`String too big`) when sending custom payloads by removing restrictive arbitrary string length bounds across all network packets. Network payload variables now utilize the standard Minecraft length limit, fully accommodating dynamically long string representations (such as 73+ character UUIDs or descriptions) without crashing the server connection.
- **"Moved too quickly" Spam Fix:** Eliminated excessive `moved too quickly!` log warnings that plagued servers using Distant Horizons or experiencing TPS drops, by dynamically increasing movement validation tolerance based on server load.
- **Chunk Gen Throttle Oscillation:** Fixed an issue where the TPS chunk throttling system would rapidly oscillate the server view distance under heavy load. The system now uses a robust deadband hysteresis, waiting for sustained TPS recovery before restoring view distances to prevent massive chunk load spikes that would instantly re-kill TPS.

## 🚀 New Features & Improvements
- **Distant Horizons Deep TPS Synergy:** Three new automated systems work together to keep your server buttery smooth with Distant Horizons installed:
  - **Chunk Gen Throttle** — Graduated, TPS-reactive view-distance reduction that scales in severity as TPS drops. Automatically activates when DH is detected and TPS falls below warning threshold. Fully reversible with hysteresis to prevent oscillation.
  - **Movement Leniency** — Intelligent anti-false-positive system that relaxes the server's "moved too quickly" detection during TPS drops or when DH is active, preventing unnecessary log spam and false kick threats.
  - **Dynamic View Distance** — Emergency view/simulation distance reduction during critical TPS drops (auto-optimize trigger). Stores and restores original distances when performance recovers.
- **Enhanced `/sm performance` Command:** Now shows real-time status of all DH synergy systems (Chunk Gen Throttle, Movement Leniency, Dynamic View Distance), current effective view/simulation distances, chunk loads throttled count, and movement leniency ticks.
- **7 New Config Options:** Full control over all DH synergy systems — toggle each independently, configure chunk reduction limits, movement leniency multiplier, and view distance reduction amount.
- **JVM GC Flag Patcher:** In-game server launch script optimization. Detects suboptimal Garbage Collectors (like G1GC) when running alongside Distant Horizons and allows OP 2+ admins to automatically patch their `run.bat`/`run.sh` scripts with optimized ZGC flags (Java 21+). Accessible via a clickable chat notification on join or the "Optimize JVM" button in the Performance Settings GUI. Safely creates `.bak` backups before modifying scripts.
- **Client-Side Rendering Enhancements:**
  - **LRU Chunk Cache:** Replaced the previous clear-on-full chunk cache with a proper Least-Recently-Used (LRU) cache, significantly reducing stuttering when rendering cached environments.
  - **Async Occlusion Culler:** Introduced an asynchronous cone-based frustum culler for entities and block entities. Offloads visibility checks to a dedicated background thread, preventing render thread bottlenecking (opt-in via Performance Settings).
- **Distant Horizons Integration Rewrite:** Replaced legacy/fabricated DH reflection hooks with a robust implementation targeting the real DH 2.x API. Includes a new `DHSyncCoordinator` that bridges SM+ server-side chunk pre-generation with DH's LOD invalidation system, ensuring LODs immediately update when new chunks are generated.
- **Advanced Performance Diagnostics:**
  - **JMX Allocation Tracker:** New zero-overhead tracker that measures JVM heap allocation rate in MB/s.
  - **Overhauled Stats GUI:** The Performance Settings stats tab now displays a GC health banner, DH version detection, active JVM heap usage, real-time allocation rate, and total GC pause times.
