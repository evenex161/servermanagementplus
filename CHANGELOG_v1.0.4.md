# Changelog - v1.0.4-b01 (Dynamic Economy & MineBay Overhaul)

**Release Date**: April 6, 2026  
**Previous Version**: v1.0.3-b05  
**Minecraft Version**: 1.21.1  
**Forge Version**: 52.1.0+  
**Release Type**: Pre-release (Beta)

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

## 📊 Supply & Demand Tracking

### ItemSupplyDemandTracker (New)
- **Server-wide item supply tracking** via Forge event handlers:
  - `BlockEvent.BreakEvent` — tracks blocks mined
  - `PlayerEvent.ItemPickupEvent` — tracks items picked up
  - `PlayerEvent.ItemCraftedEvent` — tracks items crafted
  - `PlayerEvent.ItemSmeltedEvent` — tracks items smelted
- **Supply factor formula**:
  - Below baseline (500): scarcity bonus up to +20% price increase
  - Above baseline: `1.0 / (1.0 + log10(supplyCount / 500))` — gradual price decrease
- **Automatic decay** — 0.5% supply decay per save cycle prevents runaway deflation
- **Persistent storage** — binary format at `servermanagement/supply_demand.dat`
- Integrated into `MarketPricingEngine` for real-time price adjustments

---

## 📝 Margin History Tracking

### MarginHistoryTracker (New)
- **Logs every listing's margin** for future analytics and pricing intelligence
- Stores per-item: margin percentage, money price, base market price, timestamp, seller UUID
- **Rolling history** — retains up to 100 entries per item (oldest entries pruned)
- **Analytics methods**: `getAverageMargin(itemId)`, `getGlobalAverageMargin()`, `getEntryCount(itemId)`
- **Persistent storage** — binary format at `servermanagement/margin_history.dat`
- Periodic auto-save alongside economy data

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

## ⚙️ Performance Settings GUI

### New Performance Screen
- Accessible from **Mod Settings → Performance** in the admin dashboard
- Displays real-time server performance metrics
- Toggle performance monitoring features from the GUI

### Chat Commands
- `/smmetrics` — View current performance metrics
- `/smmetrics reset` — Reset all tracked metrics

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

## 🔧 Technical Changes

### New Files
| File | Purpose |
|---|---|
| `MarketPricingEngine.java` | Server-side dynamic price calculation engine |
| `ClientMarketData.java` | Client-side market price cache |
| `ItemSupplyDemandTracker.java` | Server-wide item supply tracking with Forge events |
| `MarginHistoryTracker.java` | Per-item margin history with analytics |
| `SyncMarketPricesPacket.java` | Server→client market price synchronization |

### Modified Files
| File | Changes |
|---|---|
| `MineBayScreen.java` | Step swap, price tooltips, inventory value overlay, auto-populate fields, listing count fix |
| `CreateListingPacket.java` | Smart payment, margin system, margin history recording |
| `PurchaseListingPacket.java` | Market-price-aware payment processing with refunds |
| `MineBayListing.java` | Added market pricing fields (baseMarketPrice, marginPercent, calculatedPrice) |
| `EconomyManager.java` | Loads/shuts down MarketPricingEngine, ItemSupplyDemandTracker, MarginHistoryTracker |
| `EconomyServerHandler.java` | Periodic save for supply/demand data and margin history |
| `ModNetworking.java` | Registered SyncMarketPricesPacket |
| `PerformanceSettingsScreen.java` | New GUI with toggle switches and metrics display |
| `ConfigScreen.java` | Added Performance settings button |

### New Data Files
| File | Format | Contents |
|---|---|---|
| `servermanagement/supply_demand.dat` | Binary | Server-wide item supply counts |
| `servermanagement/margin_history.dat` | Binary | Per-item margin history entries |

---

## 📦 Build Info

- **Version**: `v1.0.4-b01-mc1.21.1-forge-release`
- **Java**: 21
- **Forge**: 52.1.0
- **Minecraft**: 1.21.1
