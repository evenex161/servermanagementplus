# Player Notification & Daily Task Management System

## Overview
Comprehensive notification system with clickable chat messages and admin daily task template management. Players receive real-time notifications for economy events, and admins can fully configure the daily task system.

## Feature 1: Clickable Chat Notifications

### NotificationManager (`com.servermanagement.features.economy.notifications.NotificationManager`)
**Purpose**: Send formatted, clickable chat messages that open GUIs

**Notification Types**:

1. **Admin Dashboard Notification** (`sendAdminDashboardNotification`)
   - Beautiful formatted header with gold borders
   - "⚙ ServerManagement Dashboard" title
   - Clickable "[Open Dashboard]" button
   - Command: `/servermanagement dashboard`
   - Shown FIRST to all OP players on login

2. **Free Reward Notification** (`sendFreeRewardNotification`)
   - "💰 You have a FREE REWARD available!"
   - Clickable "[Claim Now]" button
   - Opens daily tasks GUI

3. **Daily Tasks Notification** (`sendDailyTasksNotification`)
   - Shows count of unclaimed/unfinished tasks
   - "📋 You have X completed tasks to claim!"
   - Clickable "[View Tasks]" button

4. **Balance Update Notification** (`sendBalanceUpdateNotification`)
   - Shows change amount (+ $X or - $X)
   - Includes reason for change
   - Clickable "[View Balance]" button

5. **Payment Notifications**
   - `sendPaymentNotification`: "💸 Paid $X to PlayerName"
   - `sendReceivedPaymentNotification`: "💰 Received $X from PlayerName"
   - Both with clickable balance view

6. **Task Completed Notification** (`sendTaskCompletedNotification`)
   - "✓ Task Complete: Description"
   - Clickable "[Claim Reward]" button
   - Sent immediately when task completes

7. **Reward Claimed Notification** (`sendRewardClaimedNotification`)
   - "🎁 Reward Claimed: $X"
   - Clickable balance view

**Color Coding**:
- 💰 Gold: Money/rewards
- 📋 Aqua: Tasks/info
- ✓ Green: Success/completion
- 💸 Yellow: Payments
- ⚙ Aqua/Gold: Admin features

**Hover Text**: All clickable elements show helpful hover text

## Feature 2: Login Notification System

### LoginNotificationHandler (`com.servermanagement.features.economy.notifications.LoginNotificationHandler`)
**Purpose**: Automatically notify players of important events on login

**Login Sequence**:

**For OP Players**:
1. **Admin Dashboard** notification (ALWAYS FIRST)
2. Free reward notification (if available)
3. Daily tasks notification (if any unclaimed/unfinished)

**For Normal Players**:
1. Free reward notification (if available)
2. Daily tasks notification (if any unclaimed/unfinished)

**Smart Counting**:
- Counts unclaimed completed tasks
- Counts unfinished tasks
- Shows appropriate message based on status

**No Spam**:
- Bank balance NOT shown on login (only on actual changes)
- Only shows relevant notifications

## Feature 3: Real-Time Task Completion Notifications

### Enhanced DailyTaskProgressListener
**Automatic Notifications**: When a task is completed, player receives immediate notification

**Tracking**:
- Block breaking → Notification when goal reached
- Ore mining → Notification when goal reached
- Mob killing → Notification when goal reached
- Item crafting → Notification when goal reached
- Villager trading → Notification when goal reached
- Travel distance → Notification when goal reached

**Implementation**:
```java
String completedTask = manager.addProgress(playerUUID, TaskType.BREAK_BLOCKS, 1);
if (completedTask != null) {
    NotificationManager.sendTaskCompletedNotification(player, completedTask);
}
```

## Feature 4: Enhanced Bank Commands with Notifications

### Modified Commands:

**`/bank pay <player> <amount>`**
- Sender receives: `sendPaymentNotification`
- Receiver receives: `sendReceivedPaymentNotification`
- Both clickable to view balance

**`/bank dailies claim <1-3>`**
- Receives: `sendRewardClaimedNotification`
- Shows amount claimed with balance access

**`/bank dailies free`**
- Receives: `sendRewardClaimedNotification`
- Shows free reward amount

## Feature 5: Daily Task Template System

### DailyTaskTemplate (`com.servermanagement.features.economy.DailyTaskTemplate`)
**Purpose**: Admin-configurable task templates

