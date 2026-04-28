# Clean up neoforge files we accidentally wrote
$untracked = git ls-files --others --exclude-standard | Select-String '^neoforge/' | ForEach-Object { $_.Line.Trim() }
"untracked neoforge files: $($untracked.Count)"
foreach ($f in $untracked) {
    if (Test-Path $f) { Remove-Item $f -Force -ErrorAction SilentlyContinue }
}
# Remove now-empty dirs (innermost first)
if (Test-Path neoforge) {
    $dirs = Get-ChildItem neoforge -Directory -Recurse | Sort-Object { $_.FullName.Length } -Descending
    foreach ($d in $dirs) {
        if ((Get-ChildItem $d.FullName -Recurse -File -ErrorAction SilentlyContinue).Count -eq 0) {
            Remove-Item $d.FullName -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    if (Test-Path neoforge) {
        if ((Get-ChildItem neoforge -Recurse -File -ErrorAction SilentlyContinue).Count -eq 0) {
            Remove-Item neoforge -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}
"after cleanup, neoforge dir exists: $(Test-Path neoforge)"

# Filter chunk-rest.txt to forge-only
Get-Content tools\chunk-rest.txt | Where-Object { $_ -like 'forge/*' } | Set-Content -Encoding utf8 tools\chunk-rest.txt
"forge entries: $((Get-Content tools\chunk-rest.txt).Count)"
