# Changelog — v2.0.0 (Release)

## New Features

### Economy Statistics Dashboard
- Real-time economy overview in the admin Economy Management screen (Statistics tab)
- Tracks: total accounts, money in circulation, average balance, richest player, inflation rate
- Marketplace stats: active listings, template counts
- Gambling stats: total bets, wins, amounts wagered/won
- Transaction breakdown: purchases, sales, transfers, free rewards
- Color-coded inflation indicator (green/yellow/red)
- Refresh button for live data updates

### Transaction Recording System
- All economy operations now record typed transactions: `MINEBAY_PURCHASE`, `MINEBAY_SALE`, `GAMBLING_BET`, `GAMBLING_WIN`, `FREE_REWARD`, `PLAYER_TRANSFER_SENT`, `PLAYER_TRANSFER_RECEIVED`, `TASK_REWARD`
- Bank screen displays transaction type icons and color-coded entries
- Full audit trail across all economy features

### Action Bar Notifications
- Success/error messages now display as action bar text (above hotbar) instead of chat messages
- Applied across all MineBay, gambling, overflow, and economy operations
- Consistent color scheme: green checkmarks for success, red crosses for errors, gold for [MineBay] prefix

### Overflow Inventory — Creative Mode Fix
- Fixed items being silently destroyed when added to a creative mode player's inventory
- Added `safeAddToInventory()` with pre-check for available inventory space
- Overflow items are now correctly stored for later pickup in both survival and creative mode

## Bug Fixes

### Economy Management Screen
- Fixed search box overlapping with "+ New Template" button (moved to separate positions)
- Fixed edit template not loading existing values (description, goal, reward fields were empty when editing)
- Fixed Free Reward tab Save button overlapping with tip text

### MineBay
- Fixed "Listing created" message missing [MineBay] prefix and formatting
- Fixed listing creation notification now uses action bar display

### Overflow System
- Fixed "You have no overflow items" and "All overflow items claimed" text formatting
- Added consistent gold-bordered, green-checkmark action bar messages

## Security & Performance Improvements

### Critical Fixes
- **ConcurrentModificationException prevention**: Transaction iteration in `SyncEconomyStatsPacket` now uses snapshot copies
- **Null safety**: Added null checks for `server` and `data` in stats sync to prevent crashes during shutdown
- **Synchronized singletons**: `EconomyManager.getInstance()`, `OverflowInventoryManager.getInstance()`, `MineBayManager.getInstance()`, and `TransactionManager.getInstance()` are now thread-safe
- **Gambling payout validation**: Payouts are validated against `NaN`, `Infinity`, and negative values with automatic refund on invalid results — now applied to both money bets and item bets
- **Stale player reference fix**: `PlaceGamblingBetWithItemPacket` re-looks up player by UUID in delayed gambling result callback instead of using captured reference

### Item Duplication Prevention
- **CreateListingPacket server-authoritative items**: Listing creation now reads the item from the server-side menu container instead of trusting client packet data, preventing item spoofing
- **CreateListingPacket race-condition fix**: The offering slot is cleared **before** listing creation (not after), preventing item duplication if the menu closes between listing creation and slot clearing
- **PurchaseListingPacket listing status check**: Listings are validated as `ACTIVE` before purchase and immediately marked as `COMPLETED` before payment processing, preventing double-purchase exploits
- **AcceptOfferPacket early completion**: Listing status is set to `COMPLETED` at the start of accept processing to prevent concurrent accept operations
- **HoldItemPacket item safety**: Previously held items are returned to the player before holding a new one, preventing item loss when rapidly placing different items

### Escrow & Transaction Integrity
- **CreateOfferPacket atomic escrow**: Money escrow now uses `tryWithdraw()` (atomic check-and-deduct) and the entire escrow+offer creation is wrapped in try-catch with automatic rollback on failure — money is returned if anything fails
- **BankAccount synchronized operations**: All balance-modifying methods (`deposit`, `withdraw`, `setBalance`, `tryWithdraw`) are now `synchronized` to prevent concurrent modification race conditions
- **BankAccount atomic tryWithdraw**: New `tryWithdraw()` method combines balance check and withdrawal in one synchronized operation, eliminating TOCTOU race conditions

### High Priority Fixes
- **Margin clamp**: Seller price margins clamped to -50% to +200% (was incorrectly allowing up to 500%)
- **Packet validation**: `PurchaseListingPacket` rejects invalid slot counts instead of silently truncating
- **Offer packet bounds**: `SyncListingOffersPacket` caps offer count (50) and items per offer (27) to prevent memory exhaustion
- **Negative money guard**: `CreateOfferPacket` clamps negative money offers to zero on deserialization
- **Counteroffers snapshot**: `RequestListingOffersPacket` uses snapshot copy when iterating counteroffers to prevent CME from scheduled tasks
- **NBT size limits**: `OverflowInventoryManager.load()`, `MineBayManager.load()`, `TransactionManager.load()`, and `EconomyManager.loadBankInventories()` all use 10MB NbtAccounter limits instead of unlimited heap to prevent memory exhaustion from corrupted data files
- **HoldItemPacket slot validation**: Added bounds check on slot index before accessing player inventory

### Performance Fixes
- **Overflow I/O debounce**: `OverflowInventoryManager` uses `markDirty()` with 3-second debounce instead of saving to disk on every add/claim operation

## Network Protocol
- Added `SyncEconomyStatsPacket` (server → client, 20 stat fields)
- Added `RequestEconomyStatsPacket` (client → server, admin-only with permission level 2)
- Stats auto-sync when admin opens Economy Management screen

## Version Info
- Mod version: 2.0.0
- Release type: release
- Minecraft: 1.21.1
- Forge: 52.1.0+ / NeoForge: 21.1.222+
- Java: 21+
