# ServerManagement+ v2.1.2 (Minecraft 1.20.1)

This update backports critical bug fixes and configuration improvements from the 1.21.1 branches to the 1.20.1 release, while intentionally omitting the major UI/Networking rewrites (Multi-Item Rewards) to preserve 1.20.1 GUI stability.

## Features & Improvements
- **Configuration Consolidation**: Consolidated Economy settings (`Starting Balance`, `MineBay Enabled`, `MineStacks Enabled`, `Max Listings`, `Trade Blacklist`, etc.) into `serverconfig/servermanagement-server.toml` for standard runtime parity with modern branches.

## Bug Fixes
- **Economy NBT Serialization**: Fixed a critical exploit where items with NBT data (such as Enchanted Books, named items, or damaged tools) were losing their exact state upon server restart. The dynamic market engine and recipe pricing now correctly serialize and track NBT data in 1.20.1.
- **Repository Cleanup**: Cleaned up leftover testing directories and `TestModrinth.java` scripts that leaked into the production tree.
- **Versioning**: Adapted internal hardcoded updater target versions to `2.1.2-b1` due to the previous version being released already.
