# ServerManagement Forge Port - Migration Guide

## Overview
This document outlines the complete Fabric to Forge 1.20.1 port strategy for the ServerManagement mod.

## Fabric → Forge API Translations

### 1. Main Mod Class
**Fabric**: Uses `ModInitializer` and `ClientModInitializer`
```java
public class ServerManagementMod implements ModInitializer
```

**Forge**: Uses `@Mod` annotation
```java
@Mod(ServerManagementMod.MOD_ID)
public class ServerManagementMod
```

### 2. Networking System
**Fabric**: Uses `ClientPlayNetworking` and `ServerPlayNetworking` with Identifiers
```java
ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, (server, player, handler, buf, responseSender) -> {...});
```

**Forge**: Uses `SimpleChannel` with indexed messages
```java
INSTANCE.messageBuilder(PacketClass.class, id())
    .encoder(PacketClass::encode)
    .decoder(PacketClass::new)
    .consumerMainThread(PacketClass::handle)
    .add();
```

### 3. Screen Handlers/Menus
**Fabric**: `ScreenHandler` and `HandledScreen`
```java
public class WorldListScreenHandler extends ScreenHandler
public class WorldListScreen extends HandledScreen<WorldListScreenHandler>
```

**Forge**: `AbstractContainerMenu` and `AbstractContainerScreen`
```java
public class WorldListMenu extends AbstractContainerMenu
public class WorldListScreen extends AbstractContainerScreen<WorldListMenu>
```

### 4. Events
**Fabric**: Callback-based events
```java
ServerLifecycleEvents.SERVER_STARTED.register(server -> {...});
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {...});
```

**Forge**: EventBus with `@SubscribeEvent`
```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {...}

@SubscribeEvent
public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {...}
```

### 5. Commands
**Fabric**: `CommandRegistrationCallback`
```java
CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {...});
```

**Forge**: `RegisterCommandsEvent`
```java
@SubscribeEvent
public void onRegisterCommands(RegisterCommandsEvent event) {...}
```

### 6. Mixins
**Fabric**: Mixin system integrated
```java
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {...}
```

**Forge**: Use events or Access Transformers instead
- Portal blocking: Use `PlayerInteractEvent` or `LivingChangeTargetEvent`
- No direct injection needed in most cases

### 7. Configuration
**Fabric**: Custom config or Cloth Config
```java
// JSON-based config
```

**Forge**: ForgeConfigSpec
```java
public static final ForgeConfigSpec SPEC;
public static final BooleanValue FEATURE_ENABLED;
```

### 8. Registration
**Fabric**: Direct registry access
```java
Registry.register(Registries.BLOCK, identifier, block);
```

**Forge**: DeferredRegister
```java
public static final DeferredRegister<Block> BLOCKS = 
    DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
public static final RegistryObject<Block> MY_BLOCK = 
    BLOCKS.register("my_block", () -> new Block(...));
```

## File Structure

### Core Files (Priority 1)
1. ✅ `ServerManagementMod.java` - Main mod class
2. ✅ `ModConfig.java` - Forge config system
3. ✅ `ModNetworking.java` - Networking infrastructure
4. ✅ `FeatureRegistry.java` - Feature management

### Network Packets (Priority 2)
Need to create packet classes for:
- Config sync (5 packets)
- WorldManager operations (15 packets)
- PlayerManager operations (8 packets)
- World list/detail (4 packets)

Each packet needs:
- Constructor for decoding
- `encode(FriendlyByteBuf)` method
- `handle(Supplier<NetworkEvent.Context>)` method

### Features (Priority 3)
1. `Feature.java` interface
2. `FeatureManager.java` - Feature lifecycle
3. `WorldManagerFeature.java` - Main WM feature
4. `PlayerManagerFeature.java` - Main PM feature
5. `SlimeHeadFeature.java` - Slime heads

### World Manager (Priority 4)
1. `WorldManager.java` - Core logic
2. `WorldManagerData.java` - JSON persistence
3. `WorldManagerEvents.java` - Forge event handlers
4. `PortalTimerManager.java` - Timer system

