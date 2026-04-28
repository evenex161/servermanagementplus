# ServerManagement+ GUI Testing Checklist
> Use with the debug overlay (F3+M) to verify layouts. Run `test.bat client` to launch.

---

## Global Checks (every screen)
- [ ] Open with F3+M debug overlay active
- [ ] Panel bounding box (cyan) fits within screen at default GUI scale
- [ ] Panel bounding box fits at GUI Scale: 1 (small)
- [ ] Panel bounding box fits at GUI Scale: 4 (large)
- [ ] No widget bounding boxes (green outlines) overlap
- [ ] No widgets extend outside the panel bounds
- [ ] All button text is fully visible (not clipped)
- [ ] ESC closes the screen cleanly (no crash, no orphaned state)
- [ ] Screen can be reopened immediately after closing

---

## 1. DashboardScreen (400x330)
**Open**: `/sm` command or keybind
- [ ] All 8 DashboardCards render in 3x3 grid (last cell = Close)
- [ ] Each card icon, title, and description text visible
- [ ] Hover highlight visible on each card
- [ ] Click each card → correct screen opens:
  - [ ] World Manager → WorldListScreen
  - [ ] Player Manager → PlayerManagerScreen
  - [ ] Console → ConsoleScreen
  - [ ] Global Settings → GlobalSettingsScreen
  - [ ] Economy → EconomyManagementScreen
  - [ ] Performance → PerformanceSettingsScreen
  - [ ] MOTD Editor → MotdEditorScreen
  - [ ] Mod Settings → ConfigScreen
- [ ] Close button works

---

## 2. ConfigScreen (320x330)
**Open**: Dashboard → Mod Settings
- [ ] All 6 toggle switches render with labels
- [ ] Toggle labels align with switches (check with debug overlay)
- [ ] Each toggle responds to click (visual state change)
- [ ] Toggle state persists after closing & reopening
- [ ] ← Dashboard button returns to DashboardScreen
- [ ] Close button works

---

## 3. ConsoleScreen (500x300)
**Open**: Dashboard → Console
- [ ] ConsoleOutput widget shows server log lines
- [ ] Scroll down with mouse wheel works
- [ ] Scroll up with mouse wheel works
- [ ] Shift+scroll horizontal scrolling works
- [ ] Scrollbar thumb visible and proportional
- [ ] Command input EditBox accepts text
- [ ] Enter key sends command
- [ ] Send button sends command
- [ ] Command appears in console output (green color)
- [ ] Clear button clears console output
- [ ] Error lines show red, warnings yellow, debug gray
- [ ] ← Back button works
- [ ] Close button works

---

## 4. PlayerManagerScreen (320x240)
**Open**: Dashboard → Player Manager
- [ ] Online player list shows (up to 6 buttons)
- [ ] Click player name → action menu appears
- [ ] ← Back returns to player list
- [ ] Spectate Player button functions
- [ ] View Inventory button functions
- [ ] ← Dashboard button returns to dashboard
- [ ] With 0 players online: empty state message shown
- [ ] With 7+ players: scroll/pagination works

---

## 5. WorldListScreen (350x240)
**Open**: Dashboard → World Manager
- [ ] Dimension list populates (overworld, nether, end)
- [ ] Click dimension → WorldDetailScreen opens
- [ ] Refresh button updates the list
- [ ] With many dimensions: scroll offset works
- [ ] ← Dashboard button works
- [ ] Close button works

---

## 6. WorldDetailScreen (350x230)
**Open**: WorldList → click a dimension
- [ ] Dimension name shown in title
- [ ] Portal toggle switches visible (nether/end portals, chat)
- [ ] Portal type button cycles: both → nether → end
- [ ] Set Timer button works (with EditBox input)
- [ ] Clear button resets timer
- [ ] Conditional toggles: nether portal switch hidden in The End, etc.
- [ ] Timer EditBox accepts numeric input
- [ ] ← Back returns to WorldListScreen
- [ ] Close button works

---

## 7. GlobalSettingsScreen (320x200)
**Open**: Dashboard → Global Settings
- [ ] Chat Isolation toggle renders with label
- [ ] Tab Isolation toggle renders with label
- [ ] Toggle clicks change state
- [ ] ← Dashboard button works
- [ ] Close button works

---

## 8. PerformanceSettingsScreen (380x340)
**Open**: Dashboard → Performance
- [ ] 3 tab buttons visible: Toggles, Settings, Stats
- [ ] Toggles tab: 8 toggle switches render with labels
- [ ] Settings tab: 10 value adjustment pairs (−/+) render
- [ ] Stats tab: performance statistics display
- [ ] Tab switching works cleanly (widgets rebuild)
- [ ] −/+ buttons change values
- [ ] Scroll works when content exceeds panel height
- [ ] ← Dashboard button works
- [ ] Close button works

