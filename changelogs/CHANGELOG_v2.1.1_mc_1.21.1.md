# Changelog for v2.1.1 (mc_1.21.1)

## Economy Tools & HUD Redesign (2026-08-14)

### Added
- **Multiple Item Rewards:** Daily Task Templates now support assigning an unlimited number of items as rewards.
- **Node-Based Editor Upgrade:** Integrated the advanced visual `FreeRewardEditorWidget` into the Task Template Creator.

### Changed
- **Dynamic HUD Transitions:** The `StatsBarOverlay` (Daily Tasks HUD) now utilizes smooth mathematical interpolation for vertical transitions, preventing widgets from instantly popping into place.
- **Intelligent Text Truncation:** Long task descriptions in the HUD are now intelligently truncated in the middle, ensuring the task index prefix (e.g. `[1]`) and progress suffix (e.g. `[15/20]`) always remain visible and do not push off-screen.

### Fixed
- Fixed critical network packet crashes (`SaveTemplatePacket`, `SyncEconomyTemplatesPacket`) across Forge, Fabric, and NeoForge when transferring multi-item lists via modern 1.21.1 codecs.
