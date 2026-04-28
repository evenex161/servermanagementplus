$files = @(
  'forge\src\main\java\com\servermanagement\features\economy\OverflowInventoryManager.java',
  'forge\src\main\java\com\servermanagement\features\economy\TransactionManager.java',
  'forge\src\main\java\com\servermanagement\features\minebay\MineBayManager.java',
  'forge\src\main\java\com\servermanagement\features\economy\EconomyManager.java'
)
foreach ($f in $files) {
  $b = [System.IO.File]::ReadAllText($f)
  # readCompressed(X)) → readCompressed(X)
  $new = $b -replace 'NbtIo\.readCompressed\((\w+)\)\)', 'NbtIo.readCompressed($1)'
  if ($new -ne $b) {
    [System.IO.File]::WriteAllText($f, $new, [System.Text.UTF8Encoding]::new($false))
    "fixed $f"
  } else {
    "unchanged $f"
  }
}
