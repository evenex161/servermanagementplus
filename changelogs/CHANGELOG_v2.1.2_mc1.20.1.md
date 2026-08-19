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
