# BattleBox Plugin Architecture Guide

## Overview

This document explains the modular architecture of the BattleBox plugin and provides guidelines for adding new features while maintaining clean, maintainable code.

## Architecture Overview

The BattleBox plugin follows a **layered service architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────┐
│               Commands Layer            │
│  (BattleBoxCommand, ArenaCommand)       │
├─────────────────────────────────────────┤
│              Services Layer             │
│  (GameService, PlayerService,           │
│   KitService)                          │
├─────────────────────────────────────────┤
│              Managers Layer             │
│  (GameManager, ArenaManager,            │
│   TimerManager, etc.)                  │
├─────────────────────────────────────────┤
│              Listeners Layer            │
│  (Event handlers for game logic)       │
├─────────────────────────────────────────┤
│               Data Layer                │
│  (Game, ArenaConfig, Kit objects)      │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Main Plugin Class (`BattleBox.java`)
- **Purpose**: Plugin initialization and dependency injection
- **Responsibilities**: 
  - Initialize all services and managers
  - Register commands and listeners
  - Provide getter methods for services
- **Key Pattern**: Dependency injection container

### 2. Services Layer (`core/` package)

#### GameService
- **Purpose**: High-level game orchestration
- **Responsibilities**: Game lifecycle management, player coordination
- **Dependencies**: GameManager, ArenaManager, TimerManager, PlayerService

#### PlayerService
- **Purpose**: Player-specific operations
- **Responsibilities**: Kit management, teleportation, player state
- **Dependencies**: KitService, plugin instance

#### KitService
- **Purpose**: Kit creation and management
- **Responsibilities**: Item creation, kit validation, kit utilities
- **Dependencies**: None (pure utility service)

### 3. Managers Layer (`managers/` package)

#### GameManager
- **Purpose**: Low-level game state management
- **Responsibilities**: Game creation/deletion, player-game mapping
- **Pattern**: Repository pattern for game data

#### ArenaManager
- **Purpose**: Arena configuration management
- **Responsibilities**: Arena CRUD operations, configuration loading
- **Pattern**: Repository pattern for arena data

#### TimerManager
- **Purpose**: Timer and countdown management
- **Responsibilities**: Countdown timers, scoreboard updates
- **Dependencies**: ScoreboardManager

### 4. Commands Layer (`commands/` package)

#### BattleBoxCommand
- **Purpose**: Main game commands
- **Responsibilities**: Command parsing, validation, delegation to services
- **Pattern**: Command pattern with service delegation

#### ArenaCommand
- **Purpose**: Arena creation and management commands
- **Responsibilities**: Arena builder interface, configuration commands
- **Pattern**: Builder pattern for arena creation

### 5. Listeners Layer (`listeners/` package)
- **Purpose**: Event-driven game mechanics
- **Responsibilities**: Block placement/breaking, player interactions
- **Pattern**: Observer pattern via Bukkit event system

## Key Design Principles

### 1. Single Responsibility Principle
Each class has one clear purpose:
- Services handle business logic
- Managers handle data operations
- Commands handle user interface
- Listeners handle events

### 2. Dependency Injection
Dependencies are injected through constructors, making testing and maintenance easier:

```java
public class GameService {
    private final GameManager gameManager;
    private final ArenaManager arenaManager;
    private final TimerManager timerManager;
    private final PlayerService playerService;
    
    public GameService(GameManager gameManager, ArenaManager arenaManager, 
                      TimerManager timerManager, PlayerService playerService) {
        // Constructor injection
    }
}
```

### 3. Service Layer Pattern
Business logic is centralized in service classes, keeping commands thin:

```java
// Command delegates to service
private void handleCreate(Player player, String[] args) {
    String arenaName = args[1];
    if (gameService.createGame(player, arenaName)) {
        // Success handled by service
    }
}
```

### 4. Immutable Data Objects
Game state objects are designed to be immutable where possible:
- `ArenaConfig` - Configuration data
- `Kit` - Kit definitions
- `Location` - Position data

## Adding New Features

### 1. Adding a New Game Mode

**Step 1**: Extend the Game class
```java
// In Game.java
public enum GameMode {
    CLASSIC, BRIDGE, CAPTURE_THE_FLAG
}

private GameMode gameMode = GameMode.CLASSIC;
```

**Step 2**: Create a new service if needed
```java
// Create core/BridgeGameService.java
public class BridgeGameService {
    // Bridge-specific game logic
}
```

**Step 3**: Update GameService
```java
// In GameService.java
public boolean createBridgeGame(Player creator, String arenaId) {
    // Bridge game creation logic
}
```

**Step 4**: Add command handling
```java
// In BattleBoxCommand.java
case "bridge" -> handleCreateBridge(player, args);
```

### 2. Adding a New Kit Type

**Step 1**: Define the kit in KitService
```java
// In KitService.java
public ItemStack[] createArcherKit(Game.TeamColor team) {
    return new ItemStack[] {
        createBow(Enchantment.ARROW_INFINITE, 1),
        createArrows(64),
        createTeamArmor(team, Material.LEATHER_CHESTPLATE)
    };
}
```

