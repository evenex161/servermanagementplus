# CurseForge Integration for OTA System

## Overview
The OTA system now includes **CurseForge update checking** to automatically detect when new versions are available on your mod page.

## Implementation Details

### Files Created
- **`CurseForgeUpdateChecker.java`**: Main class for checking CurseForge API
- **Updated `PlayerJoinListener.java`**: Triggers async CurseForge check on player join

### How It Works

1. **Async Checking**: When a player joins the server, the system asynchronously checks CurseForge for updates
2. **Version Comparison**: Compares current OTA version with latest file on CurseForge
3. **Logging**: Logs update availability to server console with download link

### Current Status

⚠️ **Placeholder Implementation**: The CurseForge API integration is currently a **framework/placeholder** because:

1. **API Key Required**: CurseForge API requires an API key for authentication
2. **Project ID Needed**: Your project ID must be extracted from the API
3. **Rate Limiting**: Need to implement proper rate limiting to avoid API abuse

### Your CurseForge Mod Page
🔗 **https://www.curseforge.com/minecraft/mc-mods/servermanagement**

## How to Complete the Integration

### Step 1: Get CurseForge API Key
1. Visit [CurseForge for Studios](https://console.curseforge.com/)
2. Sign in with your CurseForge account
3. Navigate to API Keys section
4. Generate a new API key for your project

### Step 2: Find Your Project ID
Option A: From URL
- Your mod page: `https://www.curseforge.com/minecraft/mc-mods/servermanagement`
- Use CurseForge API to search by slug: `servermanagement`

Option B: From API
```java
GET https://api.curseforge.com/v1/mods/search?gameId=432&slug=servermanagement
Header: x-api-key: YOUR_API_KEY
```

### Step 3: Update the Code

In `CurseForgeUpdateChecker.java`, update these constants:

```java
// Replace with your actual project ID
private static final int PROJECT_ID = YOUR_PROJECT_ID;

// Replace with your API key (or load from config)
private static final String API_KEY = "YOUR_API_KEY_HERE";
```

### Step 4: Uncomment API Implementation

In `getLatestFile()` method, uncomment the API implementation code:

```java
private static CurseForgeFile getLatestFile() {
    try {
        String apiUrl = String.format(CURSEFORGE_API, PROJECT_ID);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-api-key", API_KEY); // Add your API key
        
        // ... rest of the code
    }
}
```

## API Documentation

### CurseForge API v1
- **Base URL**: `https://api.curseforge.com/v1`
- **Docs**: https://docs.curseforge.com/
- **Rate Limits**: Typically 100-500 requests per hour

### Key Endpoints Used
1. **Get Mod Files**: `GET /mods/{modId}/files`
   - Returns list of all files for the mod
   - Can filter by Minecraft version

2. **Get Specific File**: `GET /mods/{modId}/files/{fileId}`
   - Returns details about a specific file

### Example Response
```json
{
  "data": [
    {
      "id": 12345678,
      "fileName": "servermanagement-1.0.0.3.jar",
      "displayName": "Server Management v1.0.0 Build 3",
      "downloadUrl": "https://edge.forgecdn.net/files/...",
      "fileLength": 524288,
      "gameVersions": ["1.20.1", "Forge"]
    }
  ]
}
```

## Features

### Automatic Version Detection
- Parses version from filename: `servermanagement-1.0.0.3.jar` → `1.0.0.3`
- Falls back to display name if filename parsing fails
- Uses `OTAVersion` class for intelligent comparison

### Server Console Logging
When an update is available, server logs show:
```
============================================================
NEW VERSION AVAILABLE ON CURSEFORGE!
Current: 1.0.0.2
Latest: 1.0.0.3
Download: https://edge.forgecdn.net/files/xxxx/servermanagement-1.0.0.3.jar
============================================================
```

### Non-Blocking
- Runs asynchronously using `CompletableFuture`
- Doesn't delay player joining
- Fails gracefully if API is unavailable

## Security Considerations

### API Key Storage
**DO NOT** hardcode API keys in source code. Instead:

1. **Environment Variable**:
```java
private static final String API_KEY = System.getenv("CURSEFORGE_API_KEY");
```

2. **Config File**:
```java
// In ModConfig.java
public static final ForgeConfigSpec.ConfigValue<String> CURSEFORGE_API_KEY;
```

3. **External Properties**:
```java
Properties props = new Properties();
props.load(new FileInputStream("curseforge.properties"));
String apiKey = props.getProperty("api.key");
```

### Rate Limiting
Implement caching to avoid excessive API calls:
```java
private static long lastCheck = 0;
private static final long CHECK_INTERVAL = 3600000; // 1 hour

if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL) {
    return cachedResult;
}
```

## Testing

### Test Without API Key
Current implementation logs a warning:
```
WARN: CurseForge API integration requires API key - not implemented yet
INFO: CurseForge page: https://www.curseforge.com/minecraft/mc-mods/servermanagement
```

### Test With API Key
1. Add your API key
2. Join the server
3. Check server console for update messages
4. Verify version comparison works correctly

## Future Enhancements

### Automatic Downloads
- Download latest JAR from CurseForge
- Replace current mod file
- Trigger server restart

### Update Notifications
- Send chat messages to admins
- Create update reminder GUI
- Discord webhook integration

### Version Channels
- Support for alpha/beta/release channels
- Filter by release type
- Allow server owners to choose update stability level

## Troubleshooting

### No Updates Detected
1. Check API key is valid
2. Verify project ID is correct
3. Check server console for error messages
4. Ensure CurseForge has files for Minecraft 1.20.1

### API Errors
- **403 Forbidden**: Invalid or missing API key
- **404 Not Found**: Incorrect project ID
- **429 Too Many Requests**: Rate limit exceeded

### Version Parsing Issues
- Ensure filenames follow format: `modname-version.jar`
- Check display names contain version numbers
- Add custom parsing logic if needed

## Conclusion

The CurseForge integration framework is now in place! To complete it:
1. Get your API key from CurseForge
2. Find your project ID
3. Update the constants in the code
4. Test the integration
5. Enjoy automatic update checking! 🎉

---

**Note**: This integration is completely optional. The OTA system works independently using build numbers. CurseForge checking is an additional convenience feature.