**Properties**:
- `id`: Unique identifier (UUID)
- `type`: TaskType (BREAK_BLOCKS, KILL_MOBS, etc.)
- `targetAmount`: Goal to reach
- `rewardAmount`: Money reward
- `customDescription`: Optional custom text
- `enabled`: Can be disabled without deleting

**Methods**:
```java
String getDescription()              // Custom or auto-generated
DailyTask createTask()              // Generate player task from template
boolean isValid()                   // Validate required fields
```

**Auto-Generation**: If custom description is empty, generates based on type:
- "Break 100 blocks"
- "Kill 20 mobs"
- "Travel 1000 blocks"
- etc.

### DailyTaskTemplateManager (`com.servermanagement.features.economy.DailyTaskTemplateManager`)
**Purpose**: Manage template pool and configuration

**Features**:
- **Encrypted Storage**: Templates saved with `SecureDataStorage`
- **Default Templates**: 13 default templates on first run
- **CRUD Operations**: Create, Read, Update, Delete templates
- **Random Selection**: Picks 3 random templates for player dailies
- **Free Reward Config**: Configurable amount and cooldown

**Key Methods**:
```java
List<DailyTaskTemplate> getAllTemplates()           // Get all templates
List<DailyTaskTemplate> getEnabledTemplates()       // Only enabled ones
DailyTaskTemplate getTemplate(String id)            // Get by ID
DailyTaskTemplate addTemplate(template)             // Add new
boolean updateTemplate(String id, template)         // Update existing
boolean deleteTemplate(String id)                   // Delete
List<DailyTaskTemplate> selectRandomTemplates(3)    // Pick for players
double getFreeRewardAmount()                        // Get/set free reward
void setFreeRewardAmount(double)
int getFreeRewardCooldownHours()                    // Get/set cooldown
void setFreeRewardCooldownHours(int)
Map<String, Integer> getStatistics()                // Get stats
```

**Default Templates** (13 templates):
- BREAK_BLOCKS: 100, 250, 500 blocks
- KILL_MOBS: 20, 50 mobs
- TRAVEL_DISTANCE: 1000, 5000 blocks
- CRAFT_ITEMS: 50, 100 items
- MINE_ORES: 30, 75 ores
- TRADE_VILLAGERS: 10, 25 trades

**Rewards**: Range from $75 to $250

## Feature 6: Admin Commands

### `/servermanagement dashboard`
**Purpose**: Open main admin dashboard GUI

**Access**: OP level 2 required

**Usage**:
- Click notification link on login
- Type command directly
- Shorthand: `/sm`

### `/servermanagement resetdailies`
**Purpose**: Force reset all player daily tasks immediately

**Access**: OP level 2 required

**What it does**:
- Generates new tasks for ALL players
- Resets free reward claim status
- Saves changes
- Reports count of affected players

**Output**: "Reset daily tasks for X players"

**Use Cases**:
- Server events (start daily challenges)
- Fix broken tasks
- Special occasions (holidays)

## Feature 7: Enhanced Daily System

### Template-Based Generation
**Old System**: Random generation from TaskType enums
**New System**: Selects from admin-configured template pool

**Process**:
1. Daily system calls `templateManager.selectRandomTemplates(3)`
2. Gets 3 random ENABLED templates
3. Each template creates a `DailyTask` with configured values
4. Tasks assigned to player

**Fallback**: If no templates available, uses old random generation

### Modified DailyTasksManager

**New Methods**:
```java
void setTemplateManager(DailyTaskTemplateManager)   // Link template system
int forceResetAllDailies()                          // Reset all players
boolean forceResetPlayerDailies(UUID)               // Reset specific player
```

**Enhanced Progress Tracking**:
```java
String addProgress(UUID, TaskType, int)             // Returns completed task description
```
- Returns `null` if task not completed
- Returns task description if JUST completed
- Used to trigger completion notifications

## Feature 8: EconomyManager Integration

### Template Manager Integration
```java
private DailyTaskTemplateManager templateManager;

public void initialize(MinecraftServer server) {
    this.templateManager = DailyTaskTemplateManager.load(server);
    this.dailyTasksManager.setTemplateManager(templateManager);
    // ...
}

public void save() {
    templateManager.save(server);
    // ...
}
```

**New Getter**:
```java
public DailyTaskTemplateManager getTemplateManager()
```

**Static Helper**:
```java
public static EconomyManager getInstance(MinecraftServer server)
```
- Ensures initialization
- Used by commands

## Technical Implementation

### Data Files (All Encrypted)
1. `daily_task_templates.json` - Template pool
2. `daily_tasks.json` - Player daily tasks
3. `economy.json` - Bank accounts
4. `achievement_rewards.json` - Achievement tracker
5. `money_requests.json` - Money requests

