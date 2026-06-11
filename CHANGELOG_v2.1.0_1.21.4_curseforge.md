# ServerManagement+ v2.1.0 (Minecraft 1.21.4)

**Minecraft 1.21.4** — Fabric API 0.116.1 / NeoForge 21.4.150 / Paper API 1.21.4 / Java 21

v2.1.0 for Minecraft 1.21.4 represents a major modernization of the ServerManagement+ platform. We have fully updated the codebase to Java 21, adopted official Minecraft 1.21.4 APIs, purged the obsolete legacy Forge loader, and expanded support to Paper/Folia and Quilt.

---

## Mod Loader Expansion & Modernization

*   **Fabric & NeoForge**: Core loaders updated and fully compile/run on Minecraft 1.21.4.
*   **Paper/Folia Support (New)**: Added a brand new native `paper/` module targeting Paper and Folia servers (via Paper API 1.21.4) to capture the dedicated server market.
*   **Quilt Compatibility**: Bundled `quilt.mod.json` inside the Fabric build pipeline for seamless Quilt loader support.
*   **Legacy Forge Purged**: Completely removed the obsolete legacy Forge module (`forge/`), streamlining development around modern Fabric, NeoForge, and Paper targets.

---

## Behind the Scenes & Core Refactors

*   **Java 21 Modernization**: Full utilization of Java 21 language features, including virtual threads.
*   **Virtual Threading for Saving**: Re-implemented the `AsyncSaveScheduler` using Java 21 virtual threads, allowing low-overhead, asynchronous serialization of economy, listings, and templates.
*   **Record-Based Networking**: Completed packet parity across all platform-specific network packets using modern Java records.
*   **Cryptographic Session Verification**: Hardened S2C/C2S network communications using random cryptographic session tokens verified on login.
*   **Self-Healing Tab Isolation**: A 100-tick (5-second) heartbeat resync automatically heals tab lists in case of client/server sync drift.
*   **Automatic Config Backups**: Database/config migration now auto-backs up the config folder to `serverdata/servermanagement/backups/` before running migrations.
*   **Recipe Loop Protection**: Item pricing updates converge cleanly with a hard loop limit of 25 cycles, preventing infinite startup loops.

---

## Fabric Event Wiring & Mixins

*   Wired up critical Fabric events and mixins for portals, timers, advancements, item pricing, and stale cache screen reloads.
*   Resolved Fabric-specific issues with dimension travel, portal countdowns, and advancement earning rewards.

---

## Upgrade Notes

*   v2.1.0 is **save-compatible** with v2.0.x — no world reset needed.
*   Before migration, it is highly recommended to let the auto-backup system copy your database configurations or manually back up `serverdata/servermanagement/`.
*   If migrating from Fabric to NeoForge or Paper, copy the `<world>/serverdata/servermanagement/` folder to retain listings, accounts, and server settings.
