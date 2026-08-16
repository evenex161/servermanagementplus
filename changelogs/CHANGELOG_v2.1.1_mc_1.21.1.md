# ServerManagement+ v2.1.1 (Branch: mc/1.21.1) — *Superseded by v2.1.2*

## 🐛 Bug Fixes & Economy Rebalancing
- **Comprehensive Economy Stabilization**: Completely overhauled the economy engine's scaling logic to prevent price collapses. On established servers, intrinsic item values (like Stone or Diamond) will no longer crash to the $0.50 floor when players spend their starting balances. The inflation system now correctly ensures that prices only scale *upwards* as server wealth grows, preserving the baseline value of all items.
- **Dynamic Pricing Caps**: Tamed the "Goldrush" effect! Items with zero supply will no longer skyrocket to absurd multi-million dollar prices due to runaway multipliers. We've introduced a much smoother scarcity curve and hard price caps for unknown items.
- **Supply/Demand Curve Smoothing**: Abundant building materials (like Cobblestone or Granite) will retain much more of their value on the market. The aggressive supply deflation penalty has been softened significantly.
- **Client-Side Price Sync**: Fixed an issue where players would see different prices on their client compared to the actual server economy calculations.
- **Reverse Crafting Loop Fixed**: Removed recursive recipe calculations (e.g., stone buttons devaluing stone blocks) that were causing raw material values to spiral downwards. Prices now strictly flow upward from raw materials to crafted products.