---

## 9. MotdEditorScreen (420x330)
**Open**: Dashboard → MOTD Editor
- [ ] Line 1 and Line 2 EditBoxes render
- [ ] 16 color swatch buttons visible
- [ ] Click color swatch → inserts color code at cursor
- [ ] 6 format buttons (B/I/U/S/?/R) visible and clickable
- [ ] Rainbow button works
- [ ] Gradient button works
- [ ] Live preview renders below edit area
- [ ] Advanced mode toggle switches to raw &-code editing
- [ ] activeLineIndex switches between Line 1 and Line 2
- [ ] Clear button clears both lines
- [ ] Save MOTD button sends to server
- [ ] Unsaved changes dialog appears if navigating away
- [ ] ← Dashboard button works

---

## 10. BankScreen (440x340)
**Open**: `/bank` command
- [ ] 3 tab buttons: Account, Transfer, Requests
- [ ] Quick-nav buttons: Daily Tasks, Achievements, MineBay, MineStacks
- [ ] **Account tab**:
  - [ ] Balance display visible
  - [ ] Transaction list populates
  - [ ] Pagination (← Previous / Next →) works with 5+ transactions
- [ ] **Transfer tab**:
  - [ ] Player name EditBox accepts text
  - [ ] Amount EditBox accepts numeric text
  - [ ] Send Money button works
- [ ] **Requests tab**:
  - [ ] Incoming/Outgoing sub-tabs work
  - [ ] Request list populates
  - [ ] Pay / Deny / Cancel buttons function
  - [ ] + New Request button opens request form
  - [ ] Request form fields (name, amount, message) work
  - [ ] Send Request button works
  - [ ] Pagination works with 4+ requests
- [ ] ← Dashboard button works
- [ ] Close button works

---

## 11. DailyTasksScreen (400x430)
**Open**: Bank → Daily Tasks
- [ ] 3 task slots render with descriptions and progress bars
- [ ] Progress bar animations play smoothly
- [ ] Claim button appears when task complete
- [ ] Claim button click triggers claim animation
- [ ] Free reward slot renders separately
- [ ] Claim free reward button works
- [ ] ← Bank button works
- [ ] Close button works

---

## 12. AchievementsScreen (350x260)
**Open**: Bank → Achievements
- [ ] Achievement list populates
- [ ] Pagination works (← Previous / Next →)
- [ ] Achievement progress shown
- [ ] ← Bank button works
- [ ] Close button works

---

## 13. EconomyManagementScreen (600x450) — Admin only
**Open**: Dashboard → Economy
- [ ] 3 tabs: Task Templates, Free Reward, Statistics
- [ ] **Task Templates tab**:
  - [ ] Template list renders
  - [ ] + New Template button works
  - [ ] Edit button enters edit form
  - [ ] Delete button removes template
  - [ ] Enabled/Disabled toggle works
  - [ ] Pagination (▲/▼) works
  - [ ] Search box filters templates
  - [ ] Save / Cancel in edit mode work
  - [ ] Edit form fields (description, goal, reward) accept input
- [ ] **Free Reward tab**:
  - [ ] Free reward amount EditBox works
  - [ ] Cooldown EditBox works
  - [ ] Save button works
- [ ] **Statistics tab**:
  - [ ] Stats display renders
  - [ ] ↻ Refresh button works
- [ ] ← Dashboard button works
- [ ] Close button works

---

## 14. MineBayScreen (600x400) — MOST COMPLEX
**Open**: `/minebay` command
### State: BROWSE
- [ ] All listings render as cards
- [ ] Each card shows item icon, seller, price
- [ ] Pagination arrows work with many listings
- [ ] "All Listings" / "My Listings" toggle works
- [ ] "+ Create New" button → CREATE_STEP1
- [ ] Click listing card → VIEW_DETAILS
- [ ] Scissor clipping: content area doesn't bleed

### State: CREATE_STEP1
- [ ] Item offering slot visible (inventory shown)
- [ ] Place item in offering slot
- [ ] "Next >" button → CREATE_STEP2

### State: CREATE_STEP2
- [ ] Price item selection (up to 3 items)
- [ ] Money price EditBox works
- [ ] Margin percent EditBox works
- [ ] PaymentMode selection (Balance / Items)
- [ ] Stack/Count mode toggle for price items
- [ ] "< Back" returns to CREATE_STEP1
- [ ] "Next >" → CREATE_STEP3