All use `SecureDataStorage` with AES-256-GCM encryption

### Event Handling
**Login**: `LoginNotificationHandler` listens to `PlayerLoggedInEvent`
**Progress**: `DailyTaskProgressListener` listens to multiple events:
- `BlockEvent.BreakEvent`
- `PlayerEvent.ItemCraftedEvent`
- `LivingDeathEvent`
- `TradeWithVillagerEvent`

### Chat Component System
**ClickEvent**: Opens GUI via command execution
**HoverEvent**: Shows tooltip on mouse hover
**Styling**: Colors, bold, underline for emphasis

### Example Chat Message Structure
```
═══════════════════════════════════════
⚙ ServerManagement Dashboard
Welcome back, Admin! [Open Dashboard]
              ↑ Clickable, runs /servermanagement dashboard
═══════════════════════════════════════

💰 You have a FREE REWARD available! [Claim Now]
                                     ↑ Clickable, runs /bank dailies

📋 You have 2 completed tasks to claim! [View Tasks]
                                        ↑ Clickable, runs /bank dailies

✓ Task Complete: Break 100 blocks | [Claim Reward]
                                    ↑ Clickable, runs /bank dailies
```

## Future GUI Implementation (Next Phase)

### Daily-Pack Configuration GUI
**Location**: ServerManagement Dashboard → Economy Section

**Planned Features**:
1. **Template List View**
   - Table showing all templates
   - Columns: Type, Goal, Reward, Enabled, Actions
   - Sort/filter by type
   - Enable/disable toggles

2. **Template Editor**
   - Create new template
   - Edit existing template
   - Fields: Type dropdown, Goal input, Reward input, Description input
   - Validation: Ensure positive values

3. **Template Preview**
   - Shows generated description
   - Preview how task will appear to players

4. **Free Reward Config**
   - Amount input field
   - Cooldown hours input field
   - Apply/save button

5. **Daily Reset Button**
   - "Force Reset All Dailies" button
   - Confirmation dialog
   - Shows affected player count

6. **Statistics Panel**
   - Total templates
   - Enabled/disabled count
   - Templates per type
   - Most/least used templates

7. **Current Active Tasks**
   - Shows what players currently have
   - Which templates were selected
   - Player completion stats

## Testing Checklist

### Login Notifications
- [ ] Admin login shows dashboard notification first
- [ ] Normal player login shows relevant notifications
- [ ] Free reward notification only when available
- [ ] Daily tasks notification shows correct counts
- [ ] Clicking notifications opens correct GUIs

### Task Completion Notifications
- [ ] Breaking blocks triggers notification at goal
- [ ] Killing mobs triggers notification at goal
- [ ] Crafting items triggers notification at goal
- [ ] Mining ores triggers notification at goal
- [ ] Trading villagers triggers notification at goal
- [ ] Multiple task types work simultaneously

### Payment Notifications
- [ ] Sender receives payment sent notification
- [ ] Receiver receives payment received notification
- [ ] Notifications are clickable
- [ ] Bank GUI opens from notification

### Reward Notifications
- [ ] Claiming task reward sends notification
- [ ] Claiming free reward sends notification
- [ ] Notifications show correct amounts

### Template System
- [ ] Templates load from encrypted file
- [ ] Default templates created on first run
- [ ] Daily tasks generated from templates
- [ ] Disabled templates not selected
- [ ] Custom descriptions used when set
- [ ] Auto-generated descriptions work

### Admin Commands
- [ ] `/servermanagement dashboard` opens GUI
- [ ] `/servermanagement resetdailies` resets all players
- [ ] Reset command reports correct player count
- [ ] Reset generates new tasks from templates

## Build Status
✅ **BUILD SUCCESSFUL** - All features compiled without errors

## Summary

Implemented a complete notification and admin management system:

**For Players**:
- Beautiful clickable chat notifications
- Login notifications for pending actions
- Real-time task completion alerts
- Payment/reward notifications
- One-click GUI access from chat

**For Admins**:
- First-priority dashboard notification
- Full control over daily task templates
- Configure free rewards
- Force reset dailies via command
- Template-based task generation

**Technical**:
- All data encrypted with AES-256-GCM
- Event-driven notification system
- Template pool with CRUD operations
- Smart task completion detection
- Zero spam (only relevant notifications)

**Next Phase**: Build GUI interfaces for template management in the ServerManagement Dashboard.
