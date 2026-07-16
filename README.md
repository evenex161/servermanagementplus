# ServerManagement+

**The all-in-one server management solution for Minecraft**

**Supported**: Minecraft 1.20.1 (Forge 47.4.0+) | Minecraft 1.21.1 (Forge 52.1.0+)

ServerManagement+ gives you a complete suite of tools to run your server — a full economy with bank accounts, a player marketplace, a casino, daily tasks, world management, and a sleek admin dashboard — all in one mod.

---

## Features

### Admin Dashboard
A modern dark-themed command center accessible with `/sm`. Six feature cards let you jump straight into World Manager, Player Manager, Console, Global Settings, Economy Management, or Mod Settings. Every screen has consistent navigation with back/close buttons and a polished header bar.

**Live Server Console** — Run server commands directly from the GUI. Output is color-coded by log level (red for errors, yellow for warnings) and scrollable. Requires OP level 2.

**Global Settings** — Toggle Chat Isolation (separate chat per dimension) and Tab Isolation (separate tab list per dimension) with iOS-style toggle switches.

**Mod Settings** — Enable or disable individual features (World Manager, Player Manager, SlimeHead, Economy) without restarting the server.

---

### Economy System
Every player gets a bank account on first join with a configurable starting balance (default: $1,000).

- **Balance management** — Check your balance, view transaction history, and track every dollar
- **Player-to-player transfers** — Send money directly or create money requests that the other player can accept or deny
- **Transaction history** — Full audit trail of all purchases, sales, transfers, task rewards, and free claims
- **Admin tools** — Set, give, or take money from any player via commands or the admin dashboard
- **Encrypted storage** — All economy data secured with AES-256-GCM encryption

---

### MineBay Marketplace
A fully-featured player marketplace where you can buy and sell items with other players.

- **Create listings** — Sell any item or stack from your inventory
- **Flexible pricing** — Accept money, up to 3 different item types, or both
- **Counteroffers** — Buyers can propose alternative prices; sellers review and accept/deny
- **Bank inventory** — If your inventory is full when you buy something, items go to your bank for later pickup
- **Item safety** — Atomic transactions with automatic rollback on failure. Items are never lost
- **Draft system** — Step-by-step listing creation with item picker and quantity controls

---

### MineStacks Casino
A full casino experience with four games, all using `SecureRandom` for provably fair results.

- **Coin Flip** — 2% house edge, 2x payout. Simple heads or tails
- **Dice Roll** — 3% house edge. Bet on High/Low (2x), Seven (5x), or Doubles (6x)
- **Slot Machine** — 5% house edge. Payouts from Cherry (2x) up to Jackpot (100x), with half payouts for two-of-a-kind
- **Roulette** — 2.7% house edge. Bet on Red/Black, Even/Odd, Low/High for 2x returns

Supports both money bets ($10–$10,000) and item bets (auto-valued). Includes spinning animations, particle effects on wins, screen shake, and sound effects. Your gambling stats are tracked.

---

### Daily Tasks & Free Rewards
Three randomly assigned tasks per day, drawn from a configurable template pool.

**Task Types:**
| Type | Example |
|---|---|
| Break Blocks | Break 250 blocks |
| Kill Mobs | Kill 50 mobs |
| Travel Distance | Travel 5,000 blocks |
| Craft Items | Craft 100 items |
| Mine Ores | Mine 75 ores |
| Trade Villagers | Trade 25 times |

- **Real-time progress tracking** with progress bars in the GUI
- **Instant notifications** when a task is completed — click the chat message to claim
- **Configurable rewards** — money and/or item rewards per task template
- **Free daily reward** — Claim free money and an optional item every 24 hours (cooldown configurable)
- **Admin template editor** — Create, edit, enable/disable, and delete task templates from the GUI

---

### World Management
Manage all server worlds from a visual interface.

