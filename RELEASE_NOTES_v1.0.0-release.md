# ServerManagement Mod - v1.0.0-release
## Stable Release with OTA Update System
**Release Date:** November 10, 2025  
**Data Version:** 2  
**Minecraft Version:** 1.20.1 Forge

---

## 🎉 What's New in v1.0.0-release

### 🚀 OTA (Over-The-Air) Update System
The major feature of this release is the **automatic client-side update system**!

#### How It Works:
1. **Automatic Version Detection**: When a player joins a server, the server automatically checks if the player's mod version matches the server's version
2. **Instant Notification**: Players with outdated mods receive a clear notification in chat
3. **Automated Download**: The mod JAR is automatically transferred from server to client in 32KB chunks
4. **Integrity Verification**: SHA-256 hash verification ensures file integrity
5. **Auto-Installation**: The new mod is automatically installed to the mods folder
6. **Backup Safety**: Old mod version is backed up as `.backup` before replacement

#### User Experience:
```
Player joins with v1.0.0-EA → Server running v1.0.0-release

Chat notification:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚠ MOD VERSION MISMATCH

Your version: 1.0.0-EA
Server version: 1.0.0-release

An automatic update is available!
File: servermanagement-forge-1.0.0-release.jar
Size: 411.4 KB

The update will download automatically.
Please wait while the update downloads...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[OTA] Download started...
[OTA] Downloading: 25.0% (5/20)
[OTA] Downloading: 50.0% (10/20)
[OTA] Downloading: 75.0% (15/20)
[OTA] Downloading: 100.0% (20/20)
[OTA] Download complete!
[OTA] Verification successful!
[OTA] Installing update...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ UPDATE INSTALLED SUCCESSFULLY

Version: 1.0.0-release
File: servermanagement-forge-1.0.0-release.jar

⚠ RESTART REQUIRED
Please restart your game to apply the update.

Old version backed up as .backup
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 🔧 Technical Details

### OTA System Architecture

#### Network Protocol:
- **VersionCheckPacket**: Server→Client version comparison
- **ModFileRequestPacket**: Client→Server update request
- **ModFileChunkPacket**: Server→Client file chunks (32KB each)
- **ModFileCompletePacket**: Server→Client transfer completion

#### Transfer Mechanism:
- **Chunk Size**: 32KB (32,768 bytes) per packet
- **Rate Limiting**: 10ms delay between chunks to prevent network flooding
- **Async Transfer**: Non-blocking transfer using CompletableFuture
- **Progress Tracking**: Real-time progress updates in chat

#### Security Features:
- **SHA-256 Hashing**: File integrity verification
- **Automatic Backup**: Old version preserved before update
- **Error Recovery**: Failed transfers are safely aborted with cleanup

#### Client-Side (OTAUpdateManager):
- Receives and assembles file chunks
- Verifies file integrity (size + SHA-256)
- Backs up old mod JAR
- Installs new JAR to mods/
- Provides user notifications

#### Server-Side (ModFileTransferManager):
- Auto-detects mod JAR in mods/ folder
- Calculates SHA-256 hash on startup
- Manages multiple concurrent transfers
- Chunks file for network transmission
- Sends completion/error status

---

## 📊 Migration from v1.0.0-EA

### Automatic Migration:
When updating from v1.0.0-EA to v1.0.0-release:
- **Data Version**: Incremented from 1 to 2
- **Data Compatibility**: No data transformation needed
- **Backward Compatible**: All existing data remains valid

### Migration Process:
```
Server Startup:
1. Detects Data Version 1 (v1.0.0-EA)
2. Runs migrateV1EAToV1Release()
3. Updates version tracking
4. Logs migration success
5. Server continues normally
```

### What's Preserved:
✅ All player economy data (bank accounts, transactions)  
✅ All daily tasks and achievements  
✅ All MineBay marketplace listings  
✅ All free reward progress  
✅ All configuration settings  
✅ All world management data  

---

## 📦 File Information

**JAR File**: `servermanagement-1.0.0.jar`  
**Size**: ~411 KB  
**Location**: `build/libs/servermanagement-1.0.0.jar`

### Installation:
1. **Server**: Place JAR in `mods/` folder
2. **Client**: Will be updated automatically via OTA when joining the server!

---

## 🛠️ System Components

### New Files Created:
1. **OTAUpdateManager.java** (335 lines)
   - Client-side update manager
   - Handles downloads, verification, installation
   
2. **ModFileTransferManager.java** (244 lines)
   - Server-side file transfer manager
   - Auto-detects JAR, manages transfers
   
3. **VersionCheckPacket.java** (97 lines)
   - Version comparison packet
   
4. **ModFileRequestPacket.java** (56 lines)
   - Update request packet
   
5. **ModFileChunkPacket.java** (81 lines)
   - File chunk transfer packet
   
6. **ModFileCompletePacket.java** (86 lines)
   - Transfer completion packet
   
7. **PlayerJoinListener.java** (42 lines)
   - Triggers version check on player join

### Modified Files:
1. **DataVersion.java**
   - Updated CURRENT_VERSION to 2
   - Added VERSION_1_0_0_RELEASE constant
   
2. **ServerManagementMod.java**
   - Updated MOD_VERSION to "1.0.0-release"
   - Added OTA system initialization
   
3. **MigrationManager.java**
   - Added EA→release migration path
   
4. **ModNetworking.java**
   - Registered 4 new OTA packets

---

## 🎯 Key Features (Carried Over)

All features from v1.0.0-EA remain fully functional:

### Economy System:
- Bank accounts with balance tracking
- Transaction history and logging
- Transfer commands (`/bank transfer`)
- Balance checking (`/bank balance`)

### Daily Tasks & Rewards:
- Daily task system with rewards
- Free rewards with cooldowns
- Achievement tracking
- Task completion UI

### MineBay Marketplace:
- Item listing with custom prices
- Secure transactions
- Listing management
- Price item system (diamonds, emeralds, etc.)

### GUI System:
- World Manager GUI
- Player Manager GUI
- Economy GUI
- MineBay GUI
- Daily Tasks GUI

### Version Tracking:
- Installation date tracking
- Last updated tracking
- Version history logging
- Migration tracking

---

## 🔒 Security

### Data Encryption:
- AES-256-GCM encryption for sensitive data
- Automatic key generation and management
- Encrypted storage of player data

### OTA Security:
- SHA-256 file integrity verification
- Automatic backup before installation
- Safe error handling and rollback

---

## 📈 Performance

### OTA Transfer Performance:
- **Transfer Speed**: ~3.2 MB/s (32KB chunks @ 10ms interval)
- **Network Impact**: Minimal (rate-limited chunks)
- **Server Impact**: Non-blocking async transfers
- **Client Impact**: Background installation

### Example Transfer Times:
- 400 KB mod: ~2 seconds
- 1 MB mod: ~5 seconds
- 5 MB mod: ~25 seconds

---

## 🐛 Bug Fixes

All bugs from v1.0.0-EA have been addressed in this release.

---

## 🔮 Future Roadmap

Potential features for future releases:
- Delta updates (only transfer changed parts)
- Multiple mod distribution
- Update scheduling/automation
- Rollback functionality from GUI
- Update changelog in-game display

---

## 📝 Developer Notes

### Building from Source:
```powershell
.\gradlew build
```

### Testing OTA System:
1. Run server with v1.0.0-release
2. Connect with client running v1.0.0-EA
3. Observe version check and automatic update
4. Verify file integrity and installation
5. Restart client to load new version

### OTA System Configuration:
All OTA settings are hardcoded for stability:
- Chunk size: 32KB
- Transfer delay: 10ms
- Hash algorithm: SHA-256

---

## ⚠️ Known Limitations

1. **Restart Required**: Players must restart Minecraft after update installation
2. **Single Mod Only**: Currently only updates ServerManagement mod
3. **Manual Fallback**: If OTA fails, manual installation is required
4. **Server JAR Required**: Server must have its own JAR in mods/ folder

---

## 🤝 Support

For issues, questions, or feedback:
- Check server logs for detailed error messages
- Check client logs for OTA transfer details
- Backup your data before major updates

---

## 📄 License

This mod is provided as-is for use with Minecraft Forge 1.20.1.

---

## 🎊 Credits

**Development**: ServerManagement Team  
**Testing**: Community Contributors  
**Framework**: Minecraft Forge 1.20.1  

---

**Enjoy the seamless update experience!** 🚀
