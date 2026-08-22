# ServerManagement+ v2.1.2 (Minecraft 1.20.1)

This update backports critical bug fixes and configuration improvements from the 1.21.1 branches to the 1.20.1 release, while intentionally omitting the major UI/Networking rewrites (Multi-Item Rewards) to preserve 1.20.1 GUI stability.

## Features & Improvements
- **Configuration Consolidation**: Consolidated Economy settings (`Starting Balance`, `MineBay Enabled`, `MineStacks Enabled`, `Max Listings`, `Trade Blacklist`, etc.) into `serverconfig/servermanagement-server.toml` for standard runtime parity with modern branches.

## Economy Rebalancing (Phase 1 — Goldrush Backport)
- **Inflation Floor Fix** (`MarketPricingEngine`): Changed `MIN_INFLATION` from `0.1` to `1.0`. Anchor prices now represent absolute intrinsic value floors — inflation only scales prices **up** when the economy grows, never crushes them below baseline.
- **Supply Factor Softening** (`ItemSupplyDemandTracker`, `ClientMarketData`): Replaced aggressive `1/(1+log10)` supply curve with gentle `1/(1+0.15*ln)` natural log curve. At 5000 supply (10x baseline), items now only lose ~26% value instead of ~55%. Scarcity bonus reduced from 1.2/20% to 1.1/10%.
- **Comprehensive Anchor Prices** (`ItemValuation`): Expanded from ~30 to ~140 hardcoded anchor prices. Added raw ores (gold, iron, copper, diamond, emerald, ancient debris), building blocks (stone, cobblestone, granite, diorite, andesite, deepslate, tuff), bricks, sandstone variants, prismarine, nether materials, quartz variants, copper oxidation variants, and rare unobtainables (dragon breath, heart of the sea, trident, conduit, shulker shell, echo shard, wither skeleton skull).
- **Recipe Pricing Stability** (`RecipeBasedPricing`): Tightened cycle breaker from 25 to 15 price increases. Added `lockedItems` set for proper cycle detection that falls back to anchor/dynamic prices instead of silently freezing. Added $1,000,000 hard ceiling on recipe-derived prices. Added `getDynamicFallbackPrice()` with bounded scarcity multipliers (sqrt taper, rarity modifiers) and a hard $500 cap for items with no recipe and no anchor.
- **Client-Side Fallback Pricing** (`ClientMarketData`): Added `getDynamicFallbackPrice()` mirroring server logic. Items not in the recipe cache now get scarcity-aware pricing instead of flat $1.00. Switched item ID resolution from full NBT key to simple `BuiltInRegistries.ITEM.getKey()` for consistency.
- **Fair Gambling** (`GamblingManager`): Set all MineStacks house edge constants to `0.0` (was 2-5%). All games now offer fair 50/50 payouts.

## Bug Fixes
- **Economy NBT Serialization**: Fixed a critical exploit where items with NBT data (such as Enchanted Books, named items, or damaged tools) were losing their exact state upon server restart. The dynamic market engine and recipe pricing now correctly serialize and track NBT data in 1.20.1.
- **Repository Cleanup**: Cleaned up leftover testing directories and `TestModrinth.java` scripts that leaked into the production tree.
- **Versioning**: Adapted internal hardcoded updater target versions to `2.1.2-b1` due to the previous version being released already.
- **Server Stability Fixes:**
    *   Fixed a critical crash on dedicated servers where opening the GUI would attempt to load client-only classes.
    *   Fixed a critical memory-leak where cached `Menu` items inadvertently prevented JVM garbage collection because they were incorrectly instantiated inside constructor lambdas.
    *   Fixed a `BootstrapMethodError` during dedicated server startup caused by `OpenGuiPacket` aggressively referencing client UI screens.
    *   Resolved `RuntimeDistCleaner` dedicated server crashes for the `GlobalSettingsMenu` and `MotdEditorMenu` by wrapping clientbound Forge packet lambdas in `DistExecutor.unsafeRunWhenOn(Dist.CLIENT)`.
    *   Restored missing ZGC (`-XX:+UseZGC`) warnings upon login for server administrators and local clients. Server OP's can now simply press `[Click to Fix]` in-game to auto-patch their server run-scripts, while clients get a convenient `[Copy Optimal Flags]` button directly into their clipboard.