### Player Manager (Priority 5)
1. `PlayerManagerSingleton.java` - Core logic
2. `PlayerManagerEvents.java` - Forge event handlers
3. `PlayerInventoryView.java` - Inventory viewing

### GUI System (Priority 6)
**Menu Types (Server-side)**:
1. `ModMenuTypes.java` - Register all menus
2. `ConfigMenu.java`
3. `GlobalSettingsMenu.java`
4. `WorldListMenu.java`
5. `WorldDetailMenu.java`
6. `PlayerManagerMenu.java`
7. `PortalTimerMenu.java`

**Screens (Client-side)**:
1. `ModScreens.java` - Register screen factories
2. `ConfigScreen.java`
3. `ConfigGuiScreen.java`
4. `GlobalSettingsScreen.java`
5. `WorldListScreen.java`
6. `WorldDetailScreen.java`
7. `PlayerManagerScreen.java`
8. `PlayerManagerGuiScreen.java`
9. `PortalTimerScreen.java`

### Commands (Priority 7)
Register in `ModCommands.java`:
- `/worldmanager` (/wm) - WorldManager GUI
- `/playermanager` (/pm) - PlayerManager GUI
- `/spectate` - Spectate player
- `/viewinv` - View inventory
- `/netherportals` - Toggle Nether portals
- `/endportals` - Toggle End portals
- `/setlobby` - Set lobby spawn
- `/clearlobby` - Clear lobby spawn
- `/teleportlobby` - Teleport to lobby
- `/timer` - Portal timer configuration

## Implementation Steps

### Step 1: Complete Network System
Create all packet classes with proper encoding/decoding.

### Step 2: Implement Features
Port WorldManager and PlayerManager logic to Forge events.

### Step 3: Create Menu System
Implement all AbstractContainerMenu classes for GUI synchronization.

### Step 4: Create Screens
Implement client-side GUI rendering.

### Step 5: Command Registration
Register all commands in Forge command event.

### Step 6: Event Handlers
Replace Fabric callbacks and mixins with Forge @SubscribeEvent methods.

### Step 7: Testing
- Test GUI opening/closing
- Test networking synchronization
- Test commands
- Test portal blocking
- Test timers
- Test chat isolation
- Test spectate/inventory viewing

## Known Challenges

1. **Portal Blocking**: Fabric uses mixin, Forge should use `PlayerInteractEvent.RightClickBlock`
2. **Chat Isolation**: Fabric uses `ServerMessageEvents`, Forge uses `ServerChatEvent`
3. **Tab List**: Fabric custom logic, Forge needs packet manipulation
4. **GUI Registration**: Different registration timing (mod bus vs game bus)
5. **Packet IDs**: Fabric uses Identifiers, Forge uses integer IDs

## Dependencies

- Minecraft: 1.20.1
- Forge: 47.4.0
- Java: 17
- Mappings: official

## Build Commands

```powershell
cd "c:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge"
.\gradlew build
```

## Testing Environment

Run configurations defined in `build.gradle`:
- `runClient` - Client testing
- `runServer` - Server testing
- `runData` - Data generation

## Notes

- All Fabric-specific APIs must be replaced
- Mixin system not available (use Forge events)
- Different lifecycle events
- Different networking protocol
- Different GUI synchronization
- Configuration system completely different

## Progress Tracker

- [x] Project structure
- [x] build.gradle
- [x] gradle.properties
- [x] settings.gradle
- [x] mods.toml
- [x] Main mod class
- [x] Config system foundation
- [x] Network infrastructure
- [ ] Packet implementations (0/32)
- [ ] Feature implementations (0/5)
- [ ] Menu implementations (0/6)
- [ ] Screen implementations (0/9)
- [ ] Command implementations (0/12)
- [ ] Event handlers (0/10)
- [ ] Data persistence (0/2)
- [ ] Build and test

## Estimated Work

- **Files to create**: ~45
- **Lines of code**: ~5,000-6,000
- **API translations**: ~200+
- **Testing time**: 2-4 hours

This is a substantial port requiring systematic conversion of all Fabric APIs to Forge equivalents.
