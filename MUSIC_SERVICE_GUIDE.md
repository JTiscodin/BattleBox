# MusicService Usage Guide

## Overview

The `MusicService` is a comprehensive audio management system for the BattleBox plugin that handles background music, sound effects, and player audio preferences during gameplay. It follows the established service architecture pattern and integrates seamlessly with the game lifecycle.

## Architecture Integration

The MusicService fits into the BattleBox architecture as follows:

```
┌─────────────────────────────────────────┐
│               Commands Layer            │ ← Can control music preferences
├─────────────────────────────────────────┤
│              Services Layer             │ ← MusicService lives here
│  (GameService, PlayerService,           │
│   KitService, MusicService)            │
├─────────────────────────────────────────┤
│              Managers Layer             │ ← Services use managers
├─────────────────────────────────────────┤
│              Listeners Layer            │ ← Can trigger music events
├─────────────────────────────────────────┤
│               Data Layer                │ ← Stores preferences & state
└─────────────────────────────────────────┘
```

## Core Features

### 1. **Dynamic Background Music**

- Automatically plays different music based on game state
- Seamless transitions between game phases
- Looped playback for ambient music

### 2. **Event-Driven Sound Effects**

- Player actions trigger appropriate sounds
- Team-based audio feedback
- Countdown and timer sounds

### 3. **Player Preferences**

- Individual music and sound effect controls
- Volume adjustment per player
- Persistent preference storage

### 4. **Game State Integration**

- Automatic music changes with game progression
- Team-specific audio cues
- Victory and defeat audio

## Music Events

### Background Music (Looped)

| Game State    | Music Event        | Duration | Description                |
| ------------- | ------------------ | -------- | -------------------------- |
| `WAITING`     | `GAME_WAITING`     | 11s      | Calm waiting room music    |
| `STARTING`    | `GAME_STARTING`    | 7.5s     | Building tension countdown |
| `IN_PROGRESS` | `GAME_IN_PROGRESS` | 15s      | Energetic battle music     |
| `ENDING`      | `GAME_ENDING`      | 9.75s    | Victory/conclusion music   |

### Sound Effects (One-time)

| Event             | Trigger            | Description                  |
| ----------------- | ------------------ | ---------------------------- |
| `PLAYER_JOIN`     | Player joins game  | Welcoming sound              |
| `PLAYER_LEAVE`    | Player leaves game | Door closing sound           |
| `WOOL_PLACED`     | Player places wool | Block placement confirmation |
| `COUNTDOWN_TICK`  | Countdown timer    | Regular tick sound           |
| `COUNTDOWN_FINAL` | Final 3 seconds    | Urgent bell sound            |
| `TEAM_WIN`        | Team victory       | Dramatic victory sound       |
| `GAME_DRAW`       | Game ends in draw  | Neutral ending sound         |

## Usage Examples

### Basic Integration

#### In GameService

```java
public class GameService {
    private final MusicService musicService;

    public GameService(/* other dependencies */, MusicService musicService) {
        this.musicService = musicService;
    }

    public boolean createGame(Player creator, String arenaId) {
        // Create game logic...
        Game game = new Game(gameId, arenaId);

        // Start music for the new game
        musicService.startGameMusic(game);

        return true;
    }

    public void startGame(Game game) {
        game.setState(GameState.STARTING);

        // Update music for state change
        musicService.updateGameMusic(game);

        // Start countdown with music
        startCountdown(game);
    }

    private void startCountdown(Game game) {
        // Countdown logic with music events
        for (int i = 10; i > 0; i--) {
            final int seconds = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                musicService.onCountdownTick(game, seconds);
                broadcastCountdown(game, seconds);
            }, (10 - i) * 20L);
        }
    }
}
```

#### In Event Listeners

```java
@EventHandler
public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Game game = gameManager.getPlayerGame(player);

    if (game != null && isWoolBlock(event.getBlock())) {
        // Play sound for wool placement
        musicService.onWoolPlaced(player, game);

        // Check for game end conditions
        if (checkWinCondition(game)) {
            musicService.onGameEnd(game, getWinner(game));
        }
    }
}
```

#### In Command Handlers

```java
public class BattleBoxCommand implements CommandExecutor {

    private void handleMusicCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /battlebox music <toggle|volume|info>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "toggle" -> {
                boolean enabled = musicService.togglePlayerMusic(player);
                player.sendMessage("Music " + (enabled ? "enabled" : "disabled"));
            }
            case "sounds" -> {
                boolean enabled = musicService.togglePlayerSoundEffects(player);
                player.sendMessage("Sound effects " + (enabled ? "enabled" : "disabled"));
            }
            case "volume" -> {
                if (args.length < 3) {
                    player.sendMessage("Usage: /battlebox music volume <0.0-1.0>");
                    return;
                }
                try {
                    float volume = Float.parseFloat(args[2]);
                    MusicService.MusicPreference pref = musicService.getPlayerPreference(player);
                    pref.setMusicVolume(volume);
                    musicService.updatePlayerPreference(player, pref);
                    player.sendMessage("Music volume set to " + volume);
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid volume. Use a number between 0.0 and 1.0");
                }
            }
            case "info" -> showMusicInfo(player);
        }
    }

    private void showMusicInfo(Player player) {
        MusicService.MusicPreference pref = musicService.getPlayerPreference(player);
        player.sendMessage("§6=== Music Settings ===");
        player.sendMessage("§7Music: " + (pref.isMusicEnabled() ? "§aEnabled" : "§cDisabled"));
        player.sendMessage("§7Sound Effects: " + (pref.isSoundEffectsEnabled() ? "§aEnabled" : "§cDisabled"));
        player.sendMessage("§7Music Volume: §b" + String.format("%.1f", pref.getMusicVolume()));
        player.sendMessage("§7Sound Volume: §b" + String.format("%.1f", pref.getSoundVolume()));
    }
}
```

