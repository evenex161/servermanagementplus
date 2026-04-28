$pairs = @(
  'util\AsyncSaveScheduler.java',
  'security\ItemStackTypeAdapter.java',
  'features\slimehead\SlimeHeadManager.java',
  'features\economy\ItemSupplyDemandTracker.java',
  'features\economy\MarginHistoryTracker.java',
  'features\economy\TransactionManager.java',
  'features\economy\BankInventory.java',
  'features\economy\OverflowInventoryManager.java',
  'features\economy\EconomyManager.java',
  'features\minebay\MineBayManager.java',
  'features\minebay\MineBayOffer.java',
  'features\minebay\MineBayListing.java',
  'features\minebay\PriceItemEntry.java',
  'features\worldmanager\TabListIsolationHandler.java',
  'features\playermanager\PlayerManagerSingleton.java',
  'gui\ScalableContainerScreen.java'
)
foreach ($p in $pairs) {
  $src = "forge\src\main\java\com\servermanagement\$p"
  $dst = "fabric\src\main\java\com\servermanagement\$p"
  if (Test-Path $src) { Copy-Item -Force $src $dst; "copied $p" }
}
