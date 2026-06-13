# ServerManagement+ v2.1.0

**Minecraft 1.20.1** — Forge 47.4.0 / Fabric API 0.92.8

v2.1.0 is the **1.20.1 backport** of the v2.1.0 release shipped on the `mc/1.21.1-forge` branch. It brings the full feature set, GUI polish, networking refactor, and bug-fix work from the 1.21.1 line back to Minecraft 1.20.1.

---

## Now Available on Forge and Fabric

ServerManagement+ now ships for both Forge and Fabric loaders on Minecraft 1.20.1. Pick the JAR that matches your server — every feature works the same on both loaders.

- Forge 47.4.0 (Minecraft 1.20.1)
- Fabric API 0.92.8 + Fabric Loader 0.19.2 (Minecraft 1.20.1)

> Note: Both loaders are feature-complete and runtime-tested for the major flows. Please report any loader-specific issues you run into.

---

## GUI Polish — Every Screen

Every screen in the mod was reviewed and cleaned up. No more clipped buttons, overlapping text, or wasted space.

### World Blur Backdrop (Performance Optimized)
- Manual post-processing `PostChain` shader manually renders the world blurred behind GUIs (equivalent of 1.21+ `renderBlurredBackground`).
- Compiled shader is cached and reused across GUI screen transitions to completely eliminate the synchronous OpenGL compilation stutter/lag when opening screens.
- Automatic self-healing resets the shader chain on resize or resource reload exceptions.

### Admin Dashboard
- Close button now sits inside the panel instead of hanging off the bottom.
- Cards re-sized so everything fits cleanly at any window size.
- Card titles and descriptions are guaranteed to fit inside the card border at every GUI scale (including Auto / Scale 4 / Scale 5 on small windows).

### MineBay
- **Step 1 (Place item)**: Clear step indicators (1 / 2 / 3), a visible drop-zone frame around the offering slot, and a wider "Next" button.
- **Step 2 (Set prices)**: Wider price input columns, bigger amount/mode/clear buttons, and a preview area on the right of each price row showing the picked item or a "Click to pick" hint.
- **Edit Listing — Back button**: Pressing Back no longer dumps you back into the place-item step. It now returns to the screen you came from. If you've made changes, you'll see a "Discard changes / Keep editing" prompt so you can't lose work by accident.
- Step indicators no longer overlap the header title.

### MineStacks
- Slightly bigger panel for more breathing room.

### World Manager
- Larger World Detail panel with the Portal Timer label moved above its controls (was below).
- Bigger timer controls and a wider portal type button.
- Symmetric Back / Close buttons.

### Player Manager
- Pressing **E** while typing a player name no longer closes the screen.

### Server Console
- Bigger panel and bigger Back button.
- Title text shifted so it no longer overlaps the back arrow.
- Larger Send / Clear buttons.

### Global Settings (Chat & Tab Isolation)
- Toggling Chat or Tab Isolation off now restores the full player list immediately instead of waiting up to a second.

### Economy Management
- All three template cards now fit inside the panel without overflow.
- Buttons (Edit / Delete / Enabled toggle) properly stay inside their cards.
- Free Reward and Task Template item slots can be clicked at any GUI scale.
- Stats page is now scrollable.

### Performance Settings
- Bigger panel — all eight toggles now fit without clipping.
- Tab buttons evenly spaced; bottom buttons (Dashboard / Close) symmetric.
- Stats tab is scrollable.

### MOTD Editor
- Discard-changes confirmation buttons work at any GUI scale.

### Mod Configuration (`/smconfig`)
- `/smconfig` now opens the **current** config screen (with Economy, Server Performance, MOTD toggles and feature descriptions) instead of an old 3-toggle screen.
- Added per-feature description text under each name.
- Alternating row backgrounds and subtle dividers for easier reading.

---

## Chat & Tab Isolation — Now Actually Works

The Chat Isolation and Tab Isolation toggles in **Global Settings** were not actually doing anything in v2.0.0. Both have been completely fixed.