### State: CREATE_STEP3
- [ ] Listing preview renders correctly
- [ ] Confirm button creates listing
- [ ] Cancel button returns to BROWSE

### State: VIEW_DETAILS
- [ ] Item details render (name, description, seller)
- [ ] Price display correct
- [ ] "Buy Now" button → BUY_CONFIRM
- [ ] "Negotiate" button → MAKE_OFFER
- [ ] Back button → BROWSE

### State: BUY_CONFIRM
- [ ] Confirmation dialog shows item + price
- [ ] Confirm / Cancel buttons work

### State: MAKE_OFFER
- [ ] Offer slots visible (inventory shown)
- [ ] Place offer items
- [ ] Submit offer button works
- [ ] Cancel button → VIEW_DETAILS

### State: VIEW_MY_LISTINGS
- [ ] Own listings render
- [ ] Edit button works (isEditMode)
- [ ] Delete button → DELETE_CONFIRM
- [ ] View Offers button → VIEW_OFFERS

### State: DELETE_CONFIRM
- [ ] Confirmation dialog for deletion
- [ ] Confirm / Cancel work

### State: VIEW_OFFERS
- [ ] Offer list for selected listing
- [ ] Accept / Decline buttons work
- [ ] Back button returns

### Inventory integration
- [ ] Player inventory shows only in states: CREATE_STEP1, MAKE_OFFER
- [ ] Inventory hides correctly in all other states
- [ ] Slot visibility toggles work (no ghost slots)

---

## 15. MineStacksScreen (400x220)
**Open**: `/minestacks` command
### State: MENU
- [ ] 5 game mode buttons render (Coin Flip, Dice Roll, Slot Machine, Roulette, Stats)
- [ ] Each button clickable → enters game mode
- [ ] ← Dashboard button works

### State: COIN_FLIP / DICE_ROLL
- [ ] Bet amount EditBox accepts numeric input
- [ ] Bet type toggle (money vs item) works
- [ ] Betting slot visible when item mode selected
- [ ] Play button works
- [ ] Tension animation plays (3s build + 0.5s ending)
- [ ] Win: particle explosion effect + screen shake
- [ ] Lose: appropriate visual feedback
- [ ] Result text visible after animation
- [ ] ← Back returns to MENU

### State: SLOT_MACHINE
- [ ] 3 slot reels render
- [ ] Spin animation plays
- [ ] Matching results display correctly
- [ ] Win/lose effects work

### State: ROULETTE
- [ ] Number/color selection works
- [ ] Bet placement works
- [ ] Spin animation plays
- [ ] Result highlights correctly

### State: STATS
- [ ] Win/loss statistics display
- [ ] Per-game breakdown visible

### Inventory integration
- [ ] Betting slot shows only during item bet mode
- [ ] Inventory toggling works per game mode
- [ ] GamblingSlot enabled/disabled toggle works

---

## 16. PortalTimerScreen (300x240)
**Open**: WorldDetail → Set Timer
- [ ] Portal type cycle button works (both/nether/end)
- [ ] Hours/Minutes/Seconds EditBoxes accept input
- [ ] Quick-set buttons: 5 min, 10 min, 30 min, 1 hour
- [ ] Start Timer button sends to server
- [ ] Cancel button returns
- [ ] Close button works

---

## 17. ServerManagementScreen (300x180)
**Open**: Non-admin mod settings
- [ ] 3 toggle switches visible with labels
- [ ] Toggles respond to clicks
- [ ] Close button works

---

## 18. ItemPickerScreen (195x280)
**Open**: MineBay price item selection
- [ ] Search EditBox filters items
- [ ] Item grid populates with matching items
- [ ] Scroll through item list works
- [ ] Click item selects it and closes picker
- [ ] Screen size clamped within window bounds

---

## 19. OTAUpdateScreen (fullscreen)
**Open**: Automatic on version mismatch
- [ ] Progress bar renders during download phase
- [ ] "Complete" state shows success message
- [ ] "Failed" state shows error message
- [ ] Screen is non-interactive (no escape/close until complete)

---

## Debug Overlay Verification
- [ ] F3+M toggles overlay on/off
- [ ] Info panel shows: screen size, GUI scale, ScreenScaler factor
- [ ] Panel outline (cyan) visible on container screens
- [ ] Widget outlines green (active), red (inactive)
- [ ] Slot overlays yellow with index numbers
- [ ] Mouse crosshair tracks cursor
- [ ] Hovered widget shows class name + message
- [ ] Overlay only appears on mod screens, not vanilla
