param(
    [Parameter(Mandatory=$true)] [string] $ListFile
)

$root = (Resolve-Path .).Path
$files = Get-Content $ListFile | Where-Object { $_ -and -not $_.StartsWith('#') }

foreach ($f in $files) {
    $f = $f.Trim()
    if (-not $f) { continue }
    $abs = Join-Path $root ($f -replace '/','\')
    $dir = Split-Path $abs -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $content = git show "mc/1.21.1-forge:$f" 2>$null
    if ($LASTEXITCODE -ne 0 -or $null -eq $content) {
        Write-Warning "MISSING in mc/1.21.1-forge: $f"
        continue
    }
    $text = $content -join "`r`n"
    [System.IO.File]::WriteAllText($abs, $text + "`r`n", [System.Text.UTF8Encoding]::new($false))
    Write-Host "wrote $f"
}
