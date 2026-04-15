# Changelog - v1.0.4-b01 NeoForge (Dynamic Economy & MineBay Overhaul)

**Release Date**: April 9, 2026
**Previous Version**: v1.0.3-b05 (NeoForge)
**Minecraft Version**: 1.21.1
**Mod Loader**: NeoForge
**NeoForge Version**: 21.1.222+
**OTA Build**: 01
**JAR File**: `servermanagementplus-v1.0.4-b01-mc1.21.1-neoforge-pre-release.jar`
**Branch**: `mc/1.21.1-neoforge`

> **Forge users**: See [CHANGELOG_v1.0.4.md](CHANGELOG_v1.0.4.md) for the Forge-specific changelog.

---

## 📈 Dynamic Market Pricing System

### MarketPricingEngine (New)
- **Real-time market prices** for all items, dynamically calculated based on server economy state
- **Inflation multiplier** — prices scale with average player wealth and active player count: `inflationMultiplier = (avgBalance / starterMoney) * (1 + log10(max(playerCount, 1)))`
- **Supply factor integration** — item prices rise when scarce, fall when oversupplied
- **Static base values** — 40+ items have hand-tuned base prices; unlisted items default to $1
- **Server-to-client sync** — market prices are broadcast to clients via `SyncMarketPricesPacket` so GUIs always show current values

### Smart Payment Processing
- `PurchaseListingPacket` now calculates the correct money amount based on live market prices
- Overpayment is automatically refunded to the buyer
- Partial item + money payments are supported and correctly balanced

### Margin System
- Sellers set a **margin percentage** on top of the market base price (default: 10%)
- Margin is clamped between -50% and +200% to prevent abuse
- Final listing price = `baseMarketPrice * (1 + marginPercent / 100)`

---

## 📦 Supply & Demand Tracking

### ItemSupplyDemandTracker (New)
- **Server-wide item supply tracking** via NeoForge event handlers:
  - `BlockEvent.BreakEvent` — tracks blocks mined
  - `ItemEntityPickupEvent.Post` — tracks items picked up (NeoForge: replaces `PlayerEvent.ItemPickupEvent`)
  - `PlayerEvent.ItemCraftedEvent` — tracks items crafted
  - `PlayerEvent.ItemSmeltedEvent` — tracks items smelted
- **Supply factor formula**:
  - Below baseline (500): scarcity bonus up to +20% price increase
  - Above baseline: `1.0 / (1.0 + log10(supplyCount / 500))` — gradual price decrease
- **Automatic decay** — 0.5% supply decay per save cycle prevents runaway deflation
- **Persistent storage** — binary format at `servermanagement/supply_demand.dat`
- Integrated into `MarketPricingEngine` for real-time price adjustments

---

## 📊 Margin History Tracking

### MarginHistoryTracker (New)
- **Logs every listing's margin** for future analytics and pricing intelligence
- Stores per-item: margin percentage, money price, base market price, timestamp, seller UUID
- **Rolling history** — retains up to 100 entries per item (oldest entries pruned)
- **Analytics methods**: `getAverageMargin(itemId)`, `getGlobalAverageMargin()`, `getEntryCount(itemId)`
- **Persistent storage** — binary format at `servermanagement/margin_history.dat`
- Periodic auto-save alongside economy data using `ServerLifecycleHooks.getCurrentServer()`

---

## 🛒 MineBay Marketplace — UX Overhaul

### Step Order Swap
- **Step 1** is now **Place Item to Sell** (was previously Set Prices)
- **Step 2** is now **Set Prices** (was previously Place Item)
- Players place items first, then configure pricing — a more natural workflow

### Auto-Populated Pricing
- The **Price ($)** field auto-fills with the current market base value for the placed item
- The **Margin %** field defaults to **10%** on new listings
- Sellers can adjust both values — the final price updates in real time

### Price Tooltips
- Hovering over any item in MineBay now shows the **market price per unit** and **total stack value**
- Tooltips appear on all inventory items and listing items

