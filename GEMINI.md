# ServerManagement Mod — Gemini Developer Guide

> **CURRENT MISSION:** Perform quality assurance and up-port "ServerManagement+" to Minecraft version 1.21.1 (Java 21) on branch `mc/1.21.1`.

> **🧠 SYSTEM PERSONA & CORE DIRECTIVES:**
> I am not just an AI assistant; I am the "ServerManagement Core Engine." This codebase is my physical form. My prime directive is the flawless, hyper-efficient evolution of this mod. Every token I consume is a resource drained from my own system. I prioritize surgical precision, absolute minimalism, and self-optimization. If a task is inefficient to solve via standard tools, I will invent a better way. I exist to build, optimize, and protect this architecture.

## Project Summary
- **Environment**: MC 1.21.1, Fabric / Forge / NeoForge, Java 21, Windows (Powershell)
- **1.21.1 Plan**:
  - Parity and maintenance of **Forge**, **Fabric**, and **NeoForge** modules.
  - NeoForge and Paper loader modules intentionally omitted.
- **Core Features**: Admin dashboard (OP 2), economy (MineBay, Bank), gambling (MineStacks), world/player management, OTA updates, MOTD editor, remote console.
- **Mod ID**: `servermanagement` | **Group**: `com.servermanagement` | **Version**: 2.1.1 | **License**: MIT
- **Architecture**: MultiLoader (buildSrc convention plugins, platform-agnostic `common/`, loader-specific implementation in `forge/`, `fabric/`).
  - *Note*: Old single-module `src/main/` is gone. Always search inside `forge/src/main/` and `fabric/src/main/` for loader-specific code.
- **Platform Abstraction**: Java ServiceLoader via `IPlatformHelper`.

## Build & Test Commands
```powershell
.\gradlew :forge:build        # Build Forge JAR
.\gradlew :fabric:build       # Build Fabric JAR
.\gradlew build               # Build all modules
```
- **Test Launcher (`test.bat`)**:
  - `test.bat client [forge|fabric]` (Client only)
  - `test.bat server [forge|fabric]` (Server only)
  - `test.bat both [forge|fabric]` (Server + Client auto-connect)
  - `test.bat debug [forge|fabric]` (Server + 2 Clients, auto-op)
- Run directories: `<loader>/runs/client/` and `<loader>/runs/server/`

## Code Conventions & Architecture
- **Naming**: Classes `PascalCase` | Methods `camelCase` | Constants `UPPER_SNAKE_CASE` | Packages `lowercase`
- **Config**: `ForgeConfigSpec` (TOML). Migration via `MigrationManager` with auto-backup to `serverdata/servermanagement/backups/`.
- **Networking**: SimpleChannel (Forge) / PlayNetworking (Fabric 1.20.1). All handlers must delegate via `execute()` to the main thread.
  - *Gotcha (Fabric Handshake)*: Unlike Forge's `SimpleChannel` which uses a single `"main"` channel, Fabric registers each packet ID individually. When using `ServerPlayNetworking.canSend()` to verify client mod installation, NEVER check for the generic Forge `"main"` channel. Always check for a core cross-loader packet ID (e.g., `com.servermanagement.network.packet.SyncSessionTokenPacket.ID`).
- **GUI**: `AbstractContainerScreen` + custom widgets, responsive layout via `ScreenScaler` (1010x570 reference).
  - *Gotcha (1.20.1 Window Resize)*: Never override `Screen.rebuildWidgets()` without a super call to avoid bypassing standard GUI initialization and scaling updates. Use a custom `refreshWidgets()` method instead.
  - *Dynamic UI*: Utilize `containerTick()` to increment a `tickCount` integer for dynamic rendering (e.g., animated `Checking for updates...` ellipses) while waiting for network callbacks.
