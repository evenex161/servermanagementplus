# ServerManagement Mod — Gemini Developer Guide

## Project Summary
- **Environment**: MC 1.21.1 / 1.20.1, Forge 52.1.0, Java 21.
- **Core Features**: Admin dashboard (OP 2), economy (MineBay, Bank), gambling (MineStacks), world/player management, OTA updates, MOTD editor, remote console.
- **Mod ID**: `servermanagement` | **Group**: `com.servermanagement` | **Version**: 2.1.0 | **License**: MIT
- **Architecture**: MultiLoader (buildSrc convention plugins, platform-agnostic `common/`, loader-specific implementation in `forge/`, `neoforge/`, `fabric/`).
- **Platform Abstraction**: Java ServiceLoader via `IPlatformHelper`.

## Build & Test Commands
```powershell
.\gradlew :forge:build          # Build Forge JAR
.\gradlew :neoforge:build       # Build NeoForge JAR
.\gradlew :fabric:build         # Build Fabric JAR
.\gradlew build                 # Build all modules
```
- **Test Launcher (`test.bat`)**:
  - `test.bat client` (Forge client) | `test.bat server` (Forge server)
  - `test.bat client neoforge` | `test.bat server fabric`
  - `test.bat both` (Server + Client auto-connect)
  - `test.bat debug` (Server + 2 Clients, auto-op, debug logging)
- Run directories: `<loader>/runs/client/` and `<loader>/runs/server/`

## Code Conventions & Architecture
- **Naming**: Classes `PascalCase` | Methods `camelCase` | Constants `UPPER_SNAKE_CASE` | Packages `lowercase`
- **Config**: `ForgeConfigSpec` (TOML). Migration via `MigrationManager` with auto-backup to `serverdata/servermanagement/backups/`.
- **Networking**: SimpleChannel (Forge) / CustomPacketPayload (Fabric/NeoForge). All handlers must delegate via `execute()` to the main thread.
- **GUI**: `AbstractContainerScreen` + custom widgets, responsive layout via `ScreenScaler` (1010x570 reference).
- **GUI Styling**: Opaque `0xFF1A1A2E` bar + `0xFF333333` separator + gold `0xFFD700` title. Use ASCII only (no emojis in Minecraft fonts).
- **Persistence**: Gson JSON + AES-256-GCM. Async writing scheduled through virtual threads in `AsyncSaveScheduler`.

---

## Token Efficiency & Session Management Rules
To ensure optimal performance and avoid hitting context limits, Gemini MUST adhere to the following rules:

1. **Minimal Context reads**: Only read targeted files and specify line numbers (`StartLine`/`EndLine` in `view_file`) when possible. Avoid viewing entire files when only small edits are needed.
2. **Minimal diffs**: Use `replace_file_content` or `multi_replace_file_content` to perform surgically precise edits. Never rewrite entire files.
3. **Fluff-free responses**: Be extremely concise. Keep explanations brief. Focus purely on code correctness and efficiency.
4. **Token Efficiency Reminder**:
   - **Trigger**: If the current chat session has reached 10+ messages, includes large log outputs, or has loaded multiple files, Gemini *must* trigger the Token Efficiency Reminder.
   - **Action**: Start the response with:
     > [!WARNING]
     > **TOKEN EFFICIENCY REMINDER**: The current chat session is growing long. Please copy the progress summary below and start a new chat session to reset the context buffer and improve response speed.
5. **Always Keep Progress Summaries Updated**:
   - When the reminder is triggered (and at the end of every session), append/overwrite the current progress status directly in the response.
   - Summaries must follow this format:
     - **Quick Summary (Done)**: Bulleted list, extremely compact.
     - **Detailed Outstanding Tasks**: Detailed bulleted list of what is left to implement/test.
   - Standard session summaries must be saved to `session_summary.md` and appended to `docs/progress.md`.
6. **Keep GEMINI.md Up-to-Date**:
   - Every time a roadmap item is completed, a new bug is found, or architecture decisions change, Gemini MUST immediately update `GEMINI.md` (specifically the "Current Status & Roadmap" sections) to reflect the new state. This guarantees seamless collaboration between different agent sessions.

---

## Current Status & Roadmap

### Quick Summary (Done)
- [x] MultiLoader migration committed
- [x] Full compilation and packaging configured for Forge, Fabric, and NeoForge
- [x] Record-based networking migration completed
- [x] Virtual threading implementation for AsyncSaveScheduler
- [x] Tab isolation heartbeat (100-tick resync)
- [x] Case-insensitive OTA Mod JAR lookup bug resolved (in `ModFileTransferManager`)
- [x] Cryptographic S2C/C2S session token validation implemented
- [x] Config migration automatic folder backup system implemented
- [x] Recipe pricing convergence loop warning fixed (limit 25 cycles)
- [x] Fabric event/mixin wiring completed (portals, timers, advancements, item prices, stale cache screen reload)
- [x] Verified OTA mod JAR detection & case-insensitive starts-with filename pattern matches
- [x] Cleaned up and verified `src.bak/` duplicate directory is completely removed

### Future Roadmap / Tasks for Next Update (v2.x.x)
- **Loader Verification**:
  - Run loader client and server instances for NeoForge and Fabric using `test.bat`.
  - Validate portal travel blocking/permissions.
  - Verify portal timer countdown synchronization and GUI updates (check for no overlapping edit fields or incorrect countdown representations).
  - Verify advancement earnings trigger and reward payouts properly.
