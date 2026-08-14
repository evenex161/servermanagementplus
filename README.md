# ServerManagement+

**The all-in-one server management solution for Minecraft**

**Supported**: Minecraft 1.20.1 (Forge 47.4.0+) | Minecraft 1.21.1 (Forge 52.1.0+, NeoForge 21.1.80+, Fabric 0.116.1+)

ServerManagement+ gives you a complete suite of tools to run your server — a full economy with bank accounts, a player marketplace, a casino, daily tasks, world management, and a sleek admin dashboard — all in one mod.

---

## Changelogs

Detailed changelogs are available per branch on GitHub:

| Branch | Changelog |
|---|---|
| MC 1.20.1 — Forge (`mc/1.20.1-forge`) | [CHANGELOG_v1.0.3.md](https://github.com/evenex161/servermanagementplus/blob/mc/1.20.1-forge/CHANGELOG_v1.0.3.md) |
| MC 1.21.1 — MultiLoader (`mc/1.21.1`) | [CHANGELOG_BETA-v2.1.1-v2-goldrush.md](https://github.com/evenex161/servermanagementplus/blob/mc/1.21.1/changelogs/CHANGELOG_BETA-v2.1.1-v2-goldrush.md) |

---

## Features

### Admin Dashboard
A modern dark-themed command center accessible with `/sm`. Six feature cards let you jump straight into World Manager, Player Manager, Console, Global Settings, Economy Management, or Mod Settings. Every screen has consistent navigation with back/close buttons and a polished header bar.

**Live Server Console** — Run server commands directly from the GUI. Output is color-coded by log level (red for errors, yellow for warnings) and scrollable. Requires OP level 2.

**Global Settings** — Toggle Chat Isolation (separate chat per dimension) and Tab Isolation (separate tab list per dimension) with iOS-style toggle switches.

**Mod Settings** — Enable or disable individual features (World Manager, Player Manager, SlimeHead, Economy) without restarting the server.

---

### Economy System & Global Census Engine
Every player gets a bank account on first join with a configurable starting balance (default: $1,000).

- **Global Census Engine** — A revolutionary tick-based differential scanner that tracks the exact population of all items on the server across loaded chunks and offline player inventories. Solves duplication glitches and hyper-inflation permanently!
- **Dynamic Scarcity & Capital Scaling** — The economy automatically scales base anchor prices against the server's inflation. Uncraftable items (like Dragon's Breath) receive massive scarcity multipliers based on real server supply.
- **Creative Mode Market Protection** — Items spawned via Creative Mode or admin `/give` commands are explicitly tagged and ignored by the market, protecting your economy from artificial crashes.
- **Balance management** — Check your balance, view transaction history, and track every dollar.
- **Player-to-player transfers** — Send money directly or create money requests that the other player can accept or deny.

---

### MineBay Marketplace
A fully-featured player marketplace where you can buy and sell items with other players.

- **Create listings** — Sell any item or stack from your inventory.
- **Flexible pricing** — Accept money, up to 3 different item types, or both.
- **Counteroffers** — Buyers can propose alternative prices; sellers review and accept/deny.
- **Bank inventory** — If your inventory is full when you buy something, items go to your bank for later pickup.
- **Trade Blacklist Editor** — Admins can enforce global trade bans on specific items. Banned items are visually labeled in tooltips and blocked from being listed.

---

### MineStacks Casino
A full casino experience with four games, all using `SecureRandom` for provably fair results.

- **Coin Flip** — 50/50 odds, exact 2x payout (0% house edge).
- **Dice Roll** — Bet on High/Low (2x), Seven (5x), or Doubles (6x).
- **Slot Machine** — Payouts from Cherry (2x) up to Jackpot (100x), with half payouts for two-of-a-kind.
- **Roulette** — Bet on Red/Black, Even/Odd, Low/High for 2x returns.

Supports both money bets ($10–$10,000) and item bets (auto-valued). Includes spinning animations, particle effects on wins, screen shake, and sound effects. Your gambling stats are tracked.

---

### Daily Tasks & Visual Node Editor
Three randomly assigned tasks per day, drawn from a configurable template pool.

- **Visual Node-Based Editor** — Create multi-item task templates via a stunning, interactive TIA Portal-inspired node graph with cubic bezier connections!
- **Multiple Item Rewards** — Templates fully support assigning an unlimited number of items as rewards alongside cash.
- **Real-time progress tracking** with dynamic smooth-transitioning progress bars in the GUI.
- **Instant notifications** when a task is completed — click the chat message to claim.
- **Free daily reward** — Claim free money and an optional item every 24 hours (cooldown configurable).

---

### World & Player Management
Manage all server worlds and online players from visual interfaces.

- **World list** — See all dimensions with player counts. Click to teleport.
- **Portal control** — Toggle Nether and End portals on or off dynamically.
- **Chat/Tab isolation** — Keep chat messages and tab lists separated per-dimension.
- **Spectate & View Inventory** — Watch any player in real-time or inspect their inventory.
- **Kick/Ban** — Remove or ban players directly from the GUI.

---

### OTA Update System
Automatic over-the-air mod updates for connected clients.

- **Background Smart Checking** — Silently queries Modrinth/CurseForge APIs and presents a beautiful `UpdateAvailableScreen` upon game startup.
- **Seamless Download & Handoff** — Downloads the correct JAR and seamlessly restarts the game via the standalone `updater.jar`.
- **Multi-version & Multi-loader aware** — Updates are blocked across different Minecraft versions and mod loaders.

---

## Security & Architecture

All sensitive data is encrypted at rest and authenticated in transit. **For a full cryptographic breakdown, see [SECURITY.md](SECURITY.md).**

- **Runtime Bytecode Integrity Verification** — The telemetry API keys are encrypted at rest. The mod uses the SHA-256 hash of its own `.class` file to decrypt it at runtime, acting as a permanent anti-tamper lock.
- **AES-256-GCM encryption** for economy data, daily tasks, money requests, and achievement tracking.
- **HMAC-SHA256** packet authentication prevents replay and tampering.
- **Atomic transactions** with automatic escrow rollback — no partial operations, no data loss.
- **Network buffer hardening** — Strict payload length limits to prevent memory exhaustion.

---

## Performance Orchestration & Synergy Engine

ServerManagement+ is heavily optimized for massive scale, operating via a multi-tiered Synergy Engine:

- **Phase 1:** Implements `zstd-jni` packet compression utilizing raw Netty ByteBuffers for zero-copy memory efficiency.
- **Phase 2:** Server-side MSPT Monitors dynamically scale `SimulationDistance` during lag spikes. Chunk Unload Delay Managers handle lazy 10-second offloads.
- **Phase 3:** Client Chunk Caching retains up to 10k unloaded chunks, feeding them back into a FakeChunkInjector, offloading visibility raycasts to an AsyncOcclusionCuller.
- **Phase 4:** Distant Horizons soft-dependency hooking pushes rendering minimums outwards and eliminates Z-fighting.
- **O(1) Data Structures** — Tick handlers utilize HashSets, precompiled regex, and direct array loops rather than streams to eliminate GC pressure.

---

## Commands

### Player Commands
| Command | Description |
|---|---|
| `/bank` | Open Bank GUI |
| `/minebay` | Open MineBay marketplace |
| `/minestacks` or `/casino` | Open MineStacks Casino |
| `/teleportlobby` | Teleport to lobby |
| `/dailies claim` | Claim completed tasks |

### Admin Commands (OP 2)
| Command | Description |
|---|---|
| `/sm` or `/servermanagement` | Open admin Dashboard |
| `/sm update` | Open the Server Updater |
| `/worldmanager` or `/wm` | Open World Manager |
| `/playermanager` or `/pm` | Open Player Manager |
| `/spectate <player>` | Spectate a player |
| `/viewinv <player>` | View a player's inventory |

---

## Installation

### Requirements
- **Minecraft** 1.20.1 or 1.21.1
- **Forge** 47.4.0+ (MC 1.20.1) or 52.1.0+ (MC 1.21.1), **or NeoForge** 21.1.80+ (MC 1.21.1), **or Fabric** API 0.116.1+ (MC 1.21.1)
- **Java** 17+ (MC 1.20.1) or 21+ (MC 1.21.1)

### Setup
1. Download the JAR for your Minecraft version and mod loader.
2. Place it in your server's `mods/` folder.
3. Start the server — config and data folders generate automatically.
4. Adjust settings in `config/servermanagement-common.toml` and restart.

*(Back up `world/data/servermanagement/` and `config/` before major updates.)*

---

## License

MIT License — free to use, modify, and distribute.

---

*ServerManagement+ v2.1.1 — Minecraft 1.20.1 / 1.21.1 — Forge 47.4.0+ / 52.1.0+ — NeoForge 21.1.80+ — Fabric API 0.116.1+*
