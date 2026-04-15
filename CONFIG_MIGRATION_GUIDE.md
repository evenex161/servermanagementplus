# Config Versioning & Auto-Migration System

## Overview
The ServerManagement mod now includes a robust config versioning and auto-migration system that automatically updates old config files to new formats when the mod is updated.

---

## How It Works

### Config Version Tracking
Every config file now includes a `configVersion` field in the `[Meta]` section:

```toml
[Meta]
    # Config version for auto-migration. DO NOT EDIT MANUALLY!
    configVersion = 1
```

This version is automatically managed by the mod and should **never be edited manually**.

---

## Current Version: 1

The current config version is **1** (defined in `ModConfig.CURRENT_CONFIG_VERSION`).

---

## Automatic Migration Process

### On Startup:
1. **Version Check**: Mod reads the config version
2. **Migration Detection**: Compares with expected version
3. **Backup Creation**: Creates timestamped backup before migration
4. **Sequential Migration**: Applies all migrations in order (v0→v1, v1→v2, etc.)
5. **Version Update**: Updates config version to current
6. **Validation**: Validates the migrated config

### Migration Scenarios:

#### New Config (No Migration)
```
[INFO] Config file does not exist, no migration needed
[INFO] Configuration validated successfully
```

#### Old Config (Migration Required)
```
[INFO] Config version check: Current=0, Target=1
[INFO] === CONFIG MIGRATION REQUIRED ===
[INFO] Migrating config from version 0 to 1
[INFO] Created migration backup: servermanagement-common.toml.pre-migration_v0_20250109_150530
[INFO] Applying migration 0->1: Initial config version - adding version tracking
[INFO] Migration 0->1 completed successfully
[INFO] === MIGRATION SUCCESSFUL ===
[INFO] Config updated from version 0 to 1
```

#### Future Mod Version (Downgrade Warning)
```
[WARN] Config version 3 is newer than expected 1! You may have downgraded the mod.
[WARN] Attempting to use config as-is, but issues may occur.
```

---

## Admin Commands

### `/smconfig info`
Shows current config version and migration status:
```
=== Config Information ===
Current Version: 1
Expected Version: 1
Config is up to date!
```

### `/smconfig migrate`
Manually trigger config migration:
```
Starting config migration...
Migration completed successfully!
A backup of your old config was created.
```

### `/smconfig validate`
Validate and repair config if corrupted

### `/smconfig reset confirm`
Reset config to defaults (creates backup)

### `/smconfig backup`
Cleanup old backup files

---

## For Mod Developers: Adding New Migrations

When adding new config options or changing config structure in future versions:

### Step 1: Update Config Version
In `ModConfig.java`:
```java
public static final int CURRENT_CONFIG_VERSION = 2; // Increment version
```

### Step 2: Add New Config Fields
```java
public static final BooleanValue NEW_FEATURE_ENABLED;

static {
    // ... existing config ...
    
    NEW_FEATURE_ENABLED = BUILDER
        .comment("Enable new feature")
        .define("newFeatureEnabled", true);
}
```

### Step 3: Create Migration
In `ConfigMigration.java`, add to the `MIGRATIONS` map:

```java
static {
    // Existing migrations...
    
    // Migration from version 1 to version 2
    MIGRATIONS.put(1, new Migration() {
        @Override
        public void apply() throws Exception {
            ServerManagementMod.LOGGER.info("Applying migration 1->2: Adding new feature settings");
            
            // Example: Read old value if you're renaming/moving a setting
            // String oldValue = readOldConfigValue(configFile, "OldSection", "oldKey");
            
            // Example: Set default for new config option
            // ModConfig.NEW_FEATURE_ENABLED.set(true);
            
            // Always update version and save
            ModConfig.CONFIG_VERSION.set(2);
            ModConfig.SPEC.save();
        }
        
        @Override
        public String getDescription() {
            return "Adding support for new feature with additional config options";
        }
    });
}
```

### Step 4: Test Migration
1. Create a test config with version 1
2. Launch mod with version 2 code
3. Verify migration runs and backup is created
4. Verify config values are preserved/migrated correctly

---

## Migration Examples

