$branches = @("mc/1.20.1", "mc/1.21.1", "mc/1.21.4", "mc/1.21.11")
$baseDir = "c:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge"
$releaseDir = Join-Path $baseDir "releases"

# Create clean releases folder
if (Test-Path $releaseDir) { Remove-Item -Recurse -Force $releaseDir }
New-Item -ItemType Directory -Path $releaseDir | Out-Null

foreach ($b in $branches) {
    Write-Host "==================== Building Release for: $b ===================="
    git checkout $b
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to checkout branch $b"
        break
    }
    # Clean up any leftover build directories from previous loops/branches
    Get-ChildItem -Path $baseDir -Directory -Filter "build" -Recurse | ForEach-Object {
        if (Test-Path $_.FullName) { Remove-Item -Recurse -Force $_.FullName -ErrorAction SilentlyContinue }
    }
    
    # Run clean build
    .\gradlew clean build
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed on branch $b"
        break
    }
    
    # Create target directory
    $folderName = $b.Replace("mc/", "mc-")
    $targetDir = Join-Path $releaseDir $folderName
    New-Item -ItemType Directory -Path $targetDir | Out-Null
    
    # Copy all built JARs from fabric, forge, neoforge, and paper build/libs
    $builtJars = Get-ChildItem -Path $baseDir -Filter "*.jar" -Recurse | Where-Object { 
        $_.FullName -match "build\\libs" -and $_.Name -notmatch "sources|javadoc|buildSrc|wrapper" 
    }
    
    foreach ($jar in $builtJars) {
        Write-Host "  Copying: $($jar.Name)"
        Copy-Item -Path $jar.FullName -Destination $targetDir
    }
    
    # Copy all changelog markdown files
    $changelogs = Get-ChildItem -Path $baseDir -Filter "CHANGELOG*.md"
    foreach ($cl in $changelogs) {
        Write-Host "  Copying: $($cl.Name)"
        Copy-Item -Path $cl.FullName -Destination $targetDir
    }
}

# Switch back to baseline
git checkout mc/1.20.1
Write-Host "All releases successfully compiled and gathered under $releaseDir"
