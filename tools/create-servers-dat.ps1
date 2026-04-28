# Creates a Minecraft servers.dat (gzipped NBT) with a localhost server entry
# Usage: powershell -File create-servers-dat.ps1 <output-path>
param([string]$OutputPath)

if (-not $OutputPath) {
    Write-Host "Usage: create-servers-dat.ps1 <output-path>"
    exit 1
}

# NBT binary data: root compound -> list "servers" -> 1 entry with name + ip
$nbt = [byte[]]@(
    0x0A, 0x00, 0x00,                                                          # TAG_Compound (root, empty name)
    0x09, 0x00, 0x07, 0x73, 0x65, 0x72, 0x76, 0x65, 0x72, 0x73,              # TAG_List "servers"
    0x0A,                                                                       # element type: TAG_Compound
    0x00, 0x00, 0x00, 0x01,                                                    # list length: 1
    0x08, 0x00, 0x04, 0x6E, 0x61, 0x6D, 0x65,                                 # TAG_String "name"
    0x00, 0x10,                                                                 # string length: 16
    0x53, 0x4D, 0x2B, 0x20, 0x44, 0x65, 0x62, 0x75, 0x67, 0x20,              # "SM+ Debug "
    0x53, 0x65, 0x72, 0x76, 0x65, 0x72,                                        # "Server"
    0x08, 0x00, 0x02, 0x69, 0x70,                                              # TAG_String "ip"
    0x00, 0x09,                                                                 # string length: 9
    0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74,                     # "localhost"
    0x00,                                                                       # TAG_End (entry)
    0x00                                                                        # TAG_End (root)
)

$ms = [System.IO.MemoryStream]::new()
$gz = [System.IO.Compression.GZipStream]::new($ms, [System.IO.Compression.CompressionMode]::Compress)
$gz.Write($nbt, 0, $nbt.Length)
$gz.Close()

$parentDir = Split-Path -Parent $OutputPath
if ($parentDir -and -not (Test-Path $parentDir)) {
    New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
}

[System.IO.File]::WriteAllBytes($OutputPath, $ms.ToArray())
Write-Host "Created $OutputPath ($($ms.ToArray().Length) bytes)"