### Example 1: Adding New Feature (Simple)
```java
MIGRATIONS.put(1, new Migration() {
    @Override
    public void apply() throws Exception {
        ServerManagementMod.LOGGER.info("Applying migration 1->2: Adding teleportation feature");
        // New feature with defaults - no action needed, ForgeConfigSpec handles it
        ModConfig.CONFIG_VERSION.set(2);
        ModConfig.SPEC.save();
    }
    
    @Override
    public String getDescription() {
        return "Adding teleportation feature configuration";
    }
});
```

### Example 2: Renaming Config Key (Complex)
```java
MIGRATIONS.put(2, new Migration() {
    @Override
    public void apply() throws Exception {
        ServerManagementMod.LOGGER.info("Applying migration 2->3: Renaming portal settings");
        
        // Read old values before they're lost
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("servermanagement-common.toml");
        String content = Files.readString(configPath);
        
        // Extract old value (example - adjust parsing as needed)
        boolean oldPortalsSetting = content.contains("enablePortals = true");
        
        // Set new config option with old value
        ModConfig.PORTALS_ENABLED.set(oldPortalsSetting);
        
        // Update version
        ModConfig.CONFIG_VERSION.set(3);
        ModConfig.SPEC.save();
        
        ServerManagementMod.LOGGER.info("Migrated portal setting: {}", oldPortalsSetting);
    }
    
    @Override
    public String getDescription() {
        return "Renaming 'enablePortals' to 'portalsEnabled' for consistency";
    }
});
```

### Example 3: Removing Deprecated Feature
```java
MIGRATIONS.put(3, new Migration() {
    @Override
    public void apply() throws Exception {
        ServerManagementMod.LOGGER.info("Applying migration 3->4: Removing deprecated feature");
        
        // If a feature is being removed, maybe save the old value to a backup
        // or migrate it to a new equivalent feature
        
        ModConfig.CONFIG_VERSION.set(4);
        ModConfig.SPEC.save();
        
        ServerManagementMod.LOGGER.warn("Deprecated feature removed. Settings have been reset.");
    }
    
    @Override
    public String getDescription() {
        return "Removing deprecated experimental feature";
    }
});
```

---

## Backup System

### Automatic Backups Created:
1. **Migration Backups**: `servermanagement-common.toml.pre-migration_v<version>_<timestamp>`
2. **Corruption Backups**: `servermanagement-common.toml.backup_<timestamp>`
3. **Manual Reset Backups**: `servermanagement-common.toml.backup_<timestamp>`

### Backup Retention:
- Keeps last 5 backups automatically
- Older backups auto-deleted via `/smconfig backup` or on startup

---

## Error Handling

### Migration Fails
- Original config is preserved (backup created first)
- Error logged with details
- Mod may fall back to defaults or refuse to load

### Corrupted Config During Migration
- Migration aborted
- Config validation and repair system takes over
- Fresh config created with defaults

### Version Downgrade Detected
- Warning logged
- Config used as-is
- Some features may not work correctly

---

## Best Practices

### For Users:
✅ **Never edit** the `configVersion` field manually
✅ Keep backups before major mod updates
✅ Check server logs after updating
✅ Use `/smconfig info` to check migration status

### For Developers:
✅ **Always increment** `CURRENT_CONFIG_VERSION` when changing config structure
✅ **Always add migration** for the previous version
✅ **Test migrations** with real config files from previous versions
✅ **Document changes** in migration description
✅ **Preserve user settings** when possible during migration
✅ **Create backups** before any destructive changes

---

## Troubleshooting

### "Config migration failed"
1. Check server logs for specific error
2. Look for backup file created before migration
3. Try `/smconfig reset confirm` to start fresh
4. Report issue with logs to mod developer

### "Config version is newer than expected"
- You downgraded the mod
- Config is from a future version
- Either upgrade mod or reset config

### "No migration defined for version X"
- Missing migration implementation
- Update mod to latest version
- Report to mod developer if persists

---

## Version History

### Version 1 (Current)
- Initial config versioning implementation
- Basic config structure with World Manager and Player Manager
- Packet timestamping system
- Config validation and auto-repair

### Version 0 (Legacy)
- Pre-versioning configs (before this system was added)
- Automatically migrated to v1 on first load

---

## Future Migrations (Template)

When creating new versions, update this section:

### Version 2 (Planned)
- [Description of changes]
- [New features added]
- [Migration strategy]

### Version 3 (Planned)
- [Description of changes]
- [New features added]
- [Migration strategy]
