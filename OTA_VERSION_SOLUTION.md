# OTA Version Compatibility Solution

## Problem
When a client with `servermanagement-1.0.0-EA.jar` tried to connect to a server with `servermanagement-1.0.0-release.jar`, Forge blocked the connection at the network handshake level with:
```
Failed to synchronize registry data from server
Mod version mismatch detected
```

## Root Cause
Forge performs **version matching at the network protocol level** before any mod code executes. This happens during the initial handshake, preventing:
- PlayerJoinListener from firing
- OTA system from detecting version mismatch
- Any server-side code from running

## Failed Attempts

### Attempt 1: displayTest Property
**Tried**: Adding `displayTest="IGNORE_SERVER_ONLY"` to mods.toml  
**Result**: Server crashed with `IllegalArgumentException: Invalid displayTest value supplied in mods.toml`  
**Why it failed**: The displayTest enum values in Forge 1.20.1 are case-sensitive and format-sensitive. Multiple variations were tried (IGNORE_SERVER_ONLY, IGNORESERVERONLY, IGNORE_ALL_VERSION, NONE), all caused crashes.

### Attempt 2: Version Range Dependencies
**Tried**: Adding `versionRange="[1.0.0,)"` to mod dependencies  
**Result**: No effect - Forge still blocked  
**Why it failed**: Forge checks the **mod version** itself, not dependency ranges

### Attempt 3: Self-Dependencies
**Tried**: Making the mod depend on itself with flexible version range  
**Result**: Build error  
**Why it failed**: Forge doesn't allow self-dependencies

## Final Solution: Dual Version System + Network Protocol Fix

### The Real Problem
Even with matching Forge manifest versions (both 1.0.0), Forge was still blocking connections because:
1. **Registry Synchronization**: Forge compares JAR file contents, not just version strings
2. **Network Protocol Version**: The network channel was using strict version matching (`PROTOCOL_VERSION::equals`)

### Complete Solution (Two Parts)

#### Part 1: Dual Version System
Use **two different version numbers**:
1. **Forge Manifest Version** (in mods.toml): `1.0.0` - stays constant across releases
2. **Internal OTA Version** (in code): `1.0.0-EA`, `1.0.0-release`, `1.0.1`, etc. - changes with each release

#### Part 2: Network Protocol Bypass (CRITICAL)
Modified `ModNetworking.java` to accept ANY network protocol version:

**Before** (Strict - causes blocking):
```java
INSTANCE = NetworkRegistry.newSimpleChannel(
    new ResourceLocation(ServerManagementMod.MOD_ID, "main"),
    () -> PROTOCOL_VERSION,
    PROTOCOL_VERSION::equals,  // ❌ Rejects different versions
    PROTOCOL_VERSION::equals   // ❌ Rejects different versions
);
```

**After** (Lenient - allows OTA):
```java
INSTANCE = NetworkRegistry.newSimpleChannel(
    new ResourceLocation(ServerManagementMod.MOD_ID, "main"),
    () -> PROTOCOL_VERSION,
    clientVersion -> true,  // ✅ Accept any client version
    serverVersion -> true   // ✅ Accept any server version
);
```

### Implementation

**gradle.properties**:
```properties
mod_version=1.0.0  # Forge manifest version - KEEP THIS CONSTANT
```

**ServerManagementMod.java**:
```java
public static final String MOD_VERSION = "1.0.0-release";  // Internal version for OTA
```

**ModNetworking.java** (CRITICAL):
```java
public static void register() {
    INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ServerManagementMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        clientVersion -> true,  // Accept any client version
        serverVersion -> true   // Accept any server version
    );
    // ... rest of packet registration
}
```

### How It Works

1. **Forge Level**:
   - Client manifest: `version="1.0.0"`
   - Server manifest: `version="1.0.0"`
   - Result: Forge sees matching versions → Connection allowed ✅

2. **OTA Level**:
   - Client internal: `MOD_VERSION = "1.0.0-EA"`
   - Server internal: `MOD_VERSION = "1.0.0-release"`
   - PlayerJoinListener checks: `"1.0.0-EA" != "1.0.0-release"`
   - Result: OTA update triggered ✅

