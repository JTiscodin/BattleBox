# BattleBox Plugin Testing Guide

This guide covers testing all major features of the BattleBox plugin including scoreboards, arena creation, game mechanics, winner detection, and timer systems.

## Prerequisites

1. **Server Setup**: Spigot/Paper 1.20.1 server
2. **Dependencies**: WorldEdit plugin installed and working
3. **Permissions**: Admin/OP permissions for testing
4. **Multiple Players**: Some tests require 2+ players

## 1. Scoreboard System Testing

### Automatic Scoreboard Display
- **Test**: Join the server as a player
- **Expected**: Scoreboard appears automatically on the right side showing:
  - Server title "BattleBox Server"
  - Player name
  - Game status: "No Active Game"
  - Arena info: "None"
  - Online players count
  - Updates every second

### Scoreboard Updates
- **Test**: Have other players join/leave the server
- **Expected**: Online players count updates in real-time on all scoreboards

## 2. Arena Creation System Testing

### Basic Arena Creation Workflow
```
/arena create TestArena
/arena setregion
[Use WorldEdit wand to select region - //wand, left-click, right-click]
/arena setregion confirm
/arena addkit Warrior
[Right-click a block to place the kit button]
/arena save
```

### Arena Management Commands
```
/arena list                    # View all arenas
/arena info TestArena         # View arena details
/arena delete TestArena       # Delete arena
```

### Interactive Arena Creation (Wizard Mode)
```
/arena wizard
# Follow the step-by-step guided process
```

## 3. Game Mechanics Testing

### Create and Join Games
```
/gametest create game1 TestArena    # Create test game
/gametest join game1                # Join the game
/gametest teams                     # Check team assignments
```

### Multi-Player Team Testing
- Have 4+ players join the same game
- Use `/gametest teams` to verify team balancing
- Teams should be: Red, Blue, Green, Yellow
- Players should be distributed evenly

### Winner Detection Testing
```
/gametest woolcount game1           # Check current wool counts
# Place colored wool blocks in the arena box area
/gametest winner game1              # Test winner calculation
```

**Manual Testing**:
1. Create a game with multiple players
2. Place RED_WOOL, BLUE_WOOL, GREEN_WOOL, YELLOW_WOOL blocks in the arena box coordinates
3. Use `/gametest woolcount` to verify counting
4. Use `/gametest winner` to test winner logic

## 4. Timer System Testing

### Personal Timer Testing
```
/timertest start 10                           # 10-second personal timer
/timertest start 30 "PREPARE FOR BATTLE"     # Custom title timer
/timertest start 60                           # 1-minute countdown
```

### Global Timer Testing
```
/timertest global 15                          # Global 15-second timer for all players
/timertest global 45 "SERVER RESTART"        # Global timer with custom title
```

### Game-Specific Timer Testing
```
/gametest create timer_game TestArena         # Create a test game
/gametest join timer_game                     # Join the game
/timertest game timer_game 20 "GAME STARTING" # Start timer for game players
```

### Timer Management Testing
```
/timertest status                             # Check active timers
/timertest remaining test_YourPlayerName      # Check remaining time
/timertest stop test_YourPlayerName           # Stop specific timer
/timertest stopall                            # Stop all active timers
```

### Timer Visual Testing
- **Expected Behavior**:
  - Timers appear as title/subtitle in center of screen
  - Green color for >10 seconds remaining
  - Yellow color for 4-10 seconds remaining  
  - Red color for 1-3 seconds remaining
  - "GO!" message appears when timer completes
  - Smooth 1-second intervals
  - Time format: "MM:SS" for >60 seconds, "SS" for ≤60 seconds

### Advanced Timer Scenarios
```
# Test multiple concurrent timers
/timertest start 30 "Personal Timer"
/timertest global 45 "Global Timer"

# Test timer interruption
/timertest start 60
/timertest stop test_YourPlayerName

# Test game integration
/gametest create multi_game TestArena
/gametest join multi_game
/timertest game multi_game 25 "MATCH STARTS"
```

## 5. Integration Testing

### Full Game Flow with Timers
1. Create arena: `/arena create IntegrationTest`
2. Set up arena with WorldEdit selection and kit buttons
3. Create game: `/gametest create full_test IntegrationTest`
4. Multiple players join: `/gametest join full_test`
5. Start game countdown: `/timertest game full_test 30 "BATTLE BEGINS"`
6. Test wool placement and winner detection during/after timer

### Stress Testing
```
# Create multiple games
/gametest create stress1 TestArena
/gametest create stress2 TestArena
/gametest create stress3 TestArena

# Start multiple timers
/timertest global 60 "Global Test"
/timertest game stress1 45 "Game 1"
/timertest game stress2 30 "Game 2"

# Check system performance
/timertest status
```

## 6. Error Handling Testing

### Invalid Commands
```
/timertest start -5                   # Negative duration
/timertest start abc                  # Invalid number
/timertest game nonexistent 30       # Non-existent game
/timertest stop invalid_timer         # Non-existent timer
```

### Edge Cases
```
/timertest start 0                    # Zero duration
/timertest start 3600                 # Very long duration (1 hour)
/gametest join full_game              # Join full game
/arena delete nonexistent            # Delete non-existent arena
```

## 7. Performance Testing

### Server Performance
- Monitor server TPS during timer operations
- Test with 10+ concurrent timers
- Check memory usage with multiple active games
- Test scoreboard updates with many online players

### Memory Cleanup
1. Start multiple timers and games
2. Use `/timertest stopall` and `/gametest delete <gameId>`
3. Verify no memory leaks in console
4. Restart plugin and verify clean state

## 8. Configuration Testing

### Arena Configuration Files
- Check `arenas.json` is created/updated properly
- Verify arena data persistence across server restarts
- Test arena hot-reloading capabilities

### Plugin Reload Testing
1. Create arenas and games
2. Start timers
3. Reload plugin: `/reload confirm`
4. Verify state recovery and cleanup

## 9. Expected Console Output

### Successful Operations
```
[INFO] Started timer 'test_PlayerName' for 30 seconds with 1 players
[INFO] Timer test_PlayerName completed for player PlayerName
[INFO] ArenaCommand executed with args: [create, TestArena]
[INFO] Arena 'TestArena' created successfully
```

### Error Scenarios
```
[WARNING] World 'InvalidWorld' not found for arena TestArena
[INFO] Stopped timer 'global_timer'
[INFO] Stopped 3 active timers
```

## 10. Troubleshooting

### Common Issues
- **Timer not showing**: Check player has titles enabled in client settings
- **WorldEdit not working**: Verify WorldEdit plugin is installed and loaded
- **Permissions errors**: Ensure player has appropriate battlebox.* permissions
- **Arena not saving**: Check file permissions for plugin data folder

### Debug Commands
```
/timertest status          # Check active timers
/gametest list            # List all games
/arena list               # List all arenas
/plugins                  # Verify BattleBox and WorldEdit are loaded
```

This testing guide ensures comprehensive coverage of all BattleBox features including the new timer system functionality.
