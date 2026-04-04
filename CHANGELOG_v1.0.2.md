# Changelog - v1.0.2 (Admin Dashboard & Economy Update)

**Release Date**: March 22, 2026  
**Previous Version**: v1.0.1  
**Minecraft Version**: 1.20.1  
**Forge Version**: 47.4.0+

---

## 🖥️ Admin Dashboard — Complete Overhaul

### New Dashboard Hub
- **Central command center** accessible via `/sm` or `/servermanagement` with a modern 400×330 dark-themed GUI
- **6 feature cards** in a responsive 3×2 grid layout:
  - World Manager, Player Manager, Console, Global Settings, Economy, Mod Settings
- **Custom widget system** powering all dashboard screens:
  - **ModernButton** — 5 color styles (Primary, Success, Danger, Secondary, Dark) with hover effects and builder pattern
  - **ToggleSwitch** — iOS-style sliding toggle with animated thumb
  - **DashboardCard** — Large clickable cards with icon, title, and description
  - **ConsoleOutput** — Scrollable output panel with color-coded log levels

### Live Server Console
- Execute server commands directly from the GUI (OP level 2 required)
- Real-time output relay via `ConsoleResponsePacket` with color-coded log levels (red for errors, yellow for warnings)
- Scrollable history with mouse wheel support, Send and Clear buttons
- Compact back-arrow navigation in the header bar

### Global Settings Screen
- Complete redesign with modern dark theme (320×200)
- Toggle switches for **Chat Isolation** and **Tab Isolation**
- Settings synced via `SyncGlobalSettingsPacket` for responsive UI

### Navigation System
- Consistent packet-based navigation across all screens (server validates permissions)
- Every screen has ← Dashboard / ← Back and Close buttons in standard positions
- `OpenGuiPacket` extended with `CONSOLE`, `DASHBOARD`, and `ECONOMY` GuiTypes
- Security: bounds checking on GuiType ordinals, `isAdminOnly()` permission gate

---

## 💰 Economy Management System

### Task Template CRUD
- Full create, read, update, and delete workflow for daily task templates
- **6 task types**: Break Blocks, Kill Mobs, Travel Distance, Craft Items, Mine Ores, Trade Villagers
- Configurable goal amounts and monetary rewards per template
- Enable/disable individual templates without deleting them
- Scrollable template list with search functionality
- Template data persists via encrypted JSON storage

### Item Reward Selection
- Admins can assign **item rewards** to task templates and the free daily reward
- Select an item in your hotbar and click the item slot in the GUI to assign it
- Items are serialized over the network via `SaveTemplatePacket` and `SyncEconomyTemplatesPacket`
- Reward items displayed in the template list alongside monetary rewards
- Item data round-trips correctly through NBT serialization on save/sync

### Free Daily Reward Configuration
- Configure reward amount ($), cooldown period (hours), and an optional reward item
- Settings saved via `SaveFreeRewardSettingsPacket` with item support
- Players can claim once per configured cooldown period

### Economy Network Packets (New)
- `SaveTemplatePacket` — Create/update templates with item rewards
- `DeleteTemplatePacket` — Remove templates by ID
- `ToggleTemplatePacket` — Enable/disable templates
- `SyncEconomyTemplatesPacket` — Server→client full template sync with ItemStack data
- `SaveFreeRewardSettingsPacket` — Save free reward amount, cooldown, and item

---

## 🎨 GUI Visual Fixes

### Header Visibility
- Fixed faint/ghostly header text across **7 dashboard screens**: DashboardScreen, WorldListScreen, WorldDetailScreen, PlayerManagerScreen, ConsoleScreen, GlobalSettingsScreen, ConfigScreen
- Headers now use opaque dark background (`0xFF1A1A2E`) instead of semi-transparent overlays
- Gold title text (`0xFFD700`) with drop shadow for readability
- Consistent `0xFF333333` separator line below all headers

### Economy Screen Layout
- Fixed "Edit Template" title overlapping with "Task Type:" label (both were rendered at the same Y position)
- Search box and "+ New Template" button now hidden during edit mode (previously visible behind the edit form)
- Free Reward tab: increased vertical spacing between title, input labels, item slot, and Save button to prevent overlapping
- Save button moved below the item slot area to prevent visual collision
- Edit form background box resized to properly contain all form elements

