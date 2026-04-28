param([Parameter(Mandatory=$true)] [string] $ListFile)

# Find files locally that exist in forge/src/main/java/com/servermanagement
# but are NOT listed in $ListFile, and are not the manually-fixed ones,
# and don't live under network/. Print their paths.

$root = (Resolve-Path 'forge\src\main\java\com\servermanagement').Path
$current = Get-ChildItem -Recurse -File $root | ForEach-Object {
    'forge/src/main/java/com/servermanagement/' + ($_.FullName.Substring($root.Length + 1) -replace '\\','/')
}
$target = Get-Content $ListFile
$keepNetwork = $current | Where-Object { $_ -like 'forge/src/main/java/com/servermanagement/network/*' }
$kept = @(
    'forge/src/main/java/com/servermanagement/util/AsyncSaveScheduler.java',
    'forge/src/main/java/com/servermanagement/security/ItemStackTypeAdapter.java'
)
$obsolete = $current | Where-Object {
    $_ -notin $target -and $_ -notin $keepNetwork -and $_ -notin $kept
}
"obsolete count: $($obsolete.Count)"
$obsolete | Set-Content -Encoding utf8 tools\obsolete.txt
"wrote tools\obsolete.txt"