- **MineBay/MineStacks Gating:**
    *   Fixed MineBay and MineStacks GUIs remaining accessible to players even after an admin disabled them via the Economy Management Settings screen. Toggle states saved correctly to `ForgeConfigSpec`, but subsequent `ModConfig.*.get()` calls in `OpenGuiPacket` returned stale cached `true` values due to NightConfig's `FileWatcher` autoreload racing with the main server thread after `SPEC.save()`.
    *   Added explicit `clearCache()` calls on all modified `ForgeConfigSpec.ConfigValue` entries in `SaveEconomySettingsPacket.handle()` after `set()` and `save()`, ensuring the next `.get()` re-reads from the updated backing config.
    *   Added missing `ModConfig.MINEBAY_ENABLED.get()` / `MINESTACKS_ENABLED.get()` gating checks to the `/minebay`, `/minestacks`, and `/casino` chat commands, which previously bypassed the `OpenGuiPacket` server-side check entirely. Players now receive a red `[SM+] ... is currently disabled.` chat message when attempting to use a disabled sub-feature via command.
    *   Applied all gating fixes to both Forge and Fabric modules for full loader parity.
    *   Fixed `/minebay`, `/minestacks`, and `/casino` commands bypassing the master economy feature toggle. The commands only checked individual sub-feature flags (`MINEBAY_ENABLED`/`MINESTACKS_ENABLED`) but not `FeatureManager.isFeatureEnabled("economy")`, so disabling the entire economy from ConfigScreen had no effect on these commands. Added the master economy check as a first-pass gate before the sub-feature check on both Forge and Fabric.
- **Economy Feature Toggle Cascade:**
    *   Fixed the master `economy` feature toggle (ConfigScreen) not cascading to sub-screens. Disabling the economy via the Feature Configuration screen now blocks server-side access to **all** economy sub-screens: Bank, MineBay, MineStacks, DailyTasks, and Achievements. Previously, only MineBay/MineStacks had individual config checks, while Bank/DailyTasks/Achievements were completely unguarded — and none of them checked the economy master toggle.
    *   Added `FeatureManager.isFeatureEnabled("economy")` gate to all 5 economy `GuiType` cases in `OpenGuiPacket.handle()` on both Forge and Fabric. Blocked attempts show `"[SM+] The economy system is currently disabled."` in red chat.
    *   Fixed market value tooltips (`ClientItemTooltipHandler`) remaining visible when the economy feature was disabled. Added `FeatureManager.isFeatureEnabled("economy")` early-return check before the existing `showMarketValueTooltips` admin setting check.
    *   Fixed feature state changes not propagating to other connected clients. `ToggleFeaturePacket.handle()` now broadcasts a `SyncFeatureStatesPacket` to **all** connected players after toggling, ensuring tooltips disappear and screens are blocked immediately without requiring reconnect.
    *   Economy sub-settings (`minebayEnabled`, `minestacksEnabled`, `showMarketValueTooltips`) are preserved in `ModConfig` when the economy feature is disabled, and automatically restored to their last configured state when re-enabled.
    *   Applied all changes to both Forge and Fabric modules for full loader parity.
- **Chat Prefix Unification:**
    *   Unified all inconsistent in-game chat prefixes (`[ServerManagement]`, `[SM+]`, `[ServerManagement+]`) to a single `[SM+]` prefix via a new `Constants.CHAT_PREFIX` constant. All player-facing chat messages across both Forge and Fabric now reference this constant, ensuring consistent branding.
    *   Cleaned up redundant `[ServerManagement]` prefixes from `BlurBackdrop` SLF4J logger messages (the logger name already identifies the source).
    *   `[MineBay]` sub-brand prefix intentionally preserved as-is.
    *   Shell script console output and GUI titles retain the full `ServerManagement+` branding where appropriate.