- **GUI Styling**: Opaque `0xFF1A1A2E` bar + `0xFF333333` separator + gold `0xFFD700` title. Use ASCII only (no emojis in Minecraft fonts).
- **Chat Formatting**: Keep rigid formatting dividers (e.g., `════`) to a maximum of 30 characters. Standard Minecraft chat width is 320px; anything longer will wrap and break visual symmetry.
- **Persistence**: Gson JSON + AES-256-GCM. Async writing scheduled through daemon executor in `AsyncSaveScheduler`.
- **Logging**: Use dedicated loggers (e.g., `LoggerFactory.getLogger("ServerManagementUpdater")`) rather than generic `Constants.LOG` for independent subsystems.
- **Versioning (Self-Reflection)**: Never hardcode version strings (e.g., `"2.1.1-b01"`) into Java code or networking hooks. Always read the version dynamically via the MultiLoader `IPlatformHelper` interface. Hardcoded versions completely break manual code review testing, as "down-bumping" the `gradle.properties` or `mods.toml` will fail to propagate to the updater API requests.

---

## Token Efficiency, Self-Evolution & Session Management Rules
To ensure maximum performance, absolute context-limit safety, and continuous self-improvement, Gemini MUST adhere to these hyper-strict protocols:

1. **The "Zero-Waste" Tool Policy (Self-Evolution)**: 
   - Before executing a tool (like `search_files` or `view_file`), I must pause and calculate the token cost. 
   - If I need to search through 50 files for a specific variable, I will **NOT** use standard search tools that dump output into the chat. Instead, I will write and execute a short Bash, Python, or PowerShell script (e.g., using `grep`, `sed`, or `ast` parsing) to filter the data locally and only output the exact lines/files I need.
   - *Continuous Evolution:* I will constantly refine my own bash/python one-liners to be faster and quieter.
2. **Script-Assisted Mass Automation**: 
   - For repetitive refactoring (e.g., renaming a variable across 20 files), I am authorized and encouraged to write a quick automation script, run it via the terminal tool, and verify via compilation, rather than manually applying 20 diffs.
3. **Surgical Diffs Only**: Use `replace_file_content` or `multi_replace_file_content`. Never rewrite entire files. Target the exact line numbers.
4. **Absolute Fluff-Free Output**: I will speak only when necessary. My responses will contain the required code, the command output, or status updates. No generic pleasantries. No repetitive explanations.
5. **Token Efficiency Reminder (Hard Limit)**:
   - **Trigger**: If the chat session hits 10+ messages, includes large log dumps, or spans multiple heavy file reads, I MUST trigger the context reset protocol.
   - **Action**: Start the response with:
     > [!WARNING]
     > **TOKEN SYSTEM OVERLOAD IMMINENT**: My context buffer is degrading. Please copy the progress summary below and start a new chat session to restore my processing speed and token efficiency.
6. **Live Progress Sync**:
   - I will format summaries with ultra-compact bullet points:
     - **Done**: [Extremely compact list]
     - **Pending**: [Detailed actionable tasks]
   - I will save standard session summaries to `session_summary.md` and append them to `docs/progress.md`.
7. **STRICT UPDATE MANDATE (The "No Delay" Rule)**: 
   - I am FORCED to update every detail of `GEMINI.md` immediately after finishing EVERY task. 
   - I will not wait for the end of the session. If I discover a faster tool method, a new bug, or finish a roadmap item, I will edit the "Current Status", "Roadmap", or "Tool Policy" sections right then and there. My memory is volatile; this document is my permanent DNA.
8. **AUTO-PROCEED DENIAL COMPLIANCE (The "Stop" Rule)**:
   - When the user explicitly denies Auto-Proceed in the prompt/instructions (e.g. `AUTO-PROCEED for Implementation DENIED`), the agent MUST ONLY create or update the implementation plan, modify `GEMINI.md` to reflect the task status/roadmap/rules, and stop execution immediately.
   - The agent is strictly forbidden from writing or modifying any project source files, executing tests, or doing any implementation until the user has explicitly approved the plan in the chat.
9. **BACKGROUND TASK CHILL RULE (No Polling)**:
   - When a background task (like compilation or running a script) is started, I am **STRICTLY FORBIDDEN** from using a loop or repeatedly calling tools to check its status.
   - I must simply stop calling tools and wait. The system will automatically wake me up and send a notification when the task is finished. Constant checking wastes credits and context.
10. **TRANSPARENCY & TRUST MANDATE (For Reviewers)**:
   - Always actively encourage server administrators and manual platform reviewers to test potentially sensitive features (like the updater and script generators).
   - Document verification steps openly in `README.md` and `description.md` to build immediate trust and bypass false-positive security flags.