- **Chat Isolation**: When enabled, chat is restricted to players in the same dimension by default. Connected dimensions you set up still act as cross-dimension chat bridges.
- **Tab Isolation**: Players in other dimensions are now correctly hidden from your tab list (and you from theirs).
- **Returning from another dimension**: Players who travel away and come back are immediately visible again to everyone in tab — no more "ghost" players that needed a reconnect.
- **Player models stay visible** after dimension travel — fixes the bug where other players' 3D models would become invisible while their tab entry looked fine.
- **Toggling off** restores the full player list immediately instead of after the next sync cycle.

---

## /spectate — Many Fixes

The Player Manager spectate feature had a long list of issues in v2.0.0. All fixed:

- `/spectate <player>` now correctly opens the mod's spectate (it used to collide with vanilla's `/spectate`, which would return "Player is not in spectator mode").
- **Same-dimension spectate**: First-person hands and the hotbar are now hidden, and your view is locked to the target — a proper spectate camera.
- **Cross-dimension spectate**: Your body no longer follows the target around bumping into them. You're invisible to the target and don't collide with anything.
- **Tab list disguise**: Other players no longer see your name turn italic-gray (the vanilla "spectator mode" styling) — to everyone else you still appear as your normal game mode.
- **Camera no longer freaks out** before you go out of range — the cancel distance now follows the server's view distance.
- **Cross-dimension travel by the target**: You now seamlessly follow them through portals and back, with the camera reattaching cleanly. No more stuck-at-spawn camera.
- **Stuck invincibility fix**: If the server crashed while you were spectating, you could end up permanently invincible after rejoining. The mod now detects and clears this on login.
- **No more duplicate "Now spectating …" chat lines** — only one message per spectate.
- `/spectate <yourself>` now correctly tells you "You cannot spectate yourself" instead of running anyway.

---

## Other Command Fixes

- **`/teleportlobby`** now teleports you to the exact lobby coordinates set by `/setlobby` (was teleporting to the dimension's world spawn instead, ignoring the saved location).
- **`/setlobby`** now stores your exact position (including yaw/pitch) instead of a rounded block position with no rotation.

---

## Daily Tasks (Fabric)

- **Daily task progress is now tracked on Fabric** for block breaking, mob killing, item crafting, villager trading, and travel distance. Previously the events weren't wired up on Fabric, so no progress ever counted.
- **Live GUI updates**: Your Daily Tasks screen now refreshes immediately as progress is recorded — no need to close and re-open.

---

## MineBay Polish

In addition to the Step 1 / Step 2 / Edit Listing improvements above:

- Edit Listing's Back button now respects unsaved changes.
- Item-picker for price items reliably opens when you click a price slot, at any GUI scale.

---

## Behind the Scenes

A few changes you won't see directly but that improve the experience:

- **Record-Based Packets**: Parity achieved for all 72 network packet classes.
- **AsyncSaveScheduler**: Aligned to Java 17 daemon thread executors (replacing virtual threads from 1.21.1) to speed up economy, listings, and template saving with low overhead.
- **5-second heartbeat for Tab Isolation**: If your client ever drifts out of sync with the server, it self-corrects within 5 seconds.
- **Mod Session Cryptographic Verification**: Hardened packet validation using random session tokens exchanged upon login.
- **Automated Database Backups**: Restores safety by recursively backing up data folders to `serverdata/servermanagement/backups/` before configuration/database migrations.
- **Circular Recipe Loop Protection**: Item pricing update loops freeze after 25 cyclic iterations, guaranteeing convergence and preventing infinite startup cycles.

---

## Upgrade Notes

- v2.1.0 is **save-compatible** with v2.0.x — no world reset needed.
- If you've been editing TOML config files by hand, double-check the file after first start (the migration system will preserve your settings, but a manual review never hurts).
- If you're switching loaders, copy your `<world>/serverdata/servermanagement/` folder to the new server — your economy, listings, and templates carry over.

---

## Known Limitations

- The OTA update checker uses case-insensitive filename matches to resolve the CurseForge JAR path, eliminating previous missing JAR warnings.
- The 1.20.1 backport compares `ItemStack`s via `isSameItemSameTags` because component-aware serialization is not available on this version.
- Some hard-coded `Component.literal("§a…")` strings in source files contain mojibake (`┬º`) inherited from the 1.21.1 source — purely cosmetic in-game, does not affect compile or behavior. Will be cleaned up in a future patch release.