### Widget Rendering
- Fixed text rendering behind widgets by correcting the draw order: background → custom text → widget layer → tooltips
- Applied consistent rendering order across all 9 GUI screens
- Replaced unsupported emoji icons in DashboardCard and TaskType with ASCII equivalents (`*`, `!`, `>`, `+`, `#`, `$`)

---

## 🐛 Bug Fixes

### Player Manager Ghost Artifacts
- Fixed duplicate/ghost widgets appearing when switching between player list and detail views
- Replaced `clearWidgets()` + `init()` pattern with `rebuildWidgets()` to properly handle widget lifecycle

### Console Button/Input Overlap
- Console command input bar and Dashboard back button no longer overlap
- Dashboard navigation moved to a compact back arrow in the header bar
- Input field repositioned with proper spacing

### MineStacks Casino Fixes
- **Balance rounding**: Fixed floating-point display issues using `Math.round()` for clean integer display
- **16-bit ContainerData truncation**: Fixed balance values >32,767 being corrupted by using 2-slot encoding (high/low bits) in ContainerData
- **Bet timing**: Fixed immediate balance deduction on bet placement — balance now only updates when results arrive

### MineBay Marketplace Fixes
- **Item loss prevention**: Items in the offering slot are now returned to inventory when closing the GUI without creating a listing
- **Create Listing**: Fully implemented the listing creation flow with validation (item placed + price set)
- **Navigation**: Fixed navigation state transitions after listing creation

### GUI State Synchronization
- Fixed all feature toggles showing "ON" regardless of actual config state when opening the management GUI
- Added `syncFeatureStates()` call before GUI opens to push fresh server state to client

### Portal Toggle State Bug
- Fixed toggle sending stale state on repeated clicks (second click would send the same value as the first)
- Toggle switches now update `ClientPacketHandler` cache immediately; each click reads fresh state

---

## 🔄 OTA Update System Improvements

### Build Number Versioning
- New `ota.properties` file with structured version metadata: version, build number, release type, release notes
- Smart version comparison: semantic version check first, then build number comparison for same-version updates
- Renaming JAR files no longer bypasses the update check
- Backward compatible with old version strings

### Version Parser Fix
- Fixed `parseVersionPart()` not stripping leading non-numeric prefixes (e.g., `v` in `v1.0.2`)
- The parser now strips both leading prefixes and trailing suffixes, so version strings like `v1.0.2-release` are correctly parsed as `1.0.2`
- **Note**: Clients running v1.0.1 or earlier still have the old parser — they will receive the update via OTA, which includes this fix for future upgrades

---

## 📦 New Files Summary

### GUI Widgets
- `ModernButton.java` — Styled button with 5 themes and builder pattern
- `ToggleSwitch.java` — Animated iOS-style toggle switch
- `DashboardCard.java` — Feature card widget for dashboard grid
- `ConsoleOutput.java` — Scrollable color-coded console output panel

### Screens
- `DashboardScreen.java` — Central admin hub with 6 feature cards
- `EconomyManagementScreen.java` — Economy template and reward management
- `EconomyManagementMenu.java` — Container menu for economy screen

### Network Packets
- `ConsoleResponsePacket.java` — Relays console output to client
- `SyncGlobalSettingsPacket.java` — Syncs chat/tab isolation settings
- `SaveTemplatePacket.java` — Create/update economy templates with items
- `DeleteTemplatePacket.java` — Delete economy templates
- `ToggleTemplatePacket.java` — Toggle template enabled state
- `SyncEconomyTemplatesPacket.java` — Full template sync with item rewards
- `SaveFreeRewardSettingsPacket.java` — Save free reward configuration

### Server Components
- `ConsoleCommandListener.java` — Captures command output and relays via packet
- `OTAVersion.java` — Build number version comparison system
- `ota.properties` — Version metadata file

### Client Data
- `ClientPacketHandler.java` — Extended with economy template cache, global settings cache, and world detail cache

---

## 🔧 Technical Notes

- All new packets follow the `IPacket` interface pattern with matching encode/decode field order
- Packet handlers use `ctx.get().enqueueWork(...)` for thread safety
- Admin-only packets validate `player.hasPermissions(2)` before processing
- ItemStack serialization uses Forge's built-in `FriendlyByteBuf.writeItem()`/`readItem()`
- Client caches are updated optimistically for responsive UI, then confirmed by server sync packets
- GUI screens read exclusively from `ClientPacketHandler` caches, never directly from server state