- **World list** — See all dimensions with player counts
- **Teleportation** — Click to teleport between worlds (with cooldown)
- **Portal control** — Toggle Nether and End portals on or off
- **Lobby spawn** — Set a lobby spawn point and teleport players to it
- **Chat isolation** — Keep chat messages per-dimension
- **Tab isolation** — Separate the tab list by dimension
- **Timer system** — Auto-enable/disable portals on a schedule

---

### Player Manager
Admin tools for managing online players.

- **Player list** — View all online players with location, dimension, and gamemode
- **Spectate** — Watch any player in real-time
- **View inventory** — Inspect a player's inventory
- **Kick/Ban** — Remove or ban players directly from the GUI

---

### SlimeHead
A fun cosmetic item — an unbreakable slime head block with a custom texture.

- Obtained via `/slimehead` command or a 5% drop chance from slimes
- Protected from breaking by non-ops when placed
- Available in the Creative Tools & Utilities tab

---

### Notification System
Clickable chat notifications keep players informed without being spammy.

- **Login notifications** — Admins see a dashboard link; players see free reward and task reminders
- **Payment alerts** — Notifications when you send or receive money
- **Task completion** — Instant notification with a clickable claim button when a task is done
- **Reward claimed** — Confirmation when rewards are collected

---

### Help Integration
All ServerManagement+ commands are integrated into the vanilla `/help` command.

- **`/help`** — Lists all available commands including ServerManagement+ ones with descriptions
- **`/help <command>`** — Detailed usage for any mod command (e.g., `/help bank`)
- **Admin filtering** — Admin commands tagged with `[Admin]` and hidden from non-ops
- Works alongside vanilla and other mods' commands

---

### Bifurcated OTA Update & Smart Start System
Automatic over-the-air mod updates for connected clients and intelligent server update handling.