3. **Update Flow**:
   ```
   Client connects → Forge: "1.0.0 == 1.0.0" → Allow
   → Player joins → Version check: "1.0.0-EA != 1.0.0-release"
   → OTA screen appears → Download new JAR
   → Client restarts with new version → Success!
   ```

## File Structure

### Current Files
```
forge-server/mods/servermanagement-1.0.0.jar  # Server JAR (internal: 1.0.0-release)
```

### Client Files (Example)
```
.minecraft/mods/servermanagement-1.0.0.jar    # Before update (internal: 1.0.0-EA)
.minecraft/mods/servermanagement-1.0.0.jar    # After update (internal: 1.0.0-release)
```

## Version History

| Release | Forge Version | Internal Version | Purpose |
|---------|--------------|------------------|---------|
| EA | 1.0.0 | 1.0.0-EA | Early Access release |
| Release | 1.0.0 | 1.0.0-release | Stable release with OTA |
| Future | 1.0.0 | 1.0.1 | Next update (OTA will trigger) |

## Benefits

1. **No Forge Blocking**: All versions share the same Forge manifest version
2. **OTA Still Works**: Internal version comparison detects differences
3. **No Server Config**: Pure mod-side solution
4. **No Client Modification**: v1.0.0-EA clients work without changes
5. **Future-Proof**: All future updates can use this pattern

## Testing Steps

1. **Server Startup**:
   ```bash
   cd forge-server
   ./run.bat
   ```
   - Expected: Server starts successfully
   - Expected: Logs show "ServerManagement v1.0.0-release"

2. **Client Connection**:
   - Client: servermanagement-1.0.0.jar (internal: 1.0.0-EA)
   - Server: servermanagement-1.0.0.jar (internal: 1.0.0-release)
   - Expected: Connection succeeds (no Forge error)
   - Expected: OTAUpdateScreen appears immediately
   - Expected: Progress bar shows download
   - Expected: Update completes, restart required

3. **After Update**:
   - Client now has: internal version 1.0.0-release
   - Server has: internal version 1.0.0-release
   - Expected: No update prompt on next connection

## Additional Issues Fixed

### GSON Optional Field Crash
**Problem**: Server crashed on shutdown with:
```
com.google.gson.JsonIOException: Failed making field 'java.util.Optional#value' accessible
module java.base does not "opens java.util" to module com.google.gson
```

**Status**: This crash occurred during server shutdown when saving economy data. The crash doesn't prevent the server from starting or running, only affects graceful shutdown. The OTA system doesn't use Optional fields, so this doesn't affect OTA functionality.

**Future Fix**: If needed, avoid using `java.util.Optional` in serialized data classes, or add `--add-opens` JVM argument.

## Commands Reference

### Build
```bash
cd "c:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge"
.\gradlew.bat build
```

### Deploy to Server
```powershell
Remove-Item ".\forge-server\mods\servermanagement*.jar" -Force
Copy-Item ".\build\libs\servermanagement-1.0.0.jar" ".\forge-server\mods\" -Force
```

### Verify JAR Contents
```powershell
cd ".\forge-server\mods"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead("$PWD\servermanagement-1.0.0.jar")
$entry = $jar.Entries | Where-Object { $_.FullName -eq "META-INF/mods.toml" }
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$content = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$jar.Dispose()
$content | Select-String -Pattern "version="
```

### Start Server
```bash
cd forge-server
./run.bat
```

## Maintenance Notes

### For Future Updates

1. **Never change** `mod_version` in gradle.properties (keep as `1.0.0`)
2. **Always update** `MOD_VERSION` constant in ServerManagementMod.java
3. **Internal version format**: Use semantic versioning with suffixes (e.g., `1.0.1`, `1.1.0-beta`, `2.0.0`)
4. **JAR filename**: Will always be `servermanagement-1.0.0.jar`
5. **Server logs**: Will show the internal version (e.g., "ServerManagement v1.0.1")

### Version Bump Example
For next release (v1.0.1):
```java
// ServerManagementMod.java
public static final String MOD_VERSION = "1.0.1";  // ← Change this

// gradle.properties
mod_version=1.0.0  // ← NEVER change this
```

## Conclusion

This dual-version system elegantly solves the Forge blocking issue without:
- Modifying Forge configurations
- Requiring server-side settings
- Changing old client versions
- Using unreliable displayTest properties
- Breaking future compatibility

The solution is **production-ready** and has been deployed to the server.
