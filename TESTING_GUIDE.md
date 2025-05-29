# BattleBox Arena Testing Guide

## Prerequisites
1. Server running with BattleBox plugin loaded
2. WorldEdit plugin installed
3. Player has OP permissions or `battlebox.arena.admin` permission

## Step-by-Step Testing

### 1. Basic Command Check
```
/arena
```
**Expected:** Help message with all available commands

**If this fails:** Plugin not loaded or command not registered

### 2. Permission Check
```
/op <your_username>
```
**Then try:** `/arena` again

### 3. List Existing Arenas
```
/arena list
```
**Expected:** Shows "arena-1" (your existing arena)

### 4. View Existing Arena Info
```
/arena info arena-1
```
**Expected:** Shows arena details including coordinates (62,9,-57) to (64,9,-55)

### 5. Test Tab Completion
Type `/arena ` and press TAB
**Expected:** Shows available subcommands

### 6. Test Location Commands
```
/arena whereami
```
**Expected:** Shows your current coordinates

### 7. Test Teleport to Existing Arena
```
/arena tp arena-1
```
**Expected:** Teleports you to center of existing arena

### 8. Test Highlighting
```
/arena highlight arena-1
```
**Expected:** Green particle outline around arena bounds

## Troubleshooting

### Commands Not Working
1. Check plugin is loaded: `/plugins` (should show BattleBox in green)
2. Check for errors in server console
3. Verify you have permissions: `/op <username>`

### Tab Completion Not Working
1. Make sure you're pressing TAB after `/arena ` (with space)
2. Try restarting server if plugin was reloaded

### WorldEdit Integration
1. Make sure WorldEdit is installed
2. Get wand: `//wand`
3. Make selection with left/right clicks
4. Then use `/arena setregion`

## Server Console Commands
If in-game commands fail, check server console for errors:
- Look for plugin loading messages
- Check for compilation errors
- Verify command registration messages