### Advanced Usage

#### Custom Music Events

```java
public class CustomGameMode {
    private final MusicService musicService;

    public void onSpecialEvent(Game game) {
        // Play custom sound for special events
        musicService.playGameSoundEffect(game, MusicService.MusicEvent.COUNTDOWN_FINAL);

        // Or play for specific player
        Player player = getEventPlayer();
        musicService.playSoundEffect(player, MusicService.MusicEvent.TEAM_WIN);
    }
}
```

#### Music Integration in Managers

```java
public class GameManager {
    private final MusicService musicService;

    public void endGame(String gameId, Game.TeamColor winner) {
        Game game = getGame(gameId);
        if (game != null) {
            // Play appropriate end music
            musicService.onGameEnd(game, winner);

            // Clean up music after delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                musicService.stopGameMusic(gameId);
            }, 200L); // 10 seconds
        }
    }
}
```

## Player Preference Management

### MusicPreference Class

```java
MusicService.MusicPreference pref = musicService.getPlayerPreference(player);

// Check current settings
boolean musicEnabled = pref.isMusicEnabled();
boolean soundsEnabled = pref.isSoundEffectsEnabled();
float musicVolume = pref.getMusicVolume(); // 0.0 - 1.0
float soundVolume = pref.getSoundVolume(); // 0.0 - 1.0

// Modify settings
pref.setMusicEnabled(false);
pref.setMusicVolume(0.8f);
pref.setSoundEffectsEnabled(true);
pref.setSoundVolume(0.6f);

// Save changes
musicService.updatePlayerPreference(player, pref);
```

### Quick Toggle Methods

```java
// Toggle music on/off (returns new state)
boolean musicEnabled = musicService.togglePlayerMusic(player);

// Toggle sound effects on/off (returns new state)
boolean soundsEnabled = musicService.togglePlayerSoundEffects(player);
```

## Best Practices

### 1. **Service Integration**

- Always inject MusicService through constructors
- Use the service in event listeners and game logic
- Don't create multiple instances

### 2. **Performance Considerations**

- Music automatically stops when games end
- Player preferences are stored in memory
- Sounds are played per-player, not globally

### 3. **Error Handling**

```java
public void safePlayMusic(Game game) {
    try {
        musicService.updateGameMusic(game);
    } catch (Exception e) {
        plugin.getLogger().warning("Failed to play music: " + e.getMessage());
    }
}
```

### 4. **Testing Music Events**

```java
// For testing/debugging
public void testMusicEvent(Player player, String eventName) {
    try {
        MusicService.MusicEvent event = MusicService.MusicEvent.valueOf(eventName.toUpperCase());
        musicService.playSoundEffect(player, event);
        player.sendMessage("Played: " + event.displayName);
    } catch (IllegalArgumentException e) {
        player.sendMessage("Unknown music event: " + eventName);
    }
}
```

## Configuration

### Plugin Integration

The MusicService is automatically initialized in the main BattleBox plugin:

```java
// In BattleBox.java
private MusicService musicService;

@Override
public void onEnable() {
    // Initialize service
    musicService = new MusicService(this);

    // Inject into other services
    gameService = new GameService(/* other deps */, musicService);
}

@Override
public void onDisable() {
    // Cleanup
    if (musicService != null) {
        musicService.shutdown();
    }
}

// Getter for other classes
public MusicService getMusicService() {
    return musicService;
}
```

## Troubleshooting

### Common Issues

1. **Music not playing**

   - Check player preferences: `musicService.getPlayerPreference(player)`
   - Verify game state is correct
   - Ensure player is in a game

2. **Sounds cutting off**

   - This is normal Minecraft behavior - new sounds replace old ones
   - Use different sound categories if needed

3. **Performance issues**
   - Music tasks are automatically cleaned up
   - Use `musicService.shutdown()` when disabling plugin

### Debug Commands

```java
// Add to your command handler for debugging
case "musicdebug" -> {
    if (!player.hasPermission("battlebox.admin")) return;

    Game game = gameManager.getPlayerGame(player);
    if (game == null) {
        player.sendMessage("Not in a game");
        return;
    }

    player.sendMessage("Game State: " + game.getState());
    player.sendMessage("Music Preference: " + musicService.getPlayerPreference(player).isMusicEnabled());

    // Test sound
    musicService.playSoundEffect(player, MusicService.MusicEvent.COUNTDOWN_TICK);
}
```

## Extension Points

### Adding New Music Events

```java
// In MusicEvent enum, add new events:
SPECIAL_POWERUP("Power Up", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0),
BRIDGE_COMPLETE("Bridge Complete", Sound.BLOCK_ANVIL_USE, 0);

// Use in game logic:
musicService.playGameSoundEffect(game, MusicEvent.SPECIAL_POWERUP);
```

### Custom Sound Categories

For different volume controls, modify the service to support different sound categories:

```java
player.playSound(location, sound, SoundCategory.AMBIENT, volume, pitch);
```

This comprehensive system provides a robust foundation for all audio needs in the BattleBox plugin while maintaining the established architectural patterns.
