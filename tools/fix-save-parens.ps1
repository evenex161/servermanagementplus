$files = @(
  'forge\src\main\java\com\servermanagement\features\economy\TransactionManager.java',
  'forge\src\main\java\com\servermanagement\features\economy\BankInventory.java',
  'forge\src\main\java\com\servermanagement\features\minebay\MineBayListing.java',
  'forge\src\main\java\com\servermanagement\features\minebay\PriceItemEntry.java'
)
foreach ($f in $files) {
  $b = [System.IO.File]::ReadAllText($f)
  $new = $b -replace 'tag\.put\("([^"]+)",\s*(\w+)\.save\(new CompoundTag\(\)\)\)\);', 'tag.put("$1", $2.save(new CompoundTag()));'
  if ($new -ne $b) {
    [System.IO.File]::WriteAllText($f, $new, [System.Text.UTF8Encoding]::new($false))
    "fixed $f"
  } else {
    "unchanged $f"
  }
}