**Step 2**: Register the kit
```java
// In KitService.java constructor or init method
availableKits.put("archer", this::createArcherKit);
```

**Step 3**: Update kit selection listener if needed
```java
// In PlayerInteractListener.java - add new button handling
```

### 3. Adding New Commands

**Step 1**: Add to command handler
```java
// In BattleBoxCommand.java
case "spectate" -> handleSpectate(player, args);

private void handleSpectate(Player player, String[] args) {
    // Delegate to appropriate service
    gameService.addSpectator(player, args[1]);
}
```

**Step 2**: Update tab completion
```java
// In BattleBoxCommand.java
if (args.length == 1) {
    return filterStartingWith(Arrays.asList(
        "create", "join", "leave", "list", "info", "spectate"), args[0]);
}
```

**Step 3**: Add service method
```java
// In GameService.java
public boolean addSpectator(Player player, String gameId) {
    // Spectator logic
}
```

### 4. Adding New Arena Features

**Step 1**: Extend ArenaConfig
```java
// In ArenaConfig.java
public static class PowerUps {
    public Location speedBoost;
    public Location jumpBoost;
    public int respawnTime;
}

public PowerUps powerUps;
```

**Step 2**: Update ArenaCommand
```java
// In ArenaCommand.java
case "setpowerup" -> handleSetPowerUp(player, args);
```

**Step 3**: Add validation
```java
// In GameService.java
private boolean isArenaComplete(ArenaConfig arena) {
    return /* existing checks */ && 
           arena.powerUps != null && 
           arena.powerUps.speedBoost != null;
}
```

## Best Practices for Contributors

### 1. Code Organization
- Keep related functionality in the same package
- Use descriptive class and method names
- Limit class size (aim for under 300 lines)
- Extract complex logic into separate methods

### 2. Error Handling
```java
// Always validate inputs
if (arena == null) {
    player.sendMessage(ChatColor.RED + "Arena not found!");
    return false;
}

// Provide helpful error messages
if (!isArenaComplete(arena)) {
    player.sendMessage(ChatColor.RED + "Arena is incomplete. Use /arena info " + 
                      arenaId + " to see what's missing.");
    return false;
}
```

### 3. Service Integration
```java
// Services should not depend on commands or listeners
// Commands and listeners should delegate to services
// Services can depend on managers and other services

// GOOD:
public class GameService {
    private final GameManager gameManager; // Manager dependency
    private final PlayerService playerService; // Service dependency
}

// BAD:
public class GameService {
    private final BattleBoxCommand command; // Command dependency - avoid this
}
```

### 4. Event Handling
```java
// Keep listeners focused and delegate to services
@EventHandler
public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Game game = gameManager.getPlayerGame(player);
    
    if (game != null) {
        // Delegate complex logic to service
        gameService.handleBlockPlace(player, event.getBlock(), game);
    }
}
```

### 5. Configuration Management
- Use ArenaConfig for arena-specific settings
- Add validation for new configuration fields
- Provide default values where appropriate
- Update the arena creation workflow for new fields

### 6. Testing Considerations
- Services should be easily testable (no static dependencies)
- Use dependency injection for easier mocking
- Keep business logic separate from Bukkit API calls where possible

## File Structure Guidelines

```
src/main/java/plugins/battlebox/
├── BattleBox.java                 # Main plugin class
├── commands/                      # Command handlers
│   ├── BattleBoxCommand.java     # Main game commands
│   └── ArenaCommand.java         # Arena management
├── core/                         # Business logic services
│   ├── GameService.java          # Game orchestration
│   ├── PlayerService.java        # Player operations
│   └── KitService.java           # Kit management
├── managers/                     # Data management
│   ├── GameManager.java          # Game state management
│   ├── ArenaManager.java         # Arena data management
│   └── TimerManager.java         # Timer operations
├── game/                         # Game state objects
│   ├── Game.java                 # Game state
│   └── GameState.java            # Game state enum
├── listeners/                    # Event handlers
│   ├── BlockPlaceListener.java   # Block placement events
│   ├── PlayerInteractListener.java # Player interaction events
│   └── ...
└── arena/                        # Arena-related classes
    ├── ArenaInstance.java        # Runtime arena instance
    └── ArenaTemplate.java        # Arena template
```

## Migration Guide for Legacy Code

When working with existing code that doesn't follow these patterns:

1. **Identify the responsibility** of the code
2. **Extract business logic** into appropriate services
3. **Keep the original class** for compatibility
4. **Delegate to new services** from the original class
5. **Update tests** to use the new structure

This approach maintains backward compatibility while improving code organization.

## Conclusion

This modular architecture provides:
- **Maintaiability**: Easy to understand and modify
- **Testability**: Services can be tested independently
- **Extensibility**: New features fit into existing patterns
- **Separation of Concerns**: Each component has a clear purpose

When adding new features, always consider which layer they belong to and follow the established patterns for consistency and maintainability.