### Inventory Value Overlay
- A **total inventory value pill** is displayed above the player's inventory when visible
- Shows the combined market value of all items in the player's inventory (e.g., "Inventory Value: $1,234.56")

### Listing Display Fix
- Listing previews now show **item quantity** (e.g., "64x Dark Oak Log" instead of "Dark Oak Log")
- Fixed in both Step 2 (Set Prices) and Step 3 (Confirmation)

---

## ⚡ Performance Settings GUI

### New Performance Screen
- Accessible from **Mod Settings → Performance** in the admin dashboard
- Displays real-time server performance metrics
- Toggle performance monitoring features from the GUI

### Chat Commands
- `/smperformance` (`/smperf`) — View current performance status and stats
  - `status` — Show enabled/disabled state of all performance features
  - `stats` — Show current TPS, entity counts, and metrics
  - `reset` — Reset all tracked metrics
  - `toggle <feature>` — Enable or disable a specific performance feature
  - `set <feature> <value>` — Set a numeric performance parameter
  - `gui` — Open the Performance Settings GUI

---

## 🎨 Visual & UI Fixes

### Drop Shadow Fix
- Enabled text drop shadows on **all GUI label text** across ConfigScreen, GlobalSettingsScreen, PerformanceSettingsScreen, and PortalTimerScreen
- Improves readability on all screen backgrounds

### Economy Management Screen
- Fixed visual layout bugs in the Economy Management admin panel
- Corrected overlapping elements and spacing issues

### MineBay Visual Fixes
- Fixed dark/unreadable text on MineBay browse and listing screens
- Fixed text brightness inconsistencies
- Fixed empty state layout — text and animations no longer clip over the Create button

### MineStacks Casino
- Fixed animation clipping on slot machine and other game screens
- Corrected z-ordering so animations render above background elements
- Improved animation layering for smoother visual presentation

---

## 🔧 NeoForge-Specific Technical Changes

### NeoForge Event API Adaptations
| Feature | Forge Event | NeoForge Event |
|---|---|---|
| Item pickup tracking | `PlayerEvent.ItemPickupEvent` | `ItemEntityPickupEvent.Post` |
| Entity tick throttling | `LivingEvent.LivingTickEvent` | `EntityTickEvent.Pre` |
| Mob spawn limiting | `MobSpawnEvent.FinalizeSpawn` | `FinalizeSpawnEvent` |
| Server tick / TPS | `TickEvent.ServerTickEvent` | `ServerTickEvent.Post` |

- `ItemEntityPickupEvent.Post`: uses `event.getPlayer()` (was `getEntity()`) and `event.getOriginalStack()` (was `getStack()`)
- `ServerTickEvent.Post` has no `getServer()` method — uses `ServerLifecycleHooks.getCurrentServer()` instead
- All server lifecycle hooks use `net.neoforged.neoforge.server.ServerLifecycleHooks`

### New Packet Registrations (NeoForge `PayloadRegistrar` format)
| Packet | Direction | Purpose |
|---|---|---|
| `SyncMarketPricesPacket` | S→C | Broadcasts live market prices to clients |
| `SyncPerformanceSettingsPacket` | S→C | Syncs performance settings to client GUI |
| `UpdatePerformanceSettingPacket` | C→S | Client sends performance toggle/value change |