- **Dual-Query Architecture** — Independently queries both Modrinth and CurseForge APIs for the latest releases
- **Standalone Handoff** — Extracts an embedded `updater.jar` to execute outside the main JVM, permitting safe jar hot-swapping while the server is offline
- **Graceful Shutdown** — Halts the server process gracefully prior to applying updates to ensure all world data is saved and standard ports are freed
- **Smart Start Generator** — Dynamically parses existing `run.bat` / `run.sh` scripts in the server root and injects the `-Dservermanagement.smartstart=true` JVM flag, enabling automated reboot loops post-update
- **Manual Installation Notice (v2.1.1-b1)** — Due to the complete architecture overhaul, v2.1.1-b1 must be installed manually for both the server and client. Subsequent updates will be handled by the new automated system.
- **Smart version comparison** — Accurate semantic versioning evaluation that considers build numbers for granular patch detection
- **Multi-version aware** — OTA updates are blocked across different Minecraft versions (e.g., a 1.20.1 client won't receive a 1.21.1 update)
- **Multi-loader aware** — Clients are only served updates matching their active loader (Forge, Fabric, NeoForge)

#### How the Updater Works (For Manual Reviewers)
To ensure full transparency on how the mod handles updates without triggering false positive security flags:
1. **API Fetching**: The mod only makes standard GET requests to the official `api.modrinth.com` and `api.curseforge.com` endpoints to check for version numbers.
2. **Standalone Updater**: To avoid file-lock issues on Windows, the mod extracts an embedded `updater.jar` into the server root. This jar runs in a separate, temporary JVM process whose *only* job is to wait for the main server to exit, copy the newly downloaded mod jar over the old one, and then delete itself.
3. **Server-Side Script Modification**: If an admin opts into the Smart Restart feature via the in-game GUI prompt, the mod will read their existing `run.bat` or `run.sh` script, back it up to `run.bat.bak`, and inject a standard `-Dservermanagement.smartstart=true` JVM argument. It **does not** execute arbitrary shell code or download external scripts. All script generation is done locally and requires explicit in-game admin consent.
4. **Client-Side Consent**: On the client side, if an update is detected, the mod intercepts the Main Menu with a dedicated UI overlay. The client will **never** automatically download or install anything without the user clicking **"Update Now"**. Users are also explicitly provided with **"Update Later"** and **"Skip This Version"** options.
5. **Verify it Yourself**: If you are a server administrator or code reviewer and want to safely test this system, you can force the updater to trigger by "down-bumping" the mod version. Open the mod jar with an archiver (like 7-Zip), edit `META-INF/mods.toml` (Forge) or `fabric.mod.json` (Fabric), and change the version to `2.1.0`. Launch a local test server and explicitly opt-in to the Smart Restart feature via the in-game admin menu. You will observe that the mod securely downloads the update, extracts `updater.jar`, backs up your run scripts to `.bak` files, and cleanly injects the `-Dservermanagement.smartstart=true` flag without executing arbitrary shell code. Nothing is hidden.

---

### Security
All sensitive data is encrypted at rest and authenticated in transit.

- **AES-256-GCM encryption** for economy data, daily tasks, money requests, and achievement tracking
- **Unique server key** auto-generated on first run, stored with restricted permissions
- **HMAC-SHA256** packet authentication prevents replay and tampering
- **Session management** with per-player tokens and 30-minute timeout
- **Atomic transactions** with automatic rollback — no partial operations, no data loss
- **Network buffer hardening** — All 54 packet string fields enforce strict length limits (`readUtf(N)`) to prevent memory exhaustion from oversized payloads
- **Input validation** — Player names validated against `[a-zA-Z0-9_]{1,16}` regex at the network layer before any server-side processing
- **Log injection prevention** — User-controlled strings are sanitized before logging to prevent log forging
- **Thread-safe marketplace** — MineBay listing creation uses synchronized operations to prevent race-condition exploits that could bypass per-player listing limits

---

## Commands

### Player Commands

| Command | Description |
|---|---|
| `/bank` | Open Bank GUI |
| `/bank balance` | View your balance |
| `/bank pay <player> <amount>` | Send money to a player |
| `/bank request <player> <amount>` | Request money from a player |
| `/bank requests` | View pending money requests |
| `/bank accept <id>` | Accept a money request |
| `/bank deny <id>` | Deny a money request |
| `/bank stats` | View your bank statistics |
| `/bank dailies` | Open Daily Tasks GUI |
| `/bank dailies claim <1-3>` | Claim a completed daily task |
| `/bank dailies free` | Claim free daily reward |
| `/minebay` | Open MineBay marketplace |
| `/minestacks` or `/casino` | Open MineStacks Casino |
| `/teleportlobby` | Teleport to lobby |

### Admin Commands

| Command | Permission | Description |
|---|---|---|
| `/sm` or `/servermanagement` | OP 2 | Open admin Dashboard |
| `/smconfig` | OP 2 | Open mod settings GUI |
| `/smconfig toggle <feature>` | OP 2 | Toggle a feature on/off |
| `/smconfig info` | OP 4 | Show config version info |
| `/smconfig validate` | OP 4 | Validate config integrity |
| `/smconfig reset confirm` | OP 4 | Reset config to defaults |
| `/worldmanager` or `/wm` | OP 2 | Open World Manager |
| `/playermanager` or `/pm` | OP 2 | Open Player Manager |
| `/spectate <player>` | OP 2 | Spectate a player |
| `/stopspectate` | OP 2 | Stop spectating |
| `/viewinv <player>` | OP 2 | View a player's inventory |
| `/netherportals <true\|false>` | OP 2 | Toggle Nether portals |
| `/endportals <true\|false>` | OP 2 | Toggle End portals |
| `/setlobby` | OP 2 | Set lobby spawn |
| `/clearlobby` | OP 2 | Remove lobby spawn |
| `/slimehead [player]` | OP 2 | Give a Slime Head |
| `/bank admin set <player> <amount>` | OP 2 | Set a player's balance |
| `/bank admin give <player> <amount>` | OP 2 | Give money to a player |
| `/bank admin take <player> <amount>` | OP 2 | Take money from a player |
| `/servermanagement resetdailies` | OP 2 | Force-reset all daily tasks |
| `/smmetrics` | OP 2 | View performance metrics |
| `/smmetrics reset` | OP 2 | Reset performance metrics |

---

## Configuration

The config file is located at `config/servermanagement-common.toml`. Key settings:

| Setting | Default | Description |
|---|---|---|
| Starting Balance | $1,000 | Initial money for new players |
| Encrypted Storage | Enabled | AES-256-GCM data encryption |
| Transaction History Limit | 100 | Transactions stored per player |
| Tasks Per Day | 3 | Daily tasks assigned per player |
| Free Reward Amount | $50 | Daily free claim amount |
| Free Reward Cooldown | 24 hours | Time between free claims |
| Max Listings Per Player | 10 | MineBay listing cap |
| Max Price Items | 3 | Item types accepted per listing |
| Counteroffers | Enabled | Allow buyers to make offers |
| Teleport Cooldown | 5 seconds | Between world teleports |

All economy data, task templates, and player progress persist across server restarts with automatic backup and migration support.

---

## Installation

### Requirements
- **Minecraft** 1.20.1 or 1.21.1
- **Forge** 47.4.0+ (MC 1.20.1) or 52.1.0+ (MC 1.21.1)
- **Java** 17+ (MC 1.20.1) or 21+ (MC 1.21.1)

### Setup
1. Download the JAR for your Minecraft version and mod loader:
   - MC 1.20.1 (Forge): `ServerManagement-forge-1.20.1-2.1.1-b01-mc1.20.1.jar`
   - MC 1.20.1 (Fabric): `ServerManagement+-fabric-1.20.1-2.1.1-b01-mc1.20.1.jar`
   - MC 1.21.1 (Forge): `ServerManagement-forge-1.21.1-2.1.0-b01-mc1.21.1.jar`
   - MC 1.21.1 (NeoForge): `ServerManagement-neoforge-1.21.1-2.1.0-b01-mc1.21.1.jar`
   - MC 1.21.1 (Fabric): `ServerManagement+-fabric-1.21.1-2.1.0-b01-mc1.21.1.jar`
2. Place it in your server's `mods/` folder
3. Start the server — config and data folders generate automatically
4. Optionally install on clients for full GUI support (server-side only works too)
5. Adjust settings in `config/servermanagement-common.toml` and restart

### First-Time Setup (Server Owners)
1. Review config values (starting balance, rewards, cooldowns)
2. Open the admin dashboard with `/sm` to explore features
3. Go to **Economy Management** to create daily task templates
4. Set free reward amounts and items
5. Test the economy flow with `/bank`, `/minebay`, and `/minestacks`

---

## Data Storage

All mod data is stored in `world/data/servermanagement/`:

| File | Contents | Format |
|---|---|---|
| `economy.json` | Bank accounts & balances | Encrypted JSON |
| `daily_tasks.json` | Player task progress | Encrypted JSON |
| `daily_task_templates.json` | Admin task templates | Encrypted JSON |
| `money_requests.json` | Pending transfers | Encrypted JSON |
| `achievement_rewards.json` | Achievement claims | Encrypted JSON |
| `world_manager.json` | World settings | Plain JSON |
| `minebay_listings.dat` | Marketplace listings | Compressed NBT |
| `playerdata/[UUID]/bank_inventory.dat` | Bank items | Compressed NBT |
| `playerdata/[UUID]/transactions.dat` | Transaction history | Compressed NBT |

**Back up `world/data/servermanagement/` and `config/servermanagement/` before updates.**

---

## Troubleshooting

| Problem | Solution |
|---|---|
| "Could not load economy data" | Restore `economy.json` from backup, or delete it to reset |
| MineBay listings not loading | Check `minebay_listings.dat` — restore from backup if corrupted |
| Daily tasks not resetting | Verify server system clock; adjust `resetHour` in config |
| GUI not opening | Ensure the mod is installed on both client and server; reconnect |
| Transaction failed | Check chat error message — transactions auto-rollback, no data is lost |

---

## License

MIT License — free to use, modify, and distribute.

---

*ServerManagement+ v2.1.1-b1 — Minecraft 1.20.1 / 1.21.1 — Forge 47.4.0+ / 52.1.0+*
