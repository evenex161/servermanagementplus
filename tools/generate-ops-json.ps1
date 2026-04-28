param(
    [string]$OutputFile = "run\ops.json",
    [string[]]$PlayerNames
)

# Handle comma-separated input from cmd (e.g., "Player1,Player2" becomes one string)
$names = @()
foreach ($p in $PlayerNames) {
    $names += $p -split ','
}
if ($names.Count -eq 0) {
    $names = @("Player1", "Player2")
}

function Get-OfflineUUID {
    param([string]$Name)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$Name")
    $md5 = [System.Security.Cryptography.MD5]::Create().ComputeHash($bytes)
    $md5[6] = ($md5[6] -band 0x0f) -bor 0x30  # UUID version 3
    $md5[8] = ($md5[8] -band 0x3f) -bor 0x80  # UUID variant 2
    $hex = [BitConverter]::ToString($md5).Replace('-','').ToLower()
    return "{0}-{1}-{2}-{3}-{4}" -f $hex.Substring(0,8),$hex.Substring(8,4),$hex.Substring(12,4),$hex.Substring(16,4),$hex.Substring(20,12)
}

$ops = @()
foreach ($name in $names) {
    $uuid = Get-OfflineUUID -Name $name
    $ops += @{
        uuid = $uuid
        name = $name
        level = 4
        bypassesPlayerLimit = $true
    }
}

$json = ConvertTo-Json $ops -Compress

# IMPORTANT: write UTF-8 WITHOUT BOM. PowerShell 5.1's `Set-Content -Encoding UTF8`
# emits a leading BOM (EF BB BF) which Minecraft's JSON reader treats as malformed,
# causing the server to silently load an empty ops list (= nobody is OP on join).
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$resolved = if ([System.IO.Path]::IsPathRooted($OutputFile)) { $OutputFile } else { Join-Path (Get-Location) $OutputFile }
$dir = [System.IO.Path]::GetDirectoryName($resolved)
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
[System.IO.File]::WriteAllText($resolved, $json, $utf8NoBom)
Write-Host "Generated $OutputFile with $($names.Count) operators: $($names -join ', ')"
