$victims = @(
  'forge\src\main\java\com\servermanagement\gui\ScalableContainerScreen.java',
  'forge\src\main\java\com\servermanagement\gui\minebay\ItemPickerScreen.java',
  'forge\src\main\java\com\servermanagement\gui\minebay\MineBayScreen.java',
  'forge\src\main\java\com\servermanagement\gui\screen\PerformanceSettingsScreen.java',
  'forge\src\main\java\com\servermanagement\gui\screen\PlayerManagerScreen.java',
  'forge\src\main\java\com\servermanagement\gui\widgets\ConsoleOutput.java'
)
foreach ($f in $victims) {
  git checkout HEAD -- $f
  "reverted $f"
}
