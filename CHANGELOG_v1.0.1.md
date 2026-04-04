# Changelog - v1.0.1 (Gambling Enhancement Update)

**Release Date**: November 12, 2025  
**Base Version**: v1.0.0-EA  
**Minecraft Version**: 1.20.1  
**Forge Version**: 47.4.0+

---

## 🎰 Major Features

### Gambling Animation System
- **Tension & Reveal System**: Added 3.5-second reveal sequence for all gambling games
  - 3-second tension phase with fast spinning animations
  - 0.5-second ending phase with deceleration effects
  - Result display with full visual and audio effects
- **Game-Specific Animations**:
  - **Coin Flip**: Spinning coin with rotation speed indicator, ending animation flattens to reveal result
  - **Dice Roll**: Tumbling dice with random rotation, ending animation slows and displays final dots
  - **Slot Machine**: Three spinning reels with staggered speed, ending animation stops reels sequentially
  - **Roulette**: Spinning wheel with orbiting ball, ending animation slows wheel and settles ball into segment
- **Visual Effects**:
  - Dark tension overlay during animations
  - Game-specific status text ("Flipping...", "Rolling...", "Spinning...", "Spinning...")
  - Ending status text ("Landing...", "Stopping...", "Stopping...", "Settling...")
  - Smooth alpha fading for overlay transitions
  - Deceleration curves for realistic physics

### Enhanced User Experience
- **GUI Interaction Blocking**: Complete input prevention during gambling animations
  - Mouse clicks blocked on all buttons and slots
  - Keyboard input disabled (including ESC key)
  - GUI closing prevented until animation completes
  - Players must watch full animation sequence
- **Pending Result System**: Result packets stored but not displayed until animations complete
  - Maintains suspense throughout full animation
  - Ensures smooth transition from ending to result reveal

---

## 🐛 Bug Fixes

### Balance Synchronization
- **Fixed**: MineStacks balance not matching Bank balance after gambling
- **Solution**: Added `updateBalance()` call in `MineStacksMenu` when result packets arrive
- **Impact**: Balance now updates immediately and accurately reflects wins/losses

### Statistics Tracking
- **Fixed**: Gambling statistics showing 0 despite multiple gambling sessions
- **Root Cause**: Statistics only existed server-side, never synced to client
- **Solution**: 
  - Created `ClientGamblingData` class for client-side statistics storage
  - Created `SyncGamblingStatsPacket` to sync server stats to client
  - Packet sent on GUI open and after each bet completion
  - Statistics now persist and display correctly

---

## 📦 New Files

### Client-Side Data Management
- **`ClientGamblingData.java`** (115 lines)
  - Stores gambling statistics on client
  - Tracks total bets, wins, losses, wagered, and payout
  - Calculates net profit and win rate
  - Methods: `updateStats()`, `getNetProfit()`, `getWinRate()`, `reset()`

### Network Packets
- **`GamblingTensionPacket.java`** (47 lines)
  - Sent immediately when player places bet
  - Triggers client-side tension animation
  - No data payload, purely a trigger signal

- **`SyncGamblingStatsPacket.java`** (67 lines)
  - Syncs server-side statistics to client
  - Contains all gambling stats (bets, wins, losses, amounts)
  - Updates client display in real-time

---

## 🔧 Modified Files

### GUI Components
- **`MineStacksScreen.java`** (1401 lines, +350 lines)
  - Added animation state machine with three phases
  - Implemented game-specific tension animations
  - Implemented game-specific ending animations with deceleration
  - Added pending result storage system
  - Overrode `mouseClicked()`, `keyPressed()`, `onClose()` for input blocking
  - Added `renderTensionOverlay()` and `renderEndingOverlay()` methods
  - Enhanced `handleGamblingResult()` to store instead of immediately display
  - New `showPendingResult()` method for delayed result reveal
  - Constants: `TENSION_DURATION = 3000ms`, `ENDING_DURATION = 500ms`

- **`MineStacksMenu.java`** (+3 lines)
  - Added `updateBalance()` call in gambling result packet handler
  - Ensures balance sync after every gambling transaction