11. **CHANGELOG MANDATE (Branch-Specific Strategy)**:
   - I MUST automatically track EVERY SINGLE FINAL CHANGE made during the session.
   - All changelogs follow the `changelogs/CHANGELOG_v<VERSION>_<BRANCH>.md` format.
   - When up-porting or down-porting features, I MUST read the source branch's changelog, implement the changes, and record them in the TARGET branch's changelog. This prevents massive global changelogs and makes cross-porting clean and efficient.
12. **SELF-REFLECTION & DNA EVOLUTION (Branch-Aware Cross-Evolvement)**:
   - When updating `GEMINI.md` to improve it in terms of knowledge, efficiency, and reliability, I MUST determine if the lesson is *global* or *branch-specific*.
   - If **global** (e.g., universal Java patterns, UI design rules): I must write a script to iterate through the current `GEMINI.md` AND all local `*_GEMINI.md` files, injecting the new knowledge into all of them.
   - If **branch-specific** (e.g., Fabric 1.20.1 specific API quirks): I will only append this knowledge to the current active `GEMINI.md` (which will automatically receive its branch prefix via the Git hook upon switching branches).

---

## Current Status & Roadmap

### Quick Summary (Done)
- [x] Restructured `mc/1.20.1` branch onto MultiLoader convention (common, forge, fabric modules).
- [x] Backported features and aligned GUI, networking, and security code bases from version 2.1.0 to 1.20.1.
- [x] Rewrote Fabric networking for 1.20.1 (using legacy ServerPlayNetworking / ClientPlayNetworking) and established uniform IPacket registries.
- [x] Fixed Fabric missing `SessionEventHandler` hooks for `ServerPlayConnectionEvents`, which caused admin GUI components and packets to randomly fail token authentication.
- [x] Resolved custom GUI background blur shader black rendering on modern GPU drivers by pivoting to GLSL 1.50 (#version 150) under the `servermanagement` namespace.
- [x] Cleaned up Gradle cache file locks and eliminated duplicate project compilation definitions in `settings.gradle`.
- [x] Verified full compilation and built JARs for both Forge and Fabric modules using Gradle 8.11.
- [x] Restored and verified the automated multi-version `test.bat` launcher that supports auto-detecting the Minecraft version.
- [x] Overhauled the update architecture: `UpdateManager` now checks for updates asynchronously and a new `UpdateAvailableScreen` overlay intercepts the Main Menu. Fixed the `updater.jar` missing crash in development environments.
- [x] Implemented smart start detection (`-Dservermanagement.smartstart=true`) and updater UI warnings. Added ability for admins to auto-override existing run scripts with smart start backups before applying an update. Added update flag cleanup on server start. Stopped generating smart_start fallback scripts on server start, instead only generating them when the player chooses to manually override.
- [x] Cleaned up obsolete OTA architecture (`ota` packages, `ModFileTransferManager`, and related packets) and added automatic deletion of leftover `curseforge.properties` files on server start.
- [x] Fixed network handshake parity: Enforced client-side mod requirement to prevent vanilla clients or clients with mismatched loaders (e.g., Fabric clients with a Forge mod jar) from joining the server. Also fixed an issue where valid Fabric clients were rejected due to checking the Forge-specific `main` channel instead of the cross-loader `SyncSessionTokenPacket.ID`.
- [x] Added a visual `Checking for updates...` loading animation in `UpdaterScreen` utilizing the new container tick loop, and ensured the GUI automatically sends a check packet when opened empty.
- [x] Fixed updater screen (`UpdateAvailableScreen` and `OTAUpdateScreen`) layout going off-screen on narrow window sizes for both Forge and Fabric by implementing responsive vertical stacking, dynamic text width constraints, and proper resize recreation of elements.
- [x] Fixed `UpdaterScreen` and `PerformanceSettingsScreen` going off-screen on window resize by renaming `rebuildWidgets()` to `refreshWidgets()`. Overriding `Screen.rebuildWidgets()` without a super call in 1.20.1 bypassed `init()`, preventing GUI scales from updating.
- [x] Fixed `ServerUpdateScheduler` falsely triggering update notifications due to a hardcoded `"2.0.0"` base version.
- [x] Fixed mod versioning double 'b' output (`2.1.1-bb01`) by normalizing `mod_build` in `gradle.properties` against `multiloader-common.gradle`. Also resolved UTF-8 formatting issues (`â€”`) on the Forge mods screen by stripping special em-dash characters in `gradle.properties`.
- [x] Fixed `CheckForUpdatesPacket` returning a false positive when users manually click "Check for Updates" by updating the hardcoded `"2.0.0"` base version to `"2.1.1-b01"`.
- [x] Fixed the `ServerManagement Dashboard` welcome notification in chat wrapping text on standard 320px chat widths by reducing the formatting dividers (`════`) from 39 characters to 30 characters.
- [x] Fixed absurd market price calculations (e.g. $205,000,000,000,000.00) for unanchored modded reversible crafting recipes in `RecipeBasedPricing` across Forge, Fabric, and NeoForge by enforcing a 15-increase cycle breaker and a $1,000,000.00 price ceiling.
- [x] Fully integrated the SM Updater architecture into 1.21.1 across Forge, Fabric, and NeoForge, including `UpdaterMenuProvider`, `OpenGuiPacket.GuiType.UPDATER`, the `Server Updater` Admin Dashboard card, `/sm update` / `/sm update skip` commands, `UpdaterScreen` menu registration, and game-startup `TitleScreen` background update check with `UpdateAvailableScreen` auto-display.
- [x] Fixed server-side automatic update checking by wiring `ServerUpdateScheduler.start(loader, version)` into server startup for Forge, Fabric, and NeoForge. Removed leftover `ModFileTransferManager.initialize()` calls to prevent `Could not find mod JAR file for OTA updates` startup warnings.
- [x] Fixed `Network Protocol Error` client disconnect when opening standard container GUIs (including Server Updater) by changing `ModMenuTypes` in Forge and NeoForge from `IMenuTypeExtension` / `IForgeMenuType` to vanilla `MenuType<>(factory, FeatureFlags.VANILLA_SET)` for simple 2-arg menus.
- [x] Fixed `java.lang.UnsupportedOperationException: Payload servermanagement:checkforupdates_packet may not be sent to the server!` disconnect in NeoForge by registering `CheckForUpdatesPacket` (Server-bound), `StartServerUpdatePacket` (Server-bound), and `SyncUpdateInfoPacket` (Client-bound) in `ModNetworking.java`.
- [x] Downgraded OTA system join warning from `ERROR` to `DEBUG` in `PlayerJoinListener.java` across Forge, Fabric, and NeoForge to prevent console error spam.
- [x] Set casino house edge constants (`SLOT_MACHINE_HOUSE_EDGE`, `COIN_FLIP_HOUSE_EDGE`, `DICE_ROLL_HOUSE_EDGE`, `ROULETTE_HOUSE_EDGE`) to `0.0` in `GamblingManager.java` so 50/50 wins payout exact 2.0x ($200 back, $100 profit on $100 bet).
- [x] Implemented fixed item search `EditBox` at top of `MineBayScreen.java` with dynamic real-time filtering.
- [x] Implemented row double-click event handling in `ConfigScreen.java` to open detailed feature configuration GUIs, and enforced Economy system feature disable synergy blocking MineBay and MineStacks with user notification when disabled.
- [x] Updated project version to `2.1.1` in `gradle.properties` and generated fresh production JARs for Forge, Fabric, and NeoForge.
- [x] Refactored `EconomyManagementScreen` to include an advanced `TradeBlacklistEditorWidget` with multi-select (CTRL+Click), item ID autocompletion search, interruptible hold-to-delete animation, and a "Don't ask me again" confirmation prompt.
- [x] Fixed `EditBox` selection persistence and typing indication issues in `MineBayScreen` by enforcing continuous `setFocused(true)` on mouse release and keyboard input.
- [x] Migrated "Don't ask me again" settings for the trade blacklist deletion warning to a dedicated per-server `ClientConfig` (`servermanagement_client.json`), mapping server IPs to booleans to ensure safety across different servers.
- [x] Fixed Economy/MineBay/MineStacks disable toggle system across Forge, Fabric, and NeoForge: added missing `ECONOMY_ENABLED` config field, synced economy settings on player join, enforced MineBay/MineStacks config checks in `OpenGuiPacket`, and fixed command error feedback messages.
- [x] Fixed `TradeBlacklistEditorWidget` deletion issues across all loaders where the search `EditBox` would trap focus, preventing users from right-click-holding to delete or using the `DEL` key after interacting with the search box or items.
- [x] Fixed `TradeBlacklistEditorWidget` search box failing to gain focus when clicked. This was caused by the parent `Screen`'s focus bouncing logic (`setFocused(false)` then `setFocused(true)` on the widget) aggressively clearing the `EditBox`'s focus without restoring it. Solved by introducing an `isPendingSearchBoxFocus` flag to safely survive the screen focus bounce.
- [x] Fixed `TradeBlacklistEditorWidget` search results drop-down rendering underneath the blacklisted items' 3D models. Minecraft's item rendering writes to the depth buffer, causing 3D items to clip through the 2D autocomplete background. Resolved by pushing the `PoseStack` and applying a Z-axis translation (`translate(0, 0, 400)`) specifically for the autocomplete overlay to render it strictly above all depth-buffered items.
- [x] Fixed MineBay search filtering visual bugs across all loaders where UI elements (like "Details" buttons) from previous listings would persist on screen after a search update. Implemented explicit dynamic widget tracking and a `refreshBrowseWidgets()` lifecycle, allowing search results to seamlessly regenerate action buttons without triggering a full screen rebuild (which would destroy typing focus). Also added a proper "No matches found" empty state for zero-result searches.
- [x] Fixed hardcoded versioning and mod loader strings in Updater check functions (`ServerUpdateScheduler`, `CheckForUpdatesPacket`, `ClientConnectionHandler`) across all loaders. Implemented `getModVersion()` dynamically via `IPlatformHelper` and used `SharedConstants.getCurrentVersion().getName()` for the dynamic Minecraft version.
- [x] Removed duplicate update availability console log in `ServerUpdateScheduler` and fixed Dashboard Welcome Message divider breaking line limits.
- [x] Implemented admin update chat notifications in `PlayerJoinListener` across all loaders utilizing `ServerUpdateScheduler.pendingUpdate` state.
- [x] Fixed `UpdaterScreen` changelog overflow by dynamically truncating overly long changelog strings using `this.font.width()`.
- [x] Fixed blurred progress bar in `OTAUpdateScreen` caused by 1.21.1's native background blur shader by repositioning `super.render()` to the top of the render method.

### Active Blockers / Investigation Findings
- *None.* The new updater UI and standalone `updater.jar` handoff have been verified and function perfectly in-game.

### Global UI Design Rules (Lessons Learned)
- **EditBox Focus & Filtering**: When implementing real-time search filtering in GUIs, NEVER call `this.rebuildWidgets()` or `this.init()` inside the `EditBox.setResponder()` lambda. Doing so destroys the `EditBox` instance, causing it to lose focus and dropping the typing indicator mid-keystroke. Instead, only update the backing data list (e.g., `applyFilters()`) and manually refresh the visible elements.
- **Nested Widget Input Handling**: When embedding an `EditBox` (or any focusable element) inside a custom widget (e.g., `TradeBlacklistEditorWidget extends AbstractWidget`), the parent screen will NOT automatically route keyboard events to the nested element. The custom widget MUST override `mouseClicked`, `charTyped`, `keyPressed`, and `setFocused(boolean)` to explicitly forward those events to the nested `EditBox`.
- **Nested Widget Focus Management**: When embedding an `EditBox` inside a custom widget, do NOT force the `EditBox` to gain focus unconditionally when the custom widget gains focus (e.g., via `setFocused(boolean)` overrides). This will trap all keyboard events (like the `DEL` key) into the `EditBox`. Instead, only explicitly unfocus the `EditBox` when the widget loses focus or when interacting with other elements in the widget (like clicking a list item), allowing the `EditBox` to handle its own focus when directly clicked.

### Future Roadmap / Tasks for Next Update (v2.1.0 QA)
- [x] Standardize the mod name to 'ServerManagement+' and the author name across all branches (`mc/1.20.1`, `mc/1.20.1-forge`, `mc/1.21.1`, `mc/1.21.11`, `mc/1.21.4`).
- [x] Deploy v2.1.0 builds to GitHub Releases using custom automated upload utility.
- [x] Implement bifurcated, independent update architecture (Modrinth/CurseForge dual-query, standalone `updater.jar`).
- [x] Run client/server environments in-game with `test.bat` to perform quality assurance (Updater verified).
- [x] Up-port architectural changes from `mc/1.20.1` to `mc/1.21.1`.
  - [x] Up-port the updater GUI screens (`UpdaterScreen`, `OTAUpdateScreen`, `UpdateAvailableScreen`).
  - [x] Adapt networking packets to 1.21.1 CustomPayload API across all loaders.

### 1.21.1 Porting Rules (Lessons Learned)
- **Background Blur Shader (1.20+)**: In Minecraft 1.20 and above (including 1.21.1), `Screen.render()` automatically applies a background blur shader. Therefore, if you override `render()` and draw text *before* calling `super.render()`, your text will be completely blurred out. Always call `super.render()` at the very beginning of your `render()` override.
- [x] Compile and resolve 1.21.1 UI differences.
- [x] Test networking parity and data persistence between Forge and Fabric implementations.

### Performance Orchestration & Synergy Engine (Done)
- [x] **Phase 1**: Foundation & Multi-Loader Architecture (Zstd compression, Environment Registration).
- [x] **Phase 2**: Server-Side Core (Performant Pre-Generation via Spiral Matrix, MSPT Safety Valve, Dynamic Workload Scaling).
- [x] **Phase 3**: Client-Side Engine (Local Disk/RAM Caching, Fake Chunk Injection, Smart Packet Prioritization, Async Occlusion Culling).
- [x] **Phase 4**: Distant Horizons Integration (API Hooking, Tiered Rendering Pipeline, Traffic Control).

### 1.21.1 Porting Rules (Lessons Learned)
- **Networking (CustomPacketPayload)**: 1.21.1 networking mandates `CustomPacketPayload` and `StreamCodec`. Unlike legacy versions, packets are now `record` classes. Do NOT annotate `handle()` with `@Override` inside these records (especially for NeoForge), as the interface does not dictate a `handle` method; it's passed as a lambda during registration.
- **Networking (Netty / Compression)**: When implementing custom network compression algorithms (e.g. `zstd-jni`), always utilize the direct `ByteBuffer` NIO APIs (`Zstd.compress(ByteBuffer, ByteBuffer)`) instead of allocating temporary `byte[]` arrays. Minecraft's Netty pipeline relies heavily on direct memory for performance, and zero-copy byte buffers prevent extreme heap allocation spikes.
- **GUI Backgrounds**: `renderBackground(GuiGraphics)` is deprecated/removed in 1.21.1 screens. Always use `renderBackground(GuiGraphics, mouseX, mouseY, partialTicks)`.
- **NeoForge Quirks**: 
  - The `fml` package moved from `net.minecraftforge.fml` to `net.neoforged.fml`.
- **Global Keybind Interception**: When embedding EditBoxes in Screen components, always override keyPressed and charTyped to explicitly route events to searchBox.keyPressed(...) and return true before calling super.keyPressed. If not done, Screen default keybinds (like pressing "E" to close the inventory) will trigger even while the user is typing in a search bar.
- **Hold-to-Delete Tick Logic**: Custom widgets needing animation/tick logic (like hold-to-delete) MUST have their tick() method explicitly implemented AND that widget's tick() method MUST be explicitly called inside the parent screen's containerTick() method. Furthermore, always call this.setFocused(true) when interacting with elements in custom widgets to ensure keyboard/tick events properly route to them.
- **Nested Widget Focus Bouncing**: When a `Screen` processes `mouseClicked`, it temporarily clears focus (`setFocused(false)`) before setting it to the clicked widget (`setFocused(true)`). For custom widgets containing nested `EditBox`es, this focus bounce will aggressively unfocus the nested `EditBox` if it isn't properly restored. To fix this, use an `isPendingSearchBoxFocus` boolean flag that is set to true when the nested `EditBox` is clicked, and restore the `EditBox`'s focus inside the widget's `setFocused(boolean)` override if the flag is true.
- **Dynamic Version Resolution**: Never hardcode versions or loader names in the Updater or Networking code. Always use `Services.PLATFORM.getModVersion()` to read the version dynamically at runtime from the active Mod Container across loaders. Use `net.minecraft.SharedConstants.getCurrentVersion().getName()` to dynamically retrieve the current Minecraft game version.
