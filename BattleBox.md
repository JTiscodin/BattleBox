# BattleBox Minecraft Plugin

A competitive Minecraft minigame plugin featuring team-based capture the center battles.

## Overview

BattleBox is a fast-paced team-based minigame where two teams compete to fill a central 3x3 area with their colored wool. Teams must strategically use different kits and work together to claim the center while defending against the opposing team.

## Game Features

### Teams
- **Two teams**: Red and Blue
- **Team size**: 4 players each (currently supports single-player testing)
- **Team identification**: Colored leather boots (enchanted) matching team color

### Kit System

#### Base Kit (All Players)
- Wooden sword
- Bow with 6 arrows
- Stack of team-colored wool (unlimited - auto-refills)
- Unbreakable shears
- Colored leather boots (enchanted)

#### Special Kits (One per player)
Each player can choose one special kit:

- **Healer**: 2 instant health splash potions
- **Fighter**: Stone sword + leather chestplate
- **Sniper**: Crossbow + 2 arrows
- **Speedster**: Speed-enchanted leather leggings + stone sword

## Arena Structure

### Required Components
- **Team spawn locations**: Initial waiting areas for both teams
- **Teleport locations**: Game start positions (typically below spawn areas)
- **Center wool box**: 3x3 area for wool placement (coordinates: x1,y,z1 to x2,y,z2)
- **Kit selection buttons**: 8 buttons total (4 per team, one for each special kit)

### Gameplay Area
- Players can only place blocks in the designated 3x3 center area
- All other areas are protected from block placement

## Game Rules

### Timing
- **Game duration**: 2 minutes

### Victory Conditions
1. **Instant win**: First team to completely fill the 3x3 center with their wool
2. **Majority win**: Team with most wool blocks in center at time expiration
3. **Tie**: Equal wool count or no wool placed

### Death System
- Dead players become "spectators"
- Spectators are invisible to living players
- Spectators appear in tab list with spectator status
- Spectators are teleported to above the center platform for observation

## Development Status

This plugin is currently in development with arena creation logic implemented. Single-player testing is supported while multiplayer features are being refined.

## Installation

1. Place the plugin JAR file in your server's `plugins` folder
2. Configure arenas using the arena creation commands
3. Set up kit selection buttons and spawn points
4. Start hosting BattleBox games!

## Commands

*(Command documentation will be added as features are implemented)*

## Configuration

Arena configurations are stored in `arenas.json` with support for multiple arena setups.