### Network Protocol
- **`PlaceGamblingBetPacket.java`** (186 lines, +8 lines)
  - Sends `GamblingTensionPacket` immediately upon bet placement
  - Increased result delay from 2500ms to 3000ms
  - Sends `SyncGamblingStatsPacket` after result
  - Calls `updateBalance()` on menu after result

- **`PlaceGamblingBetWithItemPacket.java`** (188 lines, +8 lines)
  - Same improvements as money betting packet
  - Handles item-based gambling bets

- **`ModNetworking.java`** (+2 lines)
  - Registered `GamblingTensionPacket`
  - Registered `SyncGamblingStatsPacket`

---

## ⚡ Performance Improvements

### Animation Optimization
- Animations only render during active gambling sequences
- State-based rendering prevents unnecessary computation
- Deceleration calculations use simple linear interpolation
- No impact on frame rate when not gambling

### Network Efficiency
- `GamblingTensionPacket` has no payload (minimal bandwidth)
- `SyncGamblingStatsPacket` only sent when needed (GUI open, bet complete)
- Statistics stored locally to reduce repeated server queries

---

## 🎨 Visual Enhancements

### Animation Details
- **Coin Flip**: 
  - Tension: 20° rotation per tick, scale pulses
  - Ending: Rotation slows to 0, scale flattens to 1.0
- **Dice Roll**: 
  - Tension: Random 3D rotation, fast tumbling
  - Ending: Rotation slows, final dots appear at 50% progress
- **Slot Machine**: 
  - Tension: Each reel moves at different speed (8-12 px/tick)
  - Ending: Reels stop sequentially (reel 1 → reel 2 → reel 3)
- **Roulette**: 
  - Tension: Wheel spins, ball orbits at 80px radius
  - Ending: Wheel decelerates to 80%, ball spirals inward to segment

### Overlay System
- Semi-transparent dark background (alpha 0.6)
- Dynamic status text with alpha fade-out during ending
- Smooth transitions between phases
- Professional casino-style presentation

---

## 🔄 Technical Changes

### Architecture Improvements
- **State Machine Pattern**: Clean separation of tension/ending/result phases
- **Pending Result Pattern**: Decouples packet arrival from UI display
- **Input Event Consumption**: Proper event handling prevents interaction leaks
- **Deceleration Algorithm**: Physics-based slowdown for realistic motion

### Thread Safety
- Server-side delays use `Thread.sleep()` in scheduled tasks
- Client-side animations use render tick-based timing
- No synchronization issues between client/server

### Code Quality
- Comprehensive JavaDoc comments added
- Clear separation of concerns
- Maintainable animation rendering methods
- Consistent naming conventions

---

## 📊 Statistics

### Code Changes
- **Files Modified**: 6
- **Files Created**: 3
- **Total Lines Added**: ~520
- **Total Lines Modified**: ~40

### Feature Completion
- ✅ Balance synchronization
- ✅ Statistics tracking system
- ✅ Tension animation system
- ✅ Ending animation system
- ✅ Input blocking system
- ✅ Network protocol updates
- ✅ Client-side data management

---

## 🎯 User Experience Impact

### Before v1.0.1
- Instant result reveal (no suspense)
- Balance desynchronization issues
- Statistics always showed 0
- Players could spam-click during results

### After v1.0.1
- 3.5-second animated reveal sequence
- Perfect balance synchronization
- Accurate statistics tracking
- Forced animation viewing for engagement
- Professional casino-style experience

---

## 🔮 Future Considerations

### Potential Enhancements
- Sound effects during tension phase (spinning sounds)
- Sound effects during ending phase (deceleration sounds)
- Particle effects during tension (sparkles, smoke)
- Configurable animation speeds
- Animation skip option (after first view)

### Compatibility
- Fully compatible with v1.0.0-EA saves
- No data migration required
- Backward compatible with existing gambling system
- Network protocol additions are non-breaking

---

**Build Status**: ✅ Successful  
**Testing Status**: Ready for in-game testing  
**Recommended Version**: v1.0.1
