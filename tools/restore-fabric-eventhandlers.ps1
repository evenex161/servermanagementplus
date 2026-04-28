$f = @(
  'fabric/src/main/java/com/servermanagement/features/slimehead/SlimeHeadManager.java',
  'fabric/src/main/java/com/servermanagement/features/worldmanager/TabListIsolationHandler.java',
  'fabric/src/main/java/com/servermanagement/features/playermanager/PlayerManagerSingleton.java'
)
foreach ($x in $f) {
  $content = git show "mc/1.21.1-forge:$x"
  $abs = (Resolve-Path .).Path + '\' + ($x -replace '/','\')
  [System.IO.File]::WriteAllText($abs, ($content -join "`r`n") + "`r`n", [System.Text.UTF8Encoding]::new($false))
  "restored $x"
}
