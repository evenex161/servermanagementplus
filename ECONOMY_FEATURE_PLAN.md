# Economy Feature Implementation Plan

## Overview
Complete money system with bank accounts, achievement rewards, daily tasks, and marketplace.

## Core Components

### 1. Economy Manager (`EconomyManager.java`)
- Singleton pattern
- Manages all bank accounts
- Tracks transactions
- Handles daily task generation
- Manages marketplace listings

### 2. Bank Account System
**BankAccount.java**:
- UUID playerId
- double balance
- List<Transaction> transactionHistory (max 50)
- DailyTasks dailyTasks
- long lastFreeRewardClaim
- long dailyTasksLastRefresh

**Transaction.java**:
- TransactionType (ACHIEVEMENT, PLAYER_TRANSFER, PLAYER_REQUEST, DAILY_TASK, MARKETPLACE, FREE_REWARD)
- double amount
- String description
- long timestamp
- UUID otherPlayer (for transfers/requests)

### 3. Achievement Reward System
**AchievementListener.java** (Event Handler):
- Listen to advancement events
- Calculate reward based on rarity
- Support modded achievements

**Reward Calculation**:
- COMMON: $50-100
- UNCOMMON: $150-300
- RARE: $400-700
- EPIC: $1000-2000
- LEGENDARY: $3000-5000

### 4. Daily Tasks System
**DailyTasks.java**:
- 3 unique tasks per player
- 1 free reward (no task required)
- Individual 24h cooldown per player
- Server-wide refresh at midnight (server time)

**Task Types**:
- Break X blocks of type Y
- Kill X mobs of type Y
- Travel X blocks
- Craft X items of type Y
- Mine X ores
- Trade with villagers X times

**DailyTask.java**:
- TaskType
- int progress
- int required
- double reward
- String description
- boolean completed

### 5. Money Transfer System
**Packets**:
- SendMoneyPacket(receiver, amount)
- RequestMoneyPacket(from, amount)
- AcceptMoneyRequestPacket(requestId)
- DenyMoneyRequestPacket(requestId)

**MoneyRequest.java**:
- UUID id
- UUID from (who requests)
- UUID to (who pays)
- double amount
- long timestamp
- RequestStatus (PENDING, ACCEPTED, DENIED, EXPIRED)

### 6. Marketplace System
**MarketplaceListing.java**:
- UUID listingId
- UUID sellerId
- ItemStack itemForSale
- ItemStack requestedItem (optional)
- double requestedMoney (optional)
- long timestamp
- ListingStatus (ACTIVE, SOLD, CANCELLED, EXPIRED)

**MarketplacePackets**:
- CreateListingPacket
- BuyListingPacket
- CancelListingPacket
- BrowseListingsPacket

### 7. GUI Screens

**BankDashboardScreen.java**:
- Display balance prominently
- Show recent transactions (scrollable list)
- Buttons: Send Money, Request Money, Daily Tasks, Marketplace, Close

**SendMoneyScreen.java**:
- Player selector (online players)
- Amount input
- Confirm button

**RequestMoneyScreen.java**:
- Player selector
- Amount input
- Message/reason input
- Request button

**DailyTasksScreen.java**:
- Display 3 daily tasks with progress bars
- Show free reward with cooldown timer
- Claim buttons for completed tasks

**MarketplaceScreen.java**:
- List view: All active listings
- Buttons: Create Listing, View My Listings, Filter
- Listing details: Item icon, price, seller name
- Buy button

**CreateListingScreen.java**:
- Item selection from inventory
- Price input (money/item/both)
- Preview
- List button

### 8. Commands
**/bank** - Opens Bank Dashboard GUI
**/bank balance [player]** - Check balance (own or others if OP)
**/bank pay <player> <amount>** - Send money via command
**/bank admin give <player> <amount>** - Admin: Give money
**/bank admin take <player> <amount>** - Admin: Take money
**/bank admin reset <player>** - Admin: Reset bank account

### 9. Data Persistence
**EconomyData.java**:
- Save/load all bank accounts
- Save/load all money requests
- Save/load marketplace listings
- JSON format

## Implementation Order

1. ✅ Portal Timer Enhancement (in progress)
2. Create Economy core data structures
3. Create EconomyManager with basic operations
4. Implement Achievement listener
5. Create Bank GUI
6. Implement Daily Tasks system
7. Create Marketplace system
8. Add all commands
9. Create networking packets
10. Testing & balancing

## File Structure
```
features/
├── economy/
│   ├── EconomyManager.java
│   ├── EconomyData.java
│   ├── BankAccount.java
│   ├── Transaction.java
│   ├── TransactionType.java
│   ├── DailyTasks.java
│   ├── DailyTask.java
│   ├── TaskType.java
│   ├── MoneyRequest.java
│   ├── RequestStatus.java
│   ├── MarketplaceListing.java
│   ├── ListingStatus.java
│   └── AchievementListener.java
gui/screen/
├── BankDashboardScreen.java
├── SendMoneyScreen.java
├── RequestMoneyScreen.java
├── DailyTasksScreen.java
├── MarketplaceScreen.java
└── CreateListingScreen.java
network/packet/
├── SendMoneyPacket.java
├── RequestMoneyPacket.java
├── AcceptMoneyRequestPacket.java
├── DenyMoneyRequestPacket.java
├── CreateListingPacket.java
├── BuyListingPacket.java
├── CancelListingPacket.java
└── SyncEconomyDataPacket.java
```

## Notes
- All money operations are double precision (supports cents)
- Transaction history limited to last 50 entries
- Daily tasks refresh at midnight server time OR 24h after last claim
- Marketplace listings expire after 7 days
- Money requests expire after 24 hours
- Achievement rewards scale with difficulty (detect via advancement display frame)