### New Files
| File | Purpose |
|---|---|
| `MarketPricingEngine.java` | Server-side dynamic price calculation engine |
| `ClientMarketData.java` | Client-side market price cache |
| `ItemSupplyDemandTracker.java` | Server-wide item supply tracking (NeoForge events) |
| `MarginHistoryTracker.java` | Per-item margin history with analytics |
| `SyncMarketPricesPacket.java` | Server→client market price synchronization |
| `SyncPerformanceSettingsPacket.java` | Server→client performance settings sync |
| `UpdatePerformanceSettingPacket.java` | Client→server performance setting update |
| `ServerPerformanceFeature.java` | Feature interface implementation for performance |
| `ServerPerformanceManager.java` | TPS tracking and auto-optimize logic |
| `TpsMonitorHandler.java` | `ServerTickEvent.Post` TPS measurement |
| `ItemMergeHandler.java` | Entity item merging throttle |
| `MobSpawnLimiterHandler.java` | `FinalizeSpawnEvent` spawn cap enforcement |
| `EntityActivationRangeHandler.java` | `EntityTickEvent.Pre` activation range limit |
| `RedstoneThrottleHandler.java` | Redstone update rate throttling |
| `VillagerThrottleHandler.java` | `EntityTickEvent.Pre` villager tick throttle |
| `PerformanceSettingsScreen.java` | GUI screen for performance settings |
| `PerformanceSettingsMenu.java` | Container menu for performance settings |
| `PerformanceSettingsMenuProvider.java` | Menu provider for performance settings |

### Modified Files
| File | Changes |
|---|---|
| `MineBayScreen.java` | Step swap, price tooltips, inventory value overlay, auto-populate fields, listing count fix |
| `CreateListingPacket.java` | Smart payment, margin system, margin history recording (NeoForge format) |
| `PurchaseListingPacket.java` | Market-price-aware payment processing with refunds (NeoForge format) |
| `SyncMineBayListingsPacket.java` | Market pricing field serialization (NeoForge format) |
| `MineBayListing.java` | Added market pricing fields; fixed `net.neoforged.neoforge.server.ServerLifecycleHooks` |
| `MineBayManager.java` | Fixed `net.neoforged.neoforge.server.ServerLifecycleHooks` |
| `EconomyManager.java` | Loads/shuts down MarketPricingEngine, ItemSupplyDemandTracker, MarginHistoryTracker |
| `EconomyServerHandler.java` | Periodic save for supply/demand and margin history via `ServerLifecycleHooks.getCurrentServer()` |
| `LoginNotificationHandler.java` | Updated to NeoForge event imports (`@EventBusSubscriber`, `Bus.GAME`) |
| `ModNetworking.java` | Registered 3 new packets (SyncMarketPrices, SyncPerformanceSettings, UpdatePerformanceSetting) |
| `ModMenuTypes.java` | Added `PERFORMANCE_SETTINGS_MENU` `DeferredHolder` |
| `ClientSetup.java` | Registered `PerformanceSettingsScreen` |
| `FeatureRegistry.java` | Added `ServerPerformanceFeature` registration |
| `ModConfig.java` | Added 18 server performance config fields using `ModConfigSpec` |
| `ModCommands.java` | Added `/smperformance` and `/smperf` commands; fully-qualified `com.servermanagement.config.ModConfig` to avoid conflict with `net.neoforged.fml.config.ModConfig` |
| `OpenGuiPacket.java` | Added `PERFORMANCE_SETTINGS` to `GuiType` enum and handler case |
| `ConfigScreen.java` | Added Performance settings button |
| `DashboardScreen.java` | Text drop shadow fixes |
| `GlobalSettingsScreen.java` | Text drop shadow fixes |
| `PortalTimerScreen.java` | Text drop shadow fixes |
| `EconomyManagementScreen.java` | Visual layout fixes |
| `MineStacksScreen.java` | Animation z-ordering fix |
| `gradle.properties` | `mod_version=1.0.4`, `mod_build=01` |
| `ota.properties` | Updated release notes and previous version (`1.0.3-b05`) |

### New Data Files
| File | Format | Contents |
|---|---|---|
| `servermanagement/supply_demand.dat` | Binary | Server-wide item supply counts |
| `servermanagement/margin_history.dat` | Binary | Per-item margin history entries |

---

## 📋 Build Info

- **Version**: `v1.0.4-b01-mc1.21.1-neoforge-pre-release`
- **Java**: 21
- **NeoForge**: 21.1.222
- **Minecraft**: 1.21.1
- **Commit**: `d680140`
