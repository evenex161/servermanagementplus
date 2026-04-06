# Server Management Plus

**Version**: v1.0.4-b01 (Pre-release)  
**Supported**: Minecraft 1.20.1 (Forge 47.4.0+) | Minecraft 1.21.1 (Forge 52.1.0+) | Minecraft 1.21.1 (NeoForge 21.1.222+)

Server Management Plus gives server owners a complete toolkit to build an engaging, professional server experience — a full economy system, player marketplace, casino, daily tasks, world management, and a polished admin dashboard, all packed into a single mod.

> **Full changelogs** are available on GitHub:
> - [v1.0.4 — MC 1.21.1 Forge](https://github.com/evenex161/servermanagementplus/blob/mc/1.21.1-forge/CHANGELOG_v1.0.4.md)
> - [v1.0.3 — MC 1.20.1 Forge](https://github.com/evenex161/servermanagementplus/blob/mc/1.20.1-forge/CHANGELOG_v1.0.3.md)
> - [v1.0.3 — MC 1.21.1 Forge](https://github.com/evenex161/servermanagementplus/blob/mc/1.21.1-forge/CHANGELOG_v1.0.3.md)
> - [v1.0.3 — MC 1.21.1 NeoForge](https://github.com/evenex161/servermanagementplus/blob/mc/1.21.1-neoforge/CHANGELOG_v1.0.3-neoforge.md)

---

## Features

### 🖥️ Admin Dashboard
A modern dark-themed command center accessible with `/sm`. Jump directly into World Manager, Player Manager, Console, Global Settings, Economy Management, or Mod Settings from a single hub. Every screen features consistent navigation with back and close buttons.

**Live Server Console** — Execute server commands directly from an in-game GUI with color-coded output (red for errors, yellow for warnings). No more tabbing out to the server console.

**Global Settings** — Toggle Chat Isolation (per-dimension chat) and Tab Isolation (per-dimension player list) with a single click.

**Mod Settings** — Enable or disable individual features on the fly, without restarting the server.

---

### 💰 Economy System
Every player gets a bank account on first join with a configurable starting balance (default: $1,000).

- **Balance & history** — Check your balance, browse your full transaction history, and see exactly where every dollar came from
- **Send money** — Transfer funds directly to another player or send a money request they can accept or decline
- **Achievement rewards** — Earn money automatically when you unlock in-game advancements
- **Dynamic market pricing** — Item prices adjust in real time based on server inflation, player wealth, and supply/demand
- **Supply & demand tracking** — Mining, crafting, smelting, and pickups are tracked server-wide; scarce items cost more, oversupplied items cost less
- **Margin history** — Every listing's margin is logged for future pricing intelligence and analytics
- **Admin tools** — Server owners can set, give, or take money from any player via commands or the dashboard
- **Secure storage** — All economy data is encrypted at rest — player balances are never stored in plain text

---

### 🛒 MineBay Marketplace
A fully-featured player-to-player marketplace for buying and selling items.

- **Create listings** — Sell any item or stack directly from your inventory
- **Smart pricing** — Price fields auto-populate with the current market value; margin defaults to 10%
- **Flexible pricing** — Accept money, up to 3 different item types, or a combination of both
- **Counteroffers** — Buyers can propose alternative prices; sellers review and accept or decline
- **Price tooltips** — Hover over any item to see its market price per unit and total stack value
- **Inventory value overlay** — Total inventory value displayed above the player inventory
- **Bank inventory** — Purchased items that don't fit your inventory go straight to your bank for safe pickup later
- **No item loss** — Every transaction is atomic — if anything goes wrong, everything is automatically rolled back
- **Step-by-step listing wizard** — Place your item first, then set prices — a natural two-step flow with live price preview

---

### 🎰 MineStacks Casino
Four fully animated casino games with provably fair results.

- **Coin Flip** — 2% house edge, 2× payout
- **Dice Roll** — 3% house edge — bet on High/Low (2×), Seven (5×), or Doubles (6×)
- **Slot Machine** — 5% house edge — payouts from Cherry (2×) up to Jackpot (100×)
- **Roulette** — 2.7% house edge — Red/Black, Even/Odd, or Low/High for 2× returns

Bet with money ($10–$10,000) or items (auto-valued). Includes spinning animations, win effects, screen shake, and sound. Stats are tracked per player.

---

### 📋 Daily Tasks & Free Rewards
Three randomly assigned tasks every day keep players engaged and rewarded.

**Task types:** Break Blocks · Kill Mobs · Travel Distance · Craft Items · Mine Ores · Trade with Villagers

- **Live progress** — Progress bars update in real time in the GUI
- **Instant claim** — Click the chat notification the moment a task is complete
- **Custom rewards** — Each task template can award money, items, or both
- **Free daily reward** — A claimable free reward resets every 24 hours
- **Admin template editor** — Create, edit, enable, and delete task templates directly from the GUI

---

### 🌍 World Manager
Manage all your server's worlds and dimensions from a single visual interface.

- **World list** — See all dimensions and how many players are in each
- **Teleportation** — Click to teleport between worlds (configurable cooldown)
- **Portal control** — Toggle Nether and End portals on or off at will
- **Lobby system** — Set a lobby spawn and send players there with `/teleportlobby`
- **Chat & tab isolation** — Separate chat and player list by dimension
- **Scheduled timers** — Automatically enable or disable portals on a timer

---

### 👤 Player Manager
Everything a moderator or admin needs to manage online players.

- **Player overview** — See all online players with their location, dimension, and gamemode at a glance
- **Live spectate** — Jump into spectator view of any player instantly from the GUI
- **Inventory inspection** — View any player's inventory without disturbing them
- **Kick & ban** — Remove or ban players directly from the player manager panel

---

### 🟢 SlimeHead
A collectible cosmetic item — an unbreakable slime head with a custom texture.

- Obtained via `/slimehead` or as a rare 5% drop from slimes
- Protected from being broken by non-ops when placed
- Found in the Creative Tools & Utilities tab

---

### 🔔 Notifications
Clickable chat notifications keep everyone informed without spam.

- **Login** — Admins get a dashboard shortcut; players see free reward and task reminders on join
- **Payments** — Instant alerts when money is sent or received
- **Task complete** — Click-to-claim notification the moment a task finishes
- **Reward claimed** — Confirmation chat message when a reward is collected

---

### ❓ Help Integration
All Server Management commands appear in the vanilla `/help` system.

- `/help` — Lists all mod commands alongside vanilla ones
- `/help <command>` — Shows full usage for any mod command (e.g., `/help bank`)
- Admin commands are tagged `[Admin]` and hidden from non-ops

---

### 🔄 OTA Auto-Updates
Clients connected to your server automatically receive mod updates over the air.

- Detects version mismatches on connect and downloads the correct JAR
- Verifies file integrity before applying any update
- Shows an in-game progress screen during the update
- Version-aware: a 1.20.1 client won't accidentally receive a 1.21.1 update
- Loader-aware: Forge clients won't receive NeoForge updates and vice versa

---

### 🔒 Security
Your server's data is secure and your players are protected.

- **Encrypted data storage** — Economy data, tasks, money requests, and achievements are all encrypted at rest
- **Tamper-proof packets** — Network packets are authenticated to prevent spoofing or replay attacks
- **Session tokens** — Per-player session authentication with automatic 30-minute timeout
- **Safe transactions** — All economy operations are atomic — partial failures roll back completely so no money or items are ever lost
- **Input hardening** — Player names and all user-supplied text are validated at the network layer before reaching any server logic

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

The config file is at `config/servermanagement-common.toml`. Key settings:

| Setting | Default | Description |
|---|---|---|
| Starting Balance | $1,000 | Money given to new players on first join |
| Encrypted Storage | Enabled | Encrypts all economy data at rest |
| Transaction History Limit | 100 | Transactions stored per player |
| Tasks Per Day | 3 | Daily tasks assigned per player |
| Free Reward Amount | $50 | Daily free claim amount |
| Free Reward Cooldown | 24 hours | Time between free claims |
| Max Listings Per Player | 10 | MineBay listing cap per player |
| Max Price Items | 3 | Item types accepted as payment per listing |
| Counteroffers | Enabled | Allow buyers to propose alternative prices |
| Teleport Cooldown | 5 seconds | Cooldown between world teleports |

All economy data, task templates, and player progress persist across server restarts with automatic backup and migration support.

---

## Installation

### Requirements
- **Minecraft** 1.20.1 or 1.21.1
- **Forge** 47.4.0+ (MC 1.20.1) or 52.1.0+ (MC 1.21.1), **or NeoForge** 21.1.222+ (MC 1.21.1)
- **Java** 17+ (MC 1.20.1) or 21+ (MC 1.21.1)

### Setup
1. Download the JAR for your Minecraft version and mod loader:
   - MC 1.21.1 — Forge: `servermanagementplus-v1.0.4-b01-mc1.21.1-forge-release.jar`
   - MC 1.20.1 — Forge: `servermanagementplus-v1.0.3-b05-mc1.20.1-forge-release.jar`
   - MC 1.21.1 — NeoForge: `servermanagementplus-v1.0.3-b05-mc1.21.1-neoforge-release.jar`
2. Drop the JAR into your server's `mods/` folder
3. Start the server — all config and data folders are created automatically
4. Install the same JAR on clients for the full GUI experience (server-side-only works too)
5. Tune settings in `config/servermanagement-common.toml` and restart

### First-Time Setup
1. Review config values (starting balance, reward amounts, cooldowns)
2. Open the admin dashboard with `/sm` to get familiar with the interface
3. Go to **Economy Management** → create your daily task templates
4. Set free reward amounts and optional item rewards
5. Test the flow with `/bank`, `/minebay`, and `/minestacks`

---

## Data Storage

All mod data is stored under `world/data/servermanagement/`:

| File | Contents | Format |
|---|---|---|
| `economy.json` | Bank accounts & balances | Encrypted JSON |
| `daily_tasks.json` | Player task progress | Encrypted JSON |
| `daily_task_templates.json` | Admin task templates | Encrypted JSON |
| `money_requests.json` | Pending transfers | Encrypted JSON |
| `achievement_rewards.json` | Achievement claims | Encrypted JSON |
| `world_manager.json` | World settings | Plain JSON |
| `minebay_listings.dat` | Marketplace listings | Compressed NBT |
| `supply_demand.dat` | Server-wide item supply counts | Binary |
| `margin_history.dat` | Per-item margin history | Binary |
| `playerdata/[UUID]/bank_inventory.dat` | Bank items | Compressed NBT |
| `playerdata/[UUID]/transactions.dat` | Transaction history | Compressed NBT |

**Back up `world/data/servermanagement/` and `config/servermanagement/` before updating the mod.**

---

## Troubleshooting

| Problem | Solution |
|---|---|
| "Could not load economy data" | Restore `economy.json` from backup, or delete it to start fresh |
| MineBay listings not loading | Check `minebay_listings.dat` — restore from backup if corrupted |
| Daily tasks not resetting | Verify the server's system clock; check `resetHour` in config |
| GUI not opening | Confirm the mod is installed on both client and server, then reconnect |
| Transaction failed | Check the chat error message — all transactions auto-rollback, nothing is lost |

---

## License

MIT License — free to use, modify, and distribute.

---

*Server Management Plus v1.0.4-b01 — Minecraft 1.20.1 / 1.21.1 — Forge 47.4.0+ / 52.1.0+ — NeoForge 21.1.222+*
